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

package com.android.pandora

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.TRANSPORT_LE
import android.bluetooth.BluetoothVolumeControl
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothProfile.CONNECTION_POLICY_ALLOWED
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.google.protobuf.Empty
import io.grpc.Status
import io.grpc.stub.StreamObserver
import java.io.Closeable
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import pandora.vcp.VCPGrpc.VCPImplBase
import pandora.vcp.VcpProto.*
import pandora.HostProto.Connection

@kotlinx.coroutines.ExperimentalCoroutinesApi
class Vcp(val context: Context) : VCPImplBase(), Closeable {
    private val TAG = "PandoraVcp"

    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default.limitedParallelism(1))

    private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)!!
    private val bluetoothAdapter = bluetoothManager.adapter

    private val bluetoothVolumeControl =
        getProfileProxy<BluetoothVolumeControl>(context, BluetoothProfile.VOLUME_CONTROL)

    private val flow =
        intentFlow(
                context,
                IntentFilter().apply {
                    addAction(BluetoothVolumeControl.ACTION_CONNECTION_STATE_CHANGED)
                },
                scope,
            )
            .shareIn(scope, SharingStarted.Eagerly)

    override fun close() {
        // Deinit the CoroutineScope
        scope.cancel()
    }

    override fun setDeviceVolume(
        request: SetDeviceVolumeRequest,
        responseObserver: StreamObserver<Empty>
    ) {
        grpcUnary<Empty>(scope, responseObserver) {
            val device = request.connection.toBluetoothDevice(bluetoothAdapter)

            Log.i(TAG, "setDeviceVolume(${device}, ${request.volume})")

            bluetoothVolumeControl.setDeviceVolume(device, request.volume, false)

            Empty.getDefaultInstance()
        }
    }

    override fun setVolumeOffset(
        request: SetVolumeOffsetRequest,
        responseObserver: StreamObserver<Empty>
    ) {
        grpcUnary<Empty>(scope, responseObserver) {
            val device = request.connection.toBluetoothDevice(bluetoothAdapter)

            Log.i(TAG, "setVolumeOffset(${device}, ${request.offset})")

            bluetoothVolumeControl.setVolumeOffset(device, 1, request.offset)

            Empty.getDefaultInstance()
        }
    }

    override fun waitConnect(
        request: WaitConnectRequest,
        responseObserver: StreamObserver<Empty>
    ) {
        grpcUnary<Empty>(scope, responseObserver) {
            val device = request.connection.toBluetoothDevice(bluetoothAdapter)
            Log.i(TAG, "waitPeripheral(${device}")
            if (
                bluetoothVolumeControl.getConnectionState(device) != BluetoothProfile.STATE_CONNECTED
            ) {
                Log.d(TAG, "Manual call to setConnectionPolicy")
                bluetoothVolumeControl.setConnectionPolicy(device, CONNECTION_POLICY_ALLOWED)
                Log.d(TAG, "wait for bluetoothVolumeControl profile connection")
                flow
                    .filter { it.getBluetoothDeviceExtra() == device }
                    .map { it.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothAdapter.ERROR) }
                    .filter { it == BluetoothProfile.STATE_CONNECTED }
                    .first()
            }

            Empty.getDefaultInstance()
        }
    }
}