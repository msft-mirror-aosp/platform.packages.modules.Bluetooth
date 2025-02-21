/**
 * Copyright 2024, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

extern "C" {

#include <stddef.h>
#include <stdint.h>

/**
 * Callabcks from C to Rust
 * The given `handle` must be passed as the first parameter of all functions.
 * The functions can be called from `hal_interface.initialize()` call to
 * `hal_interface.close()` call.
 */

enum HalStatus {
  STATUS_SUCCESS,
  STATUS_ALREADY_INITIALIZED,
  STATUS_UNABLE_TO_OPEN_INTERFACE,
  STATUS_HARDWARE_INITIALIZATION_ERROR,
  STATUS_UNKNOWN,
};

struct hal_callbacks {
  void *handle;
  void (*initialization_complete)(const void *handle, enum HalStatus);
  void (*event_received)(const void *handle, const uint8_t *data, size_t len);
  void (*acl_received)(const void *handle, const uint8_t *data, size_t len);
  void (*sco_received)(const void *handle, const uint8_t *data, size_t len);
  void (*iso_received)(const void *handle, const uint8_t *data, size_t len);
};

/**
 * Interface from Rust to C
 * The `handle` value is passed as the first parameter of all functions.
 * Theses functions can be called from different threads, but NOT concurrently.
 * Locking over `handle` is not necessary.
 */

struct hal_interface {
  void *handle;
  void (*initialize)(void *handle, const struct hal_callbacks *);
  void (*close)(void *handle);
  void (*send_command)(void *handle, const uint8_t *data, size_t len);
  void (*send_acl)(void *handle, const uint8_t *data, size_t len);
  void (*send_sco)(void *handle, const uint8_t *data, size_t len);
  void (*send_iso)(void *handle, const uint8_t *data, size_t len);
  void (*client_died)(void *handle);
};
}
