/*
 * Copyright 2023 The Android Open Source Project
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

#include "bta/dm/bta_dm_int.h"
#include "types/bt_transport.h"
#include "types/raw_address.h"

void bta_dm_process_remove_device_no_callback(const RawAddress& bd_addr);
void bta_dm_process_remove_device(const RawAddress& bd_addr);

tBTA_DM_PEER_DEVICE* find_connected_device(const RawAddress& bd_addr,
                                           tBT_TRANSPORT /* transport */);

namespace bluetooth::legacy::testing {
void bta_dm_init_cb(void);
void bta_dm_acl_down(const RawAddress& bd_addr, tBT_TRANSPORT transport);
void bta_dm_acl_up(const RawAddress& bd_addr, tBT_TRANSPORT transport, uint16_t acl_handle);
tBTA_DM_PEER_DEVICE* allocate_device_for(const RawAddress& bd_addr, tBT_TRANSPORT transport);
}  // namespace bluetooth::legacy::testing
