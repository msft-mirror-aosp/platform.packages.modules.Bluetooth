/*
 * Copyright 2024 The Android Open Source Project
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

package com.android.bluetooth.btservice;

class BluetoothHciVendorSpecificNativeInterface {
    private static final String TAG = "BluetoothHciVendorSpecificNativeInterface";
    private final BluetoothHciVendorSpecificDispatcher mDispatcher;

    public BluetoothHciVendorSpecificNativeInterface(
            BluetoothHciVendorSpecificDispatcher dispatcher) {
        mDispatcher = dispatcher;
    }

    void init() {
        initNative();
    }

    void cleanup() {
        cleanupNative();
    }

    void sendCommand(int ocf, byte[] parameters, byte[] cookie) {
        sendCommandNative(ocf, parameters, cookie);
    }

    // Callbacks from the native stack

    private void onCommandStatus(int ocf, int status, byte[] cookie) {
        mDispatcher.dispatchCommandStatusOrComplete(
                cookie, (cb) -> cb.onCommandStatus(ocf, status));
    }

    private void onCommandComplete(int ocf, byte[] returnParameters, byte[] cookie) {
        mDispatcher.dispatchCommandStatusOrComplete(
                cookie, (cb) -> cb.onCommandComplete(ocf, returnParameters));
    }

    private void onEvent(int code, byte[] data) {
        mDispatcher.broadcastEvent(code, (cb) -> cb.onEvent(code, data));
    }

    // Native methods that call into the JNI interface

    private native void initNative();

    private native void cleanupNative();

    private native void sendCommandNative(int ocf, byte[] parameters, byte[] cookie);
}
