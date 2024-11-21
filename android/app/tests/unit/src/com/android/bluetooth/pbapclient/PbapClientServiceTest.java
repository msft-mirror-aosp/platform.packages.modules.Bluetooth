/*
 * Copyright 2018 The Android Open Source Project
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
package com.android.bluetooth.pbapclient;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.Intent;
import android.net.Uri;
import android.os.Looper;
import android.provider.CallLog;
import android.test.mock.MockContentProvider;
import android.test.mock.MockContentResolver;

import androidx.test.filters.MediumTest;
import androidx.test.runner.AndroidJUnit4;

import com.android.bluetooth.TestUtils;
import com.android.bluetooth.btservice.AdapterService;
import com.android.bluetooth.btservice.storage.DatabaseManager;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class PbapClientServiceTest {
    private static final String REMOTE_DEVICE_ADDRESS = "00:00:00:00:00:00";

    private PbapClientService mService = null;
    private BluetoothAdapter mAdapter = null;
    private BluetoothDevice mRemoteDevice;

    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
    @Mock private Context mMockContext;
    @Mock private AdapterService mMockAdapterService;
    @Mock private DatabaseManager mDatabaseManager;
    @Mock private PackageManager mMockPackageManager;
    private MockContentResolver mMockContentResolver;
    private MockCallLogProvider mMockCallLogProvider;
    @Mock private Resources mMockResources;
    @Mock private AccountManager mMockAccountManager;

    @Before
    public void setUp() throws Exception {
        TestUtils.setAdapterService(mMockAdapterService);
        doReturn(mDatabaseManager).when(mMockAdapterService).getDatabase();

        doReturn("").when(mMockContext).getPackageName();
        doReturn(mMockPackageManager).when(mMockContext).getPackageManager();

        doReturn(mMockResources).when(mMockContext).getResources();
        doReturn(Utils.ACCOUNT_TYPE).when(mMockResources).getString(anyInt());

        mMockContentResolver = new MockContentResolver();
        mMockCallLogProvider = new MockCallLogProvider();
        mMockContentResolver.addProvider(CallLog.AUTHORITY, mMockCallLogProvider);
        doReturn(mMockContentResolver).when(mMockContext).getContentResolver();

        doReturn(AccountManager.VISIBILITY_VISIBLE)
                .when(mMockAccountManager)
                        .getAccountVisibility(any(Account.class), anyString());
        doReturn(new Account[]{})
                .when(mMockAccountManager)
                        .getAccountsByType(eq(Utils.ACCOUNT_TYPE));
        TestUtils.mockGetSystemService(
                mMockContext,
                Context.ACCOUNT_SERVICE,
                AccountManager.class,
                mMockAccountManager);

        mAdapter = BluetoothAdapter.getDefaultAdapter();
        Assert.assertNotNull(mAdapter);
        mRemoteDevice = mAdapter.getRemoteDevice(REMOTE_DEVICE_ADDRESS);

        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        mService = new PbapClientService(mMockContext);
        mService.start();
        mService.setAvailable(true);
    }

    @After
    public void tearDown() throws Exception {
        if (mService != null) {
            mService.stop();
            mService = null;
        }
        TestUtils.clearAdapterService(mMockAdapterService);
    }

    // *********************************************************************************************
    // * Initialize Service
    // *********************************************************************************************

    @Test
    public void testInitialize() {
        Assert.assertNotNull(PbapClientService.getPbapClientService());
    }

    @Test
    public void testSetPbapClientService_withNull() {
        PbapClientService.setPbapClientService(null);

        assertThat(PbapClientService.getPbapClientService()).isNull();
    }

    // *********************************************************************************************
    // * Incoming Events
    // *********************************************************************************************

    // ACL state changes from AdapterService

    @Test
    public void aclDisconnected_withLeTransport_doesNotCallDisconnect() {
        int connectionState = BluetoothProfile.STATE_CONNECTED;
        PbapClientStateMachine sm = mock(PbapClientStateMachine.class);
        mService.mPbapClientStateMachineMap.put(mRemoteDevice, sm);
        when(sm.getConnectionState(mRemoteDevice)).thenReturn(connectionState);

        mService.aclDisconnected(mRemoteDevice, BluetoothDevice.TRANSPORT_LE);
        TestUtils.waitForLooperToFinishScheduledTask(Looper.getMainLooper());

        verify(sm, never()).disconnect(mRemoteDevice);
    }

    @Test
    public void aclDisconnected_withBrEdrTransport_callsDisconnect() {
        int connectionState = BluetoothProfile.STATE_CONNECTED;
        PbapClientStateMachine sm = mock(PbapClientStateMachine.class);
        mService.mPbapClientStateMachineMap.put(mRemoteDevice, sm);
        when(sm.getConnectionState(mRemoteDevice)).thenReturn(connectionState);

        mService.aclDisconnected(mRemoteDevice, BluetoothDevice.TRANSPORT_BREDR);
        TestUtils.waitForLooperToFinishScheduledTask(Looper.getMainLooper());

        verify(sm).disconnect(mRemoteDevice);
    }

    // User unlock state changes

    @Test
    public void broadcastReceiver_withActionUserUnlocked_callsTryDownloadIfConnected() {
        PbapClientStateMachine sm = mock(PbapClientStateMachine.class);
        mService.mPbapClientStateMachineMap.put(mRemoteDevice, sm);

        Intent intent = new Intent(Intent.ACTION_USER_UNLOCKED);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, mRemoteDevice);
        mService.mPbapBroadcastReceiver.onReceive(mService, intent);

        verify(sm).tryDownloadIfConnected();
    }

    // HFP HF State changes

    @Test
    public void headsetClientConnectionStateChanged_hfpCallLogIsRemoved() {
        mService.handleHeadsetClientConnectionStateChanged(
                mRemoteDevice,
                BluetoothProfile.STATE_CONNECTED,
                BluetoothProfile.STATE_DISCONNECTED);

        assertThat(mMockCallLogProvider.getMostRecentlyDeletedDevice())
                .isEqualTo(mRemoteDevice.getAddress());
    }

    // Device state machines cleans up

    @Test
    public void cleanUpDevice() {
        PbapClientStateMachine sm = mock(PbapClientStateMachine.class);
        mService.mPbapClientStateMachineMap.put(mRemoteDevice, sm);

        mService.cleanupDevice(mRemoteDevice);

        assertThat(mService.mPbapClientStateMachineMap).doesNotContainKey(mRemoteDevice);
    }

    // *********************************************************************************************
    // * API Methods
    // *********************************************************************************************

    @Test
    public void testSetConnectionPolicy_withNullDevice_throwsIAE() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        mService.setConnectionPolicy(
                                null, BluetoothProfile.CONNECTION_POLICY_ALLOWED));
    }

    @Test
    public void testSetConnectionPolicy() {
        int connectionPolicy = BluetoothProfile.CONNECTION_POLICY_UNKNOWN;
        when(mDatabaseManager.setProfileConnectionPolicy(
                        mRemoteDevice, BluetoothProfile.PBAP_CLIENT, connectionPolicy))
                .thenReturn(true);

        assertThat(mService.setConnectionPolicy(mRemoteDevice, connectionPolicy)).isTrue();
    }

    @Test
    public void testGetConnectionPolicy_withNullDevice_throwsIAE() {
        assertThrows(IllegalArgumentException.class, () -> mService.getConnectionPolicy(null));
    }

    @Test
    public void testGetConnectionPolicy() {
        int connectionPolicy = BluetoothProfile.CONNECTION_POLICY_ALLOWED;
        when(mDatabaseManager.getProfileConnectionPolicy(
                        mRemoteDevice, BluetoothProfile.PBAP_CLIENT))
                .thenReturn(connectionPolicy);

        assertThat(mService.getConnectionPolicy(mRemoteDevice)).isEqualTo(connectionPolicy);
    }

    @Test
    public void testConnect_withNullDevice_throwsIAE() {
        assertThrows(IllegalArgumentException.class, () -> mService.connect(null));
    }

    @Test
    public void testConnect_whenPolicyIsForbidden_returnsFalse() {
        int connectionPolicy = BluetoothProfile.CONNECTION_POLICY_FORBIDDEN;
        when(mDatabaseManager.getProfileConnectionPolicy(
                        mRemoteDevice, BluetoothProfile.PBAP_CLIENT))
                .thenReturn(connectionPolicy);

        assertThat(mService.connect(mRemoteDevice)).isFalse();
    }

    @Test
    public void testConnect_whenPolicyIsAllowed_returnsTrue() {
        int connectionPolicy = BluetoothProfile.CONNECTION_POLICY_ALLOWED;
        when(mDatabaseManager.getProfileConnectionPolicy(
                        mRemoteDevice, BluetoothProfile.PBAP_CLIENT))
                .thenReturn(connectionPolicy);

        assertThat(mService.connect(mRemoteDevice)).isTrue();
    }

    @Test
    public void testDisconnect_withNullDevice_throwsIAE() {
        assertThrows(IllegalArgumentException.class, () -> mService.disconnect(null));
    }

    @Test
    public void testDisconnect_whenNotConnected_returnsFalse() {
        assertThat(mService.disconnect(mRemoteDevice)).isFalse();
    }

    @Test
    public void testDisconnect_whenConnected_returnsTrue() {
        PbapClientStateMachine sm = mock(PbapClientStateMachine.class);
        mService.mPbapClientStateMachineMap.put(mRemoteDevice, sm);

        assertThat(mService.disconnect(mRemoteDevice)).isTrue();

        verify(sm).disconnect(mRemoteDevice);
    }

    @Test
    public void testGetConnectionState_whenNotConnected() {
        assertThat(mService.getConnectionState(mRemoteDevice))
                .isEqualTo(BluetoothProfile.STATE_DISCONNECTED);
    }

    // *********************************************************************************************
    // * Debug/Dump/toString()
    // *********************************************************************************************

    @Test
    public void dump_callsStateMachineDump() {
        PbapClientStateMachine sm = mock(PbapClientStateMachine.class);
        mService.mPbapClientStateMachineMap.put(mRemoteDevice, sm);
        StringBuilder builder = new StringBuilder();

        mService.dump(builder);

        verify(sm).dump(builder);
    }

    // *********************************************************************************************
    // * Fake Call Log Provider
    // *********************************************************************************************

    private static class MockCallLogProvider extends MockContentProvider {
        private String mMostRecentlyDeletedDevice = null;

        @Override
        public int delete(Uri uri, String selection, String[] selectionArgs) {
            if (selectionArgs != null && selectionArgs.length > 0) {
                mMostRecentlyDeletedDevice = selectionArgs[0];
            }
            return 0;
        }

        public String getMostRecentlyDeletedDevice() {
            return mMostRecentlyDeletedDevice;
        }
    }
}
