/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.bluetooth.le_scan;

class MsftAdvMonitor {
    static class Monitor {
        public byte rssi_threshold_high;
        public byte rssi_threshold_low;
        public byte rssi_threshold_low_time_interval;
        public byte rssi_sampling_period;
        public byte condition_type;
    }

    static class Pattern {
        public byte ad_type;
        public byte start_byte;
        public byte[] pattern;
    }

    static class Address {
        byte addr_type;
        String bd_addr;
    }
}
