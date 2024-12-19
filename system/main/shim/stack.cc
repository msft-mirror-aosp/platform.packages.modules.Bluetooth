/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define LOG_TAG "bt_gd_shim"

#include "main/shim/stack.h"

#include <bluetooth/log.h>
#include <com_android_bluetooth_flags.h>
#include <fcntl.h>
#include <unistd.h>

#include <chrono>
#include <future>
#include <queue>
#include <string>

#include "common/strings.h"
#include "hal/hci_hal.h"
#include "hal/snoop_logger.h"
#include "hci/acl_manager.h"
#include "hci/acl_manager/acl_scheduler.h"
#include "hci/controller.h"
#include "hci/controller_interface.h"
#include "hci/distance_measurement_manager.h"
#include "hci/hci_layer.h"
#include "hci/le_advertising_manager.h"
#include "hci/le_scanning_manager.h"
#include "hci/msft.h"
#include "hci/remote_name_request.h"
#include "lpp/lpp_offload_manager.h"
#include "main/shim/acl.h"
#include "main/shim/acl_interface.h"
#include "main/shim/distance_measurement_manager.h"
#include "main/shim/entry.h"
#include "main/shim/hci_layer.h"
#include "main/shim/le_advertising_manager.h"
#include "main/shim/le_scanning_manager.h"
#include "metrics/counter_metrics.h"
#include "os/system_properties.h"
#include "os/wakelock_manager.h"
#include "storage/storage_module.h"

#if TARGET_FLOSS
#include "sysprops/sysprops_module.h"
#endif

using ::bluetooth::os::Handler;
using ::bluetooth::os::Thread;
using ::bluetooth::os::WakelockManager;

namespace bluetooth {
namespace shim {

struct Stack::impl {
  Acl* acl_ = nullptr;
};

Stack::Stack() { pimpl_ = std::make_shared<Stack::impl>(); }

Stack* Stack::GetInstance() {
  static Stack instance;
  return &instance;
}

void Stack::StartEverything() {
  std::lock_guard<std::recursive_mutex> lock(mutex_);
  log::assert_that(!is_running_, "Gd stack already running");
  log::info("Starting Gd stack");
  ModuleList modules;

#if TARGET_FLOSS
  modules.add<sysprops::SyspropsModule>();
#else
  if (com::android::bluetooth::flags::socket_settings_api()) {  // Added with aosp/3286716
    modules.add<lpp::LppOffloadManager>();
  }
#endif
  modules.add<metrics::CounterMetrics>();
  modules.add<hal::HciHal>();
  modules.add<hci::HciLayer>();
  modules.add<storage::StorageModule>();

  modules.add<hci::Controller>();
  modules.add<hci::acl_manager::AclScheduler>();
  modules.add<hci::AclManager>();
  modules.add<hci::RemoteNameRequestModule>();
  modules.add<hci::LeAdvertisingManager>();
  modules.add<hci::MsftExtensionManager>();
  modules.add<hci::LeScanningManager>();
  modules.add<hci::DistanceMeasurementManager>();
  Start(&modules);
  is_running_ = true;
  // Make sure the leaf modules are started
  log::assert_that(GetInstance<storage::StorageModule>() != nullptr,
                   "assert failed: GetInstance<storage::StorageModule>() != nullptr");
  if (IsStarted<hci::Controller>()) {
    pimpl_->acl_ =
            new Acl(stack_handler_, GetAclInterface(), GetController()->GetLeFilterAcceptListSize(),
                    GetController()->GetLeResolvingListSize());
  } else {
    log::error("Unable to create shim ACL layer as Controller has not started");
  }

  bluetooth::shim::hci_on_reset_complete();
  bluetooth::shim::init_advertising_manager();
  bluetooth::shim::init_scanning_manager();
  bluetooth::shim::init_distance_measurement_manager();
}

void Stack::StartModuleStack(const ModuleList* modules, const os::Thread* thread) {
  std::lock_guard<std::recursive_mutex> lock(mutex_);
  log::assert_that(!is_running_, "Gd stack already running");
  stack_thread_ = const_cast<os::Thread*>(thread);
  log::info("Starting Gd stack");

  StartUp(const_cast<ModuleList*>(modules), stack_thread_);
  stack_handler_ = new os::Handler(stack_thread_);

  num_modules_ = modules->NumModules();
  is_running_ = true;
}

void Stack::Start(ModuleList* modules) {
  log::assert_that(!is_running_, "Gd stack already running");
  log::info("Starting Gd stack");

  stack_thread_ = new os::Thread("gd_stack_thread", os::Thread::Priority::REAL_TIME);
  StartUp(modules, stack_thread_);

  stack_handler_ = new os::Handler(stack_thread_);

  log::info("Successfully toggled Gd stack");
}

void Stack::Stop() {
  std::lock_guard<std::recursive_mutex> lock(mutex_);
  bluetooth::shim::hci_on_shutting_down();

  // Make sure gd acl flag is enabled and we started it up
  if (pimpl_->acl_ != nullptr) {
    pimpl_->acl_->FinalShutdown();
    delete pimpl_->acl_;
    pimpl_->acl_ = nullptr;
  }

  log::assert_that(is_running_, "Gd stack not running");
  is_running_ = false;

  stack_handler_->Clear();

  ShutDown();

  delete stack_handler_;
  stack_handler_ = nullptr;

  stack_thread_->Stop();
  delete stack_thread_;
  stack_thread_ = nullptr;

  log::info("Successfully shut down Gd stack");
}

bool Stack::IsRunning() {
  std::lock_guard<std::recursive_mutex> lock(mutex_);
  return is_running_;
}

Acl* Stack::GetAcl() {
  std::lock_guard<std::recursive_mutex> lock(mutex_);
  log::assert_that(is_running_, "assert failed: is_running_");
  log::assert_that(pimpl_->acl_ != nullptr, "Acl shim layer has not been created");
  return pimpl_->acl_;
}

os::Handler* Stack::GetHandler() {
  std::lock_guard<std::recursive_mutex> lock(mutex_);
  log::assert_that(is_running_, "assert failed: is_running_");
  return stack_handler_;
}

void Stack::Dump(int fd, std::promise<void> promise) const {
  std::lock_guard<std::recursive_mutex> lock(mutex_);
  if (is_running_ && fd >= 0) {
    stack_handler_->Call(
            [](int fd, std::promise<void> promise) {
              bluetooth::shim::GetController()->Dump(fd);
              bluetooth::shim::GetAclManager()->Dump(fd);
              bluetooth::os::WakelockManager::Get().Dump(fd);
              bluetooth::shim::GetSnoopLogger()->DumpSnoozLogToFile();
              promise.set_value();
            },
            fd, std::move(promise));
  } else {
    promise.set_value();
  }
}

void Stack::StartUp(ModuleList* modules, Thread* stack_thread) {
  management_thread_ = new Thread("management_thread", Thread::Priority::NORMAL);
  handler_ = new Handler(management_thread_);

  WakelockManager::Get().Acquire();

  std::promise<void> promise;
  auto future = promise.get_future();
  handler_->Post(common::BindOnce(&Stack::handle_start_up, common::Unretained(this), modules,
                                  stack_thread, std::move(promise)));

  auto init_status = future.wait_for(
          std::chrono::milliseconds(get_gd_stack_timeout_ms(/* is_start = */ true)));

  WakelockManager::Get().Release();

  log::info("init_status == {}", int(init_status));

  log::assert_that(init_status == std::future_status::ready, "Can't start stack, last instance: {}",
                   registry_.last_instance_);

  log::info("init complete");
}

void Stack::handle_start_up(ModuleList* modules, Thread* stack_thread, std::promise<void> promise) {
  registry_.Start(modules, stack_thread);
  promise.set_value();
}

void Stack::ShutDown() {
  WakelockManager::Get().Acquire();

  std::promise<void> promise;
  auto future = promise.get_future();
  handler_->Post(
          common::BindOnce(&Stack::handle_shut_down, common::Unretained(this), std::move(promise)));

  auto stop_status = future.wait_for(
          std::chrono::milliseconds(get_gd_stack_timeout_ms(/* is_start = */ false)));

  WakelockManager::Get().Release();
  WakelockManager::Get().CleanUp();

  log::assert_that(stop_status == std::future_status::ready, "Can't stop stack, last instance: {}",
                   registry_.last_instance_);

  handler_->Clear();
  handler_->WaitUntilStopped(std::chrono::milliseconds(2000));
  delete handler_;
  delete management_thread_;
}

void Stack::handle_shut_down(std::promise<void> promise) {
  registry_.StopAll();
  promise.set_value();
}

std::chrono::milliseconds Stack::get_gd_stack_timeout_ms(bool is_start) {
  auto gd_timeout = os::GetSystemPropertyUint32(
          is_start ? "bluetooth.gd.start_timeout" : "bluetooth.gd.stop_timeout",
          /* default_value = */ is_start ? 3000 : 5000);
  return std::chrono::milliseconds(gd_timeout *
                                   os::GetSystemPropertyUint32("ro.hw_timeout_multiplier",
                                                               /* default_value = */ 1));
}

}  // namespace shim
}  // namespace bluetooth
