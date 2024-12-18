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

#pragma once

#include <functional>
#include <mutex>

#include "module.h"
#include "os/handler.h"
#include "os/thread.h"

// The shim layer implementation on the Gd stack side.
namespace bluetooth {
namespace shim {

class Acl;
class Btm;

// GD shim stack, having modes corresponding to legacy stack
class Stack {
public:
  static Stack* GetInstance();

  Stack();
  Stack(const Stack&) = delete;
  Stack& operator=(const Stack&) = delete;

  ~Stack() = default;

  // Running mode, everything is up
  void StartEverything();

  void Stop();
  bool IsRunning();

  template <class T>
  T* GetInstance() const {
    return static_cast<T*>(registry_.Get(&T::Factory));
  }

  template <class T>
  bool IsStarted() const {
    return registry_.IsStarted(&T::Factory);
  }

  Acl* GetAcl();

  os::Handler* GetHandler();

  void Dump(int fd, std::promise<void> promise) const;

  // Start the list of modules with the given stack manager thread
  void StartModuleStack(const ModuleList* modules, const os::Thread* thread);

  size_t NumModules() const { return num_modules_; }

private:
  struct impl;
  std::shared_ptr<impl> pimpl_;

  mutable std::recursive_mutex mutex_;
  bool is_running_ = false;
  os::Thread* stack_thread_ = nullptr;
  os::Handler* stack_handler_ = nullptr;
  size_t num_modules_{0};

  void StartUp(ModuleList* modules, os::Thread* stack_thread);
  void ShutDown();

  os::Thread* management_thread_ = nullptr;
  os::Handler* handler_ = nullptr;
  ModuleRegistry registry_;

  void handle_start_up(ModuleList* modules, os::Thread* stack_thread, std::promise<void> promise);
  void handle_shut_down(std::promise<void> promise);
  static std::chrono::milliseconds get_gd_stack_timeout_ms(bool is_start);

  void Start(ModuleList* modules);
};

}  // namespace shim
}  // namespace bluetooth
