/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.bluetooth.mapclient;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.*;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Looper;
import android.os.UserHandle;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.MediumTest;
import androidx.test.runner.AndroidJUnit4;

import com.android.bluetooth.TestUtils;
import com.android.bluetooth.Utils;
import com.android.bluetooth.btservice.AdapterService;
import com.android.bluetooth.btservice.storage.DatabaseManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class MapClientTest {
    private static final String TAG = MapClientTest.class.getSimpleName();
    private MapClientService mService = null;
    private BluetoothAdapter mAdapter = null;
    private Context mTargetContext;
    private boolean mIsAdapterServiceSet;
    private boolean mIsMapClientServiceStarted;

    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock private AdapterService mAdapterService;
    @Mock private MnsService mMockMnsService;
    @Mock private DatabaseManager mDatabaseManager;

    @Before
    public void setUp() throws Exception {
        mTargetContext = InstrumentationRegistry.getTargetContext();
        TestUtils.setAdapterService(mAdapterService);
        mIsAdapterServiceSet = true;
        when(mAdapterService.getDatabase()).thenReturn(mDatabaseManager);
        mIsMapClientServiceStarted = true;
        Looper looper = null;
        mService = new MapClientService(mTargetContext, looper, mMockMnsService);
        mService.start();
        mService.setAvailable(true);
        mAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @After
    public void tearDown() throws Exception {
        if (mIsMapClientServiceStarted) {
            mService.stop();
            mService.cleanup();
            mService = MapClientService.getMapClientService();
            assertThat(mService).isNull();
        }
        if (mIsAdapterServiceSet) {
            TestUtils.clearAdapterService(mAdapterService);
        }
    }

    /**
     * Mock the priority of a bluetooth device
     *
     * @param device - The bluetooth device you wish to mock the priority of
     * @param priority - The priority value you want the device to have
     */
    private void mockDevicePriority(BluetoothDevice device, int priority) {
        when(mDatabaseManager.getProfileConnectionPolicy(device, BluetoothProfile.MAP_CLIENT))
                .thenReturn(priority);
    }

    @Test
    public void testInitialize() {
        assertThat(MapClientService.getMapClientService()).isNotNull();
    }

    /** Test connection of one device. */
    @Test
    public void testConnect() {
        // make sure there is no statemachine already defined for this device
        BluetoothDevice device = makeBluetoothDevice("11:11:11:11:11:11");
        assertThat(mService.getInstanceMap()).doesNotContainKey(device);

        // connect a bluetooth device
        mockDevicePriority(device, BluetoothProfile.CONNECTION_POLICY_ALLOWED);
        assertThat(mService.connect(device)).isTrue();

        // is the statemachine created
        Map<BluetoothDevice, MceStateMachine> map = mService.getInstanceMap();

        assertThat(map.size()).isEqualTo(1);
        MceStateMachine sm = map.get(device);
        assertThat(sm).isNotNull();
        TestUtils.waitForLooperToFinishScheduledTask(sm.getHandler().getLooper());

        assertThat(sm.getState()).isEqualTo(BluetoothProfile.STATE_CONNECTING);
        mService.cleanupDevice(device, sm);
        assertThat(mService.getInstanceMap()).doesNotContainKey(device);
    }

    /** Test that a PRIORITY_OFF device is not connected to */
    @Test
    public void testConnectPriorityOffDevice() {
        // make sure there is no statemachine already defined for this device
        BluetoothDevice device = makeBluetoothDevice("11:11:11:11:11:11");
        assertThat(mService.getInstanceMap()).doesNotContainKey(device);

        // connect a bluetooth device
        mockDevicePriority(device, BluetoothProfile.CONNECTION_POLICY_FORBIDDEN);
        assertThat(mService.connect(device)).isFalse();

        // is the statemachine created
        assertThat(mService.getInstanceMap()).isEmpty();
    }

    /** Test connecting MAXIMUM_CONNECTED_DEVICES devices. */
    @Test
    public void testConnectMaxDevices() {
        // Create bluetoothdevice & mock statemachine objects to be used in this test
        List<BluetoothDevice> list = new ArrayList<>();
        String address = "11:11:11:11:11:1";
        for (int i = 0; i < MapClientService.MAXIMUM_CONNECTED_DEVICES; ++i) {
            list.add(makeBluetoothDevice(address + i));
        }

        // make sure there is no statemachine already defined for the devices defined above
        for (BluetoothDevice d : list) {
            assertThat(mService.getInstanceMap().get(d)).isNull();
        }

        // run the test - connect all devices, set their priorities to on
        for (BluetoothDevice d : list) {
            mockDevicePriority(d, BluetoothProfile.CONNECTION_POLICY_ALLOWED);
            assertThat(mService.connect(d)).isTrue();
        }

        // verify
        Map<BluetoothDevice, MceStateMachine> map = mService.getInstanceMap();
        assertThat(map.size()).isEqualTo(MapClientService.MAXIMUM_CONNECTED_DEVICES);
        for (BluetoothDevice d : list) {
            assertThat(map).containsKey(d);
        }

        // Try to connect one more device. Should fail.
        BluetoothDevice last = makeBluetoothDevice("11:22:33:44:55:66");
        assertThat(mService.connect(last)).isFalse();
    }

    /** Test calling connect via Binder */
    @Test
    public void testConnectViaBinder() {
        BluetoothDevice device = makeBluetoothDevice("11:11:11:11:11:11");
        mockDevicePriority(device, BluetoothProfile.CONNECTION_POLICY_ALLOWED);
        Utils.setForegroundUserId(UserHandle.getCallingUserId());
        assertThat(mService.connect(device)).isTrue();
    }

    private BluetoothDevice makeBluetoothDevice(String address) {
        return mAdapter.getRemoteDevice(address);
    }
}
