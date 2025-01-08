/*
 * Copyright 2022 The Android Open Source Project
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

package com.android.bluetooth.gatt;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.IBluetoothGattCallback;
import android.bluetooth.IBluetoothGattServerCallback;
import android.content.AttributionSource;
import android.os.ParcelUuid;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.UUID;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class GattServiceBinderTest {

    private static final String REMOTE_DEVICE_ADDRESS = "00:00:00:00:00:00";

    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock private GattService mService;

    private AttributionSource mAttributionSource;

    private GattService.BluetoothGattBinder mBinder;

    @Before
    public void setUp() throws Exception {
        when(mService.isAvailable()).thenReturn(true);
        mBinder = new GattService.BluetoothGattBinder(mService);
        mAttributionSource = new AttributionSource.Builder(1).build();
    }

    @Test
    public void getDevicesMatchingConnectionStates() {
        int[] states = new int[] {BluetoothProfile.STATE_CONNECTED};

        mBinder.getDevicesMatchingConnectionStates(states, mAttributionSource);

        verify(mService).getDevicesMatchingConnectionStates(states, mAttributionSource);
    }

    @Test
    public void registerClient() {
        UUID uuid = UUID.randomUUID();
        IBluetoothGattCallback callback = mock(IBluetoothGattCallback.class);
        boolean eattSupport = true;

        mBinder.registerClient(new ParcelUuid(uuid), callback, eattSupport, mAttributionSource);

        verify(mService).registerClient(uuid, callback, eattSupport, mAttributionSource);
    }

    @Test
    public void unregisterClient() {
        int clientIf = 3;

        mBinder.unregisterClient(clientIf, mAttributionSource);

        verify(mService).unregisterClient(clientIf, mAttributionSource);
    }

    @Test
    public void clientConnect() throws Exception {
        int clientIf = 1;
        String address = REMOTE_DEVICE_ADDRESS;
        int addressType = BluetoothDevice.ADDRESS_TYPE_RANDOM;
        boolean isDirect = true;
        int transport = 2;
        boolean opportunistic = true;
        int phy = 3;

        mBinder.clientConnect(
                clientIf,
                address,
                addressType,
                isDirect,
                transport,
                opportunistic,
                phy,
                mAttributionSource);

        verify(mService)
                .clientConnect(
                        clientIf,
                        address,
                        addressType,
                        isDirect,
                        transport,
                        opportunistic,
                        phy,
                        mAttributionSource);
    }

    @Test
    public void clientDisconnect() throws Exception {
        int clientIf = 1;
        String address = REMOTE_DEVICE_ADDRESS;

        mBinder.clientDisconnect(clientIf, address, mAttributionSource);

        verify(mService).clientDisconnect(clientIf, address, mAttributionSource);
    }

    @Test
    public void clientSetPreferredPhy() throws Exception {
        int clientIf = 1;
        String address = REMOTE_DEVICE_ADDRESS;
        int txPhy = 2;
        int rxPhy = 1;
        int phyOptions = 3;

        mBinder.clientSetPreferredPhy(
                clientIf, address, txPhy, rxPhy, phyOptions, mAttributionSource);

        verify(mService)
                .clientSetPreferredPhy(
                        clientIf, address, txPhy, rxPhy, phyOptions, mAttributionSource);
    }

    @Test
    public void clientReadPhy() throws Exception {
        int clientIf = 1;
        String address = REMOTE_DEVICE_ADDRESS;

        mBinder.clientReadPhy(clientIf, address, mAttributionSource);

        verify(mService).clientReadPhy(clientIf, address, mAttributionSource);
    }

    @Test
    public void refreshDevice() throws Exception {
        int clientIf = 1;
        String address = REMOTE_DEVICE_ADDRESS;

        mBinder.refreshDevice(clientIf, address, mAttributionSource);

        verify(mService).refreshDevice(clientIf, address, mAttributionSource);
    }

    @Test
    public void discoverServices() throws Exception {
        int clientIf = 1;
        String address = REMOTE_DEVICE_ADDRESS;

        mBinder.discoverServices(clientIf, address, mAttributionSource);

        verify(mService).discoverServices(clientIf, address, mAttributionSource);
    }

    @Test
    public void discoverServiceByUuid() throws Exception {
        int clientIf = 1;
        String address = REMOTE_DEVICE_ADDRESS;
        UUID uuid = UUID.randomUUID();

        mBinder.discoverServiceByUuid(clientIf, address, new ParcelUuid(uuid), mAttributionSource);

        verify(mService).discoverServiceByUuid(clientIf, address, uuid, mAttributionSource);
    }

    @Test
    public void readCharacteristic() throws Exception {
        int clientIf = 1;
        String address = REMOTE_DEVICE_ADDRESS;
        int handle = 2;
        int authReq = 3;

        mBinder.readCharacteristic(clientIf, address, handle, authReq, mAttributionSource);

        verify(mService).readCharacteristic(clientIf, address, handle, authReq, mAttributionSource);
    }

    @Test
    public void readUsingCharacteristicUuid() throws Exception {
        int clientIf = 1;
        String address = REMOTE_DEVICE_ADDRESS;
        UUID uuid = UUID.randomUUID();
        int startHandle = 2;
        int endHandle = 3;
        int authReq = 4;

        mBinder.readUsingCharacteristicUuid(
                clientIf,
                address,
                new ParcelUuid(uuid),
                startHandle,
                endHandle,
                authReq,
                mAttributionSource);

        verify(mService)
                .readUsingCharacteristicUuid(
                        clientIf,
                        address,
                        uuid,
                        startHandle,
                        endHandle,
                        authReq,
                        mAttributionSource);
    }

    @Test
    public void writeCharacteristic() throws Exception {
        int clientIf = 1;
        String address = REMOTE_DEVICE_ADDRESS;
        int handle = 2;
        int writeType = 3;
        int authReq = 4;
        byte[] value = new byte[] {5, 6};

        mBinder.writeCharacteristic(
                clientIf, address, handle, writeType, authReq, value, mAttributionSource);

        verify(mService)
                .writeCharacteristic(
                        clientIf, address, handle, writeType, authReq, value, mAttributionSource);
    }

    @Test
    public void readDescriptor() throws Exception {
        int clientIf = 1;
        String address = REMOTE_DEVICE_ADDRESS;
        int handle = 2;
        int authReq = 3;

        mBinder.readDescriptor(clientIf, address, handle, authReq, mAttributionSource);

        verify(mService).readDescriptor(clientIf, address, handle, authReq, mAttributionSource);
    }

    @Test
    public void writeDescriptor() throws Exception {
        int clientIf = 1;
        String address = REMOTE_DEVICE_ADDRESS;
        int handle = 2;
        int authReq = 3;
        byte[] value = new byte[] {4, 5};

        mBinder.writeDescriptor(clientIf, address, handle, authReq, value, mAttributionSource);

        verify(mService)
                .writeDescriptor(clientIf, address, handle, authReq, value, mAttributionSource);
    }

    @Test
    public void beginReliableWrite() throws Exception {
        int clientIf = 1;
        String address = REMOTE_DEVICE_ADDRESS;

        mBinder.beginReliableWrite(clientIf, address, mAttributionSource);

        verify(mService).beginReliableWrite(clientIf, address, mAttributionSource);
    }

    @Test
    public void endReliableWrite() throws Exception {
        int clientIf = 1;
        String address = REMOTE_DEVICE_ADDRESS;
        boolean execute = true;

        mBinder.endReliableWrite(clientIf, address, execute, mAttributionSource);

        verify(mService).endReliableWrite(clientIf, address, execute, mAttributionSource);
    }

    @Test
    public void registerForNotification() throws Exception {
        int clientIf = 1;
        String address = REMOTE_DEVICE_ADDRESS;
        int handle = 2;
        boolean enable = true;

        mBinder.registerForNotification(clientIf, address, handle, enable, mAttributionSource);

        verify(mService)
                .registerForNotification(clientIf, address, handle, enable, mAttributionSource);
    }

    @Test
    public void readRemoteRssi() throws Exception {
        int clientIf = 1;
        String address = REMOTE_DEVICE_ADDRESS;

        mBinder.readRemoteRssi(clientIf, address, mAttributionSource);

        verify(mService).readRemoteRssi(clientIf, address, mAttributionSource);
    }

    @Test
    public void configureMTU() throws Exception {
        int clientIf = 1;
        String address = REMOTE_DEVICE_ADDRESS;
        int mtu = 2;

        mBinder.configureMTU(clientIf, address, mtu, mAttributionSource);

        verify(mService).configureMTU(clientIf, address, mtu, mAttributionSource);
    }

    @Test
    public void connectionParameterUpdate() throws Exception {
        int clientIf = 1;
        String address = REMOTE_DEVICE_ADDRESS;
        int connectionPriority = 2;

        mBinder.connectionParameterUpdate(
                clientIf, address, connectionPriority, mAttributionSource);

        verify(mService)
                .connectionParameterUpdate(
                        clientIf, address, connectionPriority, mAttributionSource);
    }

    @Test
    public void leConnectionUpdate() throws Exception {
        int clientIf = 1;
        String address = REMOTE_DEVICE_ADDRESS;
        int minConnectionInterval = 3;
        int maxConnectionInterval = 4;
        int peripheralLatency = 5;
        int supervisionTimeout = 6;
        int minConnectionEventLen = 7;
        int maxConnectionEventLen = 8;

        mBinder.leConnectionUpdate(
                clientIf,
                address,
                minConnectionInterval,
                maxConnectionInterval,
                peripheralLatency,
                supervisionTimeout,
                minConnectionEventLen,
                maxConnectionEventLen,
                mAttributionSource);

        verify(mService)
                .leConnectionUpdate(
                        clientIf,
                        address,
                        minConnectionInterval,
                        maxConnectionInterval,
                        peripheralLatency,
                        supervisionTimeout,
                        minConnectionEventLen,
                        maxConnectionEventLen,
                        mAttributionSource);
    }

    @Test
    public void registerServer() {
        UUID uuid = UUID.randomUUID();
        IBluetoothGattServerCallback callback = mock(IBluetoothGattServerCallback.class);
        boolean eattSupport = true;

        mBinder.registerServer(new ParcelUuid(uuid), callback, eattSupport, mAttributionSource);

        verify(mService).registerServer(uuid, callback, eattSupport, mAttributionSource);
    }

    @Test
    public void unregisterServer() {
        int serverIf = 3;

        mBinder.unregisterServer(serverIf, mAttributionSource);

        verify(mService).unregisterServer(serverIf, mAttributionSource);
    }

    @Test
    public void serverConnect() {
        int serverIf = 1;
        String address = REMOTE_DEVICE_ADDRESS;
        int addressType = BluetoothDevice.ADDRESS_TYPE_RANDOM;
        boolean isDirect = true;
        int transport = 2;

        mBinder.serverConnect(
                serverIf, address, addressType, isDirect, transport, mAttributionSource);

        verify(mService)
                .serverConnect(
                        serverIf, address, addressType, isDirect, transport, mAttributionSource);
    }

    @Test
    public void serverDisconnect() {
        int serverIf = 1;
        String address = REMOTE_DEVICE_ADDRESS;

        mBinder.serverDisconnect(serverIf, address, mAttributionSource);

        verify(mService).serverDisconnect(serverIf, address, mAttributionSource);
    }

    @Test
    public void serverSetPreferredPhy() throws Exception {
        int serverIf = 1;
        String address = REMOTE_DEVICE_ADDRESS;
        int txPhy = 2;
        int rxPhy = 1;
        int phyOptions = 3;

        mBinder.serverSetPreferredPhy(
                serverIf, address, txPhy, rxPhy, phyOptions, mAttributionSource);

        verify(mService)
                .serverSetPreferredPhy(
                        serverIf, address, txPhy, rxPhy, phyOptions, mAttributionSource);
    }

    @Test
    public void serverReadPhy() throws Exception {
        int serverIf = 1;
        String address = REMOTE_DEVICE_ADDRESS;

        mBinder.serverReadPhy(serverIf, address, mAttributionSource);

        verify(mService).serverReadPhy(serverIf, address, mAttributionSource);
    }

    @Test
    public void addService() {
        int serverIf = 1;
        BluetoothGattService svc = mock(BluetoothGattService.class);

        mBinder.addService(serverIf, svc, mAttributionSource);

        verify(mService).addService(serverIf, svc, mAttributionSource);
    }

    @Test
    public void removeService() {
        int serverIf = 1;
        int handle = 2;

        mBinder.removeService(serverIf, handle, mAttributionSource);

        verify(mService).removeService(serverIf, handle, mAttributionSource);
    }

    @Test
    public void clearServices() {
        int serverIf = 1;

        mBinder.clearServices(serverIf, mAttributionSource);

        verify(mService).clearServices(serverIf, mAttributionSource);
    }

    @Test
    public void sendResponse() throws Exception {
        int serverIf = 1;
        String address = REMOTE_DEVICE_ADDRESS;
        int requestId = 2;
        int status = 3;
        int offset = 4;
        byte[] value = new byte[] {5, 6};

        mBinder.sendResponse(
                serverIf, address, requestId, status, offset, value, mAttributionSource);

        verify(mService)
                .sendResponse(
                        serverIf, address, requestId, status, offset, value, mAttributionSource);
    }

    @Test
    public void sendNotification() throws Exception {
        int serverIf = 1;
        String address = REMOTE_DEVICE_ADDRESS;
        int handle = 2;
        boolean confirm = true;
        byte[] value = new byte[] {5, 6};

        mBinder.sendNotification(serverIf, address, handle, confirm, value, mAttributionSource);

        verify(mService)
                .sendNotification(serverIf, address, handle, confirm, value, mAttributionSource);
    }

    @Test
    public void disconnectAll() throws Exception {
        mBinder.disconnectAll(mAttributionSource);

        verify(mService).disconnectAll(mAttributionSource);
    }

    @Test
    public void cleanUp_doesNotCrash() {
        mBinder.cleanup();
    }
}
