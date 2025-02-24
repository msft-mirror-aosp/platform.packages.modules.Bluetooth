/******************************************************************************
 *
 *  Copyright 2009-2013 Broadcom Corporation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

/*****************************************************************************
 *
 *  Name:          btif_gatt.h
 *
 *  Description:
 *
 *****************************************************************************/

#pragma once

#include "include/hardware/bt_gatt.h"

extern const btgatt_client_interface_t btgattClientInterface;
extern const btgatt_server_interface_t btgattServerInterface;

BleScannerInterface* get_ble_scanner_instance();
const btgatt_interface_t* btif_gatt_get_interface();
