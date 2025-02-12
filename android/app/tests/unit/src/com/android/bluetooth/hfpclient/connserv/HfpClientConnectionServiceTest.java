/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.bluetooth.hfpclient;

import static com.android.bluetooth.TestUtils.getTestDevice;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;

import androidx.test.filters.MediumTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import com.android.bluetooth.R;
import com.android.bluetooth.TestUtils;
import com.android.bluetooth.btservice.AdapterService;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.List;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class HfpClientConnectionServiceTest {
    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock private AdapterService mAdapterService;
    @Mock private HeadsetClientService mMockHeadsetClientService;
    @Mock private TelecomManager mMockTelecomManager;
    @Mock private Resources mMockResources;

    private static final String TEST_NUMBER = "000-111-2222";

    private final BluetoothDevice mDevice = getTestDevice(54);

    private HfpClientConnectionService mHfpClientConnectionService;

    @Before
    public void setUp() {
        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        TestUtils.setAdapterService(mAdapterService);
        // Setup a mock TelecomManager so our calls don't start a real instance of this service
        doNothing().when(mMockTelecomManager).addNewIncomingCall(any(), any());
        doNothing().when(mMockTelecomManager).addNewUnknownCall(any(), any());

        // Set a mocked HeadsetClientService for testing so we can insure the right functions were
        // called through the service interface
        when(mMockHeadsetClientService.isAvailable()).thenReturn(true);
        HeadsetClientService.setHeadsetClientService(mMockHeadsetClientService);

        // Spy the connection service under test so we can mock some of the system services and keep
        // them from impacting the actual system. Note: Another way to do this would be to extend
        // the class under test with a constructor taking a mock context that we inject using
        // attachBaseContext, but until we need a full context this is simpler.
        mHfpClientConnectionService = spy(new HfpClientConnectionService());

        doReturn("com.android.bluetooth.hfpclient")
                .when(mHfpClientConnectionService)
                .getPackageName();
        doReturn(mHfpClientConnectionService)
                .when(mHfpClientConnectionService)
                .getApplicationContext();
        doReturn(mMockResources).when(mHfpClientConnectionService).getResources();
        doReturn(true)
                .when(mMockResources)
                .getBoolean(R.bool.hfp_client_connection_service_support_emergency_call);

        doReturn(Context.TELECOM_SERVICE)
                .when(mHfpClientConnectionService)
                .getSystemServiceName(TelecomManager.class);
        doReturn(mMockTelecomManager)
                .when(mHfpClientConnectionService)
                .getSystemService(Context.TELECOM_SERVICE);
        doReturn(getPhoneAccount(mDevice)).when(mMockTelecomManager).getPhoneAccount(any());

        doReturn(Context.BLUETOOTH_SERVICE)
                .when(mHfpClientConnectionService)
                .getSystemServiceName(BluetoothManager.class);
        doReturn(targetContext.getSystemService(BluetoothManager.class))
                .when(mHfpClientConnectionService)
                .getSystemService(Context.BLUETOOTH_SERVICE);
    }

    @After
    public void tearDown() {
        TestUtils.clearAdapterService(mAdapterService);
    }

    private void createService() {
        mHfpClientConnectionService.onCreate();
    }

    private PhoneAccountHandle getPhoneAccountHandle(BluetoothDevice device) {
        return new PhoneAccountHandle(
                new ComponentName(mHfpClientConnectionService, HfpClientConnectionService.class),
                device.getAddress());
    }

    private PhoneAccount getPhoneAccount(BluetoothDevice device) {
        PhoneAccountHandle handle = getPhoneAccountHandle(device);
        Uri uri = Uri.fromParts(HfpClientConnectionService.HFP_SCHEME, device.getAddress(), null);
        return new PhoneAccount.Builder(handle, "HFP " + device.toString())
                .setAddress(uri)
                .setSupportedUriSchemes(Arrays.asList(PhoneAccount.SCHEME_TEL))
                .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER)
                .build();
    }

    private void setupDeviceConnection(BluetoothDevice device) throws Exception {
        mHfpClientConnectionService.onConnectionStateChanged(
                device, BluetoothProfile.STATE_CONNECTED, BluetoothProfile.STATE_CONNECTING);
        HfpClientDeviceBlock block = mHfpClientConnectionService.findBlockForDevice(mDevice);
        assertThat(block).isNotNull();
        assertThat(block.getDevice()).isEqualTo(mDevice);
    }

    @Test
    public void startServiceWithAlreadyConnectedDevice_blockIsCreated() throws Exception {
        when(mMockHeadsetClientService.getConnectedDevices()).thenReturn(List.of(mDevice));
        createService();
        HfpClientDeviceBlock block = mHfpClientConnectionService.findBlockForDevice(mDevice);
        assertThat(block).isNotNull();
        assertThat(block.getDevice()).isEqualTo(mDevice);
    }

    @Test
    public void ConnectDevice_blockIsCreated() throws Exception {
        createService();
        setupDeviceConnection(mDevice);
    }

    @Test
    public void disconnectDevice_blockIsRemoved() throws Exception {
        createService();
        setupDeviceConnection(mDevice);
        HfpClientConnectionService.onConnectionStateChanged(
                mDevice, BluetoothProfile.STATE_DISCONNECTED, BluetoothProfile.STATE_CONNECTED);
        assertThat(mHfpClientConnectionService.findBlockForDevice(mDevice)).isNull();
    }

    @Test
    public void callChanged_callAdded() throws Exception {
        createService();
        setupDeviceConnection(mDevice);
        HfpClientCall call =
                new HfpClientCall(
                        mDevice,
                        /* id= */ 0,
                        HfpClientCall.CALL_STATE_ACTIVE,
                        /* number= */ TEST_NUMBER,
                        /* multiParty= */ false,
                        /* outgoing= */ false,
                        /* inBandRing= */ true);
        HfpClientConnectionService.onCallChanged(mDevice, call);
        HfpClientDeviceBlock block = mHfpClientConnectionService.findBlockForDevice(mDevice);
        assertThat(block).isNotNull();
        assertThat(block.getDevice()).isEqualTo(mDevice);
        assertThat(block.getCalls().containsKey(call.getUUID())).isTrue();
    }

    @Test
    public void audioStateChanged_scoStateChanged() throws Exception {
        createService();
        setupDeviceConnection(mDevice);
        HfpClientConnectionService.onAudioStateChanged(
                mDevice,
                HeadsetClientHalConstants.AUDIO_STATE_CONNECTED,
                HeadsetClientHalConstants.AUDIO_STATE_CONNECTING);
        HfpClientDeviceBlock block = mHfpClientConnectionService.findBlockForDevice(mDevice);
        assertThat(block).isNotNull();
        assertThat(block.getDevice()).isEqualTo(mDevice);
        assertThat(block.getAudioState())
                .isEqualTo(HeadsetClientHalConstants.AUDIO_STATE_CONNECTED);
    }

    // TODO: b/393810023 - re-enable when ConnectionRequest can be mocked
    // @Test
    // public void onCreateIncomingConnection() throws Exception {
    //     createService();
    //     setupDeviceConnection(mDevice);

    //     HfpClientCall call =
    //             new HfpClientCall(
    //                     mDevice,
    //                     /* id= */ 0,
    //                     HfpClientCall.CALL_STATE_ACTIVE,
    //                     /* number= */ TEST_NUMBER,
    //                     /* multiParty= */ false,
    //                     /* outgoing= */ false,
    //                     /* inBandRing= */ true);

    //     Bundle extras = new Bundle();
    //     extras.putParcelable(
    //             TelecomManager.EXTRA_INCOMING_CALL_EXTRAS, new ParcelUuid(call.getUUID()));
    //     ConnectionRequest connectionRequest =
    //             new ConnectionRequest.Builder().setExtras(extras).build();

    //     HfpClientConnectionService.onCallChanged(mDevice, call);

    //     Connection connection =
    //             mHfpClientConnectionService.onCreateIncomingConnection(
    //                     getPhoneAccountHandle(mDevice), connectionRequest);

    //     assertThat(connection).isNotNull();
    //     assertThat(((HfpClientConnection) connection).getDevice()).isEqualTo(mDevice);
    //     assertThat(((HfpClientConnection) connection).getUUID()).isEqualTo(call.getUUID());
    // }

    // TODO: b/393810023 - re-enable when ConnectionRequest can be mocked
    // @Test
    // public void onCreateOutgoingConnection() throws Exception {
    //     createService();
    //     setupDeviceConnection(mDevice);

    //     HfpClientCall call =
    //             new HfpClientCall(
    //                     mDevice,
    //                     /* id= */ 0,
    //                     HfpClientCall.CALL_STATE_ACTIVE,
    //                     /* number= */ TEST_NUMBER,
    //                     /* multiParty= */ false,
    //                     /* outgoing= */ true,
    //                     /* inBandRing= */ true);

    //     doReturn(call).when(mMockHeadsetClientService).dial(mDevice, TEST_NUMBER);

    //     Bundle extras = new Bundle();
    //     extras.putParcelable(
    //             TelecomManager.EXTRA_OUTGOING_CALL_EXTRAS, new ParcelUuid(call.getUUID()));
    //     ConnectionRequest connectionRequest =
    //             new ConnectionRequest.Builder()
    //                     .setExtras(extras)
    //                     .setAddress(Uri.fromParts(PhoneAccount.SCHEME_TEL, TEST_NUMBER, null))
    //                     .build();

    //     Connection connection =
    //             mHfpClientConnectionService.onCreateOutgoingConnection(
    //                     getPhoneAccountHandle(mDevice), connectionRequest);

    //     assertThat(connection).isNotNull();
    //     assertThat(((HfpClientConnection) connection).getDevice()).isEqualTo(mDevice);
    //     assertThat(((HfpClientConnection) connection).getUUID()).isEqualTo(call.getUUID());
    // }

    // TODO: b/393810023 - re-enable when ConnectionRequest can be mocked
    // @Test
    // public void onCreateUnknownConnection() throws Exception {
    //     createService();
    //     setupDeviceConnection(mDevice);

    //     HfpClientCall call =
    //             new HfpClientCall(
    //                     mDevice,
    //                     /* id= */ 0,
    //                     HfpClientCall.CALL_STATE_ACTIVE,
    //                     /* number= */ TEST_NUMBER,
    //                     /* multiParty= */ false,
    //                     /* outgoing= */ true,
    //                     /* inBandRing= */ true);

    //     Bundle extras = new Bundle();
    //     extras.putParcelable(
    //             TelecomManager.EXTRA_OUTGOING_CALL_EXTRAS, new ParcelUuid(call.getUUID()));
    //     ConnectionRequest connectionRequest =
    //             new ConnectionRequest.Builder()
    //                     .setExtras(extras)
    //                     .setAddress(Uri.fromParts(PhoneAccount.SCHEME_TEL, TEST_NUMBER, null))
    //                     .build();

    //     HfpClientConnectionService.onCallChanged(mDevice, call);

    //     Connection connection =
    //             mHfpClientConnectionService.onCreateUnknownConnection(
    //                     getPhoneAccountHandle(mDevice), connectionRequest);

    //     assertThat(connection).isNotNull();
    //     assertThat(((HfpClientConnection) connection).getDevice()).isEqualTo(mDevice);
    //     assertThat(((HfpClientConnection) connection).getUUID()).isEqualTo(call.getUUID());
    // }

    // TODO: b/393810023 - re-enable when ConnectionRequest can be mocked
    // @Test
    // public void onCreateIncomingConnection_phoneAccountIsNull_returnsNull() throws Exception {
    //     doReturn(null).when(mMockTelecomManager).getPhoneAccount(any());
    //     createService();
    //     setupDeviceConnection(mDevice);

    //     HfpClientCall call =
    //             new HfpClientCall(
    //                     mDevice,
    //                     /* id= */ 0,
    //                     HfpClientCall.CALL_STATE_ACTIVE,
    //                     /* number= */ TEST_NUMBER,
    //                     /* multiParty= */ false,
    //                     /* outgoing= */ false,
    //                     /* inBandRing= */ true);

    //     Bundle extras = new Bundle();
    //     extras.putParcelable(
    //             TelecomManager.EXTRA_INCOMING_CALL_EXTRAS, new ParcelUuid(call.getUUID()));
    //     ConnectionRequest connectionRequest =
    //             new ConnectionRequest.Builder().setExtras(extras).build();

    //     HfpClientConnectionService.onCallChanged(mDevice, call);

    //     Connection connection =
    //             mHfpClientConnectionService.onCreateIncomingConnection(
    //                     getPhoneAccountHandle(mDevice), connectionRequest);

    //     assertThat(connection).isNull();
    // }

    // TODO: b/393810023 - re-enable when ConnectionRequest can be mocked
    // @Test
    // public void onCreateOutgoingConnection_phoneAccountIsNull_returnsNull() throws Exception {
    //     doReturn(null).when(mMockTelecomManager).getPhoneAccount(any());
    //     createService();
    //     setupDeviceConnection(mDevice);

    //     HfpClientCall call =
    //             new HfpClientCall(
    //                     mDevice,
    //                     /* id= */ 0,
    //                     HfpClientCall.CALL_STATE_ACTIVE,
    //                     /* number= */ TEST_NUMBER,
    //                     /* multiParty= */ false,
    //                     /* outgoing= */ true,
    //                     /* inBandRing= */ true);

    //     doReturn(call).when(mMockHeadsetClientService).dial(mDevice, TEST_NUMBER);

    //     Bundle extras = new Bundle();
    //     extras.putParcelable(
    //             TelecomManager.EXTRA_OUTGOING_CALL_EXTRAS, new ParcelUuid(call.getUUID()));
    //     ConnectionRequest connectionRequest =
    //             new ConnectionRequest.Builder()
    //                     .setExtras(extras)
    //                     .setAddress(Uri.fromParts(PhoneAccount.SCHEME_TEL, TEST_NUMBER, null))
    //                     .build();

    //     Connection connection =
    //             mHfpClientConnectionService.onCreateOutgoingConnection(
    //                     getPhoneAccountHandle(mDevice), connectionRequest);

    //     assertThat(connection).isNull();
    // }

    // TODO: b/393810023 - re-enable when ConnectionRequest can be mocked
    // @Test
    // public void onCreateUnknownConnection_phoneAccountIsNull_returnsNull() throws Exception {
    //     doReturn(null).when(mMockTelecomManager).getPhoneAccount(any());
    //     createService();
    //     setupDeviceConnection(mDevice);

    //     HfpClientCall call =
    //             new HfpClientCall(
    //                     mDevice,
    //                     /* id= */ 0,
    //                     HfpClientCall.CALL_STATE_ACTIVE,
    //                     /* number= */ TEST_NUMBER,
    //                     /* multiParty= */ false,
    //                     /* outgoing= */ true,
    //                     /* inBandRing= */ true);

    //     Bundle extras = new Bundle();
    //     extras.putParcelable(
    //             TelecomManager.EXTRA_OUTGOING_CALL_EXTRAS, new ParcelUuid(call.getUUID()));
    //     ConnectionRequest connectionRequest =
    //             new ConnectionRequest.Builder()
    //                     .setExtras(extras)
    //                     .setAddress(Uri.fromParts(PhoneAccount.SCHEME_TEL, TEST_NUMBER, null))
    //                     .build();

    //     HfpClientConnectionService.onCallChanged(mDevice, call);

    //     Connection connection =
    //             mHfpClientConnectionService.onCreateUnknownConnection(
    //                     getPhoneAccountHandle(mDevice), connectionRequest);

    //     assertThat(connection).isNull();
    // }
}
