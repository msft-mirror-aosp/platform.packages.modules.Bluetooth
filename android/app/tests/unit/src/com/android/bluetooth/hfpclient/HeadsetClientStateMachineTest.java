/*
 * Copyright 2016 The Android Open Source Project
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

import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;
import static android.content.pm.PackageManager.FEATURE_WATCH;

import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;

import static com.android.bluetooth.hfpclient.HeadsetClientStateMachine.AT_OK;
import static com.android.bluetooth.hfpclient.HeadsetClientStateMachine.ENTER_PRIVATE_MODE;
import static com.android.bluetooth.hfpclient.HeadsetClientStateMachine.EXPLICIT_CALL_TRANSFER;
import static com.android.bluetooth.hfpclient.HeadsetClientStateMachine.VOICE_RECOGNITION_START;
import static com.android.bluetooth.hfpclient.HeadsetClientStateMachine.VOICE_RECOGNITION_STOP;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.*;

import android.app.BroadcastOptions;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAssignedNumbers;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadsetClient;
import android.bluetooth.BluetoothHeadsetClientCall;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSinkAudioPolicy;
import android.bluetooth.BluetoothStatusCodes;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.platform.test.flag.junit.SetFlagsRule;
import android.util.Pair;

import androidx.test.filters.FlakyTest;
import androidx.test.filters.LargeTest;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import com.android.bluetooth.R;
import com.android.bluetooth.TestUtils;
import com.android.bluetooth.btservice.AdapterService;
import com.android.bluetooth.btservice.RemoteDevices;
import com.android.bluetooth.hfp.HeadsetService;

import org.hamcrest.Matcher;
import org.hamcrest.core.AllOf;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.hamcrest.MockitoHamcrest;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.List;
import java.util.Set;

/** Test cases for {@link HeadsetClientStateMachine}. */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class HeadsetClientStateMachineTest {
    private BluetoothAdapter mAdapter;
    private HandlerThread mHandlerThread;
    private TestHeadsetClientStateMachine mHeadsetClientStateMachine;
    private BluetoothDevice mTestDevice;

    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock private AdapterService mAdapterService;
    @Mock private Resources mMockHfpResources;
    @Mock private HeadsetService mHeadsetService;
    @Mock private HeadsetClientService mHeadsetClientService;
    @Mock private AudioManager mAudioManager;
    @Mock private RemoteDevices mRemoteDevices;
    @Mock private PackageManager mPackageManager;
    @Mock private NativeInterface mNativeInterface;

    private static final int STANDARD_WAIT_MILLIS = 1000;
    private static final int QUERY_CURRENT_CALLS_WAIT_MILLIS = 2000;
    private static final int QUERY_CURRENT_CALLS_TEST_WAIT_MILLIS =
            QUERY_CURRENT_CALLS_WAIT_MILLIS * 3 / 2;
    private static final int TIMEOUT_MS = 1000;

    @Before
    public void setUp() throws Exception {
        // Setup mocks and test assets
        // Set a valid volume
        when(mAudioManager.getStreamVolume(anyInt())).thenReturn(2);
        when(mAudioManager.getStreamMaxVolume(anyInt())).thenReturn(10);
        when(mAudioManager.getStreamMinVolume(anyInt())).thenReturn(1);
        when(mHeadsetClientService.getAudioManager()).thenReturn(mAudioManager);
        when(mHeadsetClientService.getResources()).thenReturn(mMockHfpResources);
        when(mHeadsetClientService.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.hasSystemFeature(FEATURE_WATCH)).thenReturn(false);
        when(mMockHfpResources.getBoolean(R.bool.hfp_clcc_poll_during_call)).thenReturn(true);
        when(mMockHfpResources.getInteger(R.integer.hfp_clcc_poll_interval_during_call))
                .thenReturn(2000);

        doReturn(mRemoteDevices).when(mAdapterService).getRemoteDevices();
        doReturn(true).when(mNativeInterface).sendAndroidAt(anyObject(), anyString());

        // This line must be called to make sure relevant objects are initialized properly
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        // Get a device for testing
        mTestDevice = mAdapter.getRemoteDevice("00:01:02:03:04:05");

        // Setup thread and looper
        mHandlerThread = new HandlerThread("HeadsetClientStateMachineTestHandlerThread");
        mHandlerThread.start();
        // Manage looper execution in main test thread explicitly to guarantee timing consistency
        mHeadsetClientStateMachine =
                new TestHeadsetClientStateMachine(
                        mAdapterService,
                        mHeadsetClientService,
                        mHeadsetService,
                        mHandlerThread.getLooper(),
                        mNativeInterface);
        mHeadsetClientStateMachine.start();
        TestUtils.waitForLooperToFinishScheduledTask(mHandlerThread.getLooper());
    }

    @After
    public void tearDown() throws Exception {
        TestUtils.waitForLooperToFinishScheduledTask(mHandlerThread.getLooper());
        mHeadsetClientStateMachine.allowConnect = null;
        mHeadsetClientStateMachine.doQuit();
        TestUtils.waitForLooperToFinishScheduledTask(mHandlerThread.getLooper());
        mHandlerThread.quit();
        verifyNoMoreInteractions(mHeadsetService);
    }

    /** Test that default state is disconnected */
    @SmallTest
    @Test
    public void testDefaultDisconnectedState() {
        assertThat(mHeadsetClientStateMachine.getConnectionState(null))
                .isEqualTo(BluetoothProfile.STATE_DISCONNECTED);
    }

    /** Test that an incoming connection with low priority is rejected */
    @MediumTest
    @Test
    public void testIncomingPriorityReject() {
        // Return false for priority.
        when(mHeadsetClientService.getConnectionPolicy(any(BluetoothDevice.class)))
                .thenReturn(BluetoothProfile.CONNECTION_POLICY_FORBIDDEN);

        // Inject an event for when incoming connection is requested
        StackEvent connStCh = new StackEvent(StackEvent.EVENT_TYPE_CONNECTION_STATE_CHANGED);
        connStCh.valueInt = HeadsetClientHalConstants.CONNECTION_STATE_CONNECTED;
        connStCh.device = mTestDevice;
        mHeadsetClientStateMachine.sendMessage(StackEvent.STACK_EVENT, connStCh);

        // Verify that only DISCONNECTED -> DISCONNECTED broadcast is fired
        verifySendBroadcastMultiplePermissions(
                hasAction(BluetoothHeadsetClient.ACTION_CONNECTION_STATE_CHANGED),
                hasExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED),
                hasExtra(
                        BluetoothProfile.EXTRA_PREVIOUS_STATE,
                        BluetoothProfile.STATE_DISCONNECTED));
        assertThat(mHeadsetClientStateMachine.getCurrentState())
                .isInstanceOf(HeadsetClientStateMachine.Disconnected.class);
    }

    /** Test that an incoming connection with high priority is accepted */
    @MediumTest
    @Test
    public void testIncomingPriorityAccept() {
        // Return true for priority.
        when(mHeadsetClientService.getConnectionPolicy(any(BluetoothDevice.class)))
                .thenReturn(BluetoothProfile.CONNECTION_POLICY_ALLOWED);

        // Inject an event for when incoming connection is requested
        StackEvent connStCh = new StackEvent(StackEvent.EVENT_TYPE_CONNECTION_STATE_CHANGED);
        connStCh.valueInt = HeadsetClientHalConstants.CONNECTION_STATE_CONNECTED;
        connStCh.device = mTestDevice;
        mHeadsetClientStateMachine.sendMessage(StackEvent.STACK_EVENT, connStCh);

        // Verify that one connection state broadcast is executed
        verifySendBroadcastMultiplePermissions(
                hasExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_CONNECTING));

        assertThat(mHeadsetClientStateMachine.getCurrentState())
                .isInstanceOf(HeadsetClientStateMachine.Connecting.class);

        // Send a message to trigger SLC connection
        StackEvent slcEvent = new StackEvent(StackEvent.EVENT_TYPE_CONNECTION_STATE_CHANGED);
        slcEvent.valueInt = HeadsetClientHalConstants.CONNECTION_STATE_SLC_CONNECTED;
        slcEvent.valueInt2 = HeadsetClientHalConstants.PEER_FEAT_ECS;
        slcEvent.device = mTestDevice;
        mHeadsetClientStateMachine.sendMessage(StackEvent.STACK_EVENT, slcEvent);
        TestUtils.waitForLooperToFinishScheduledTask(mHandlerThread.getLooper());

        setUpAndroidAt(false);

        // Verify that one connection state broadcast is executed
        verifySendBroadcastMultiplePermissions(
                hasExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_CONNECTED));
        assertThat(mHeadsetClientStateMachine.getCurrentState())
                .isInstanceOf(HeadsetClientStateMachine.Connected.class);
        verify(mHeadsetService).updateInbandRinging(eq(mTestDevice), eq(true));
    }

    /** Test that an incoming connection that times out */
    @MediumTest
    @Test
    public void testIncomingTimeout() {
        // Return true for priority.
        when(mHeadsetClientService.getConnectionPolicy(any(BluetoothDevice.class)))
                .thenReturn(BluetoothProfile.CONNECTION_POLICY_ALLOWED);

        // Inject an event for when incoming connection is requested
        StackEvent connStCh = new StackEvent(StackEvent.EVENT_TYPE_CONNECTION_STATE_CHANGED);
        connStCh.valueInt = HeadsetClientHalConstants.CONNECTION_STATE_CONNECTED;
        connStCh.device = mTestDevice;
        mHeadsetClientStateMachine.sendMessage(StackEvent.STACK_EVENT, connStCh);

        // Verify that one connection state broadcast is executed
        verifySendBroadcastMultiplePermissions(
                hasExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_CONNECTING));
        assertThat(mHeadsetClientStateMachine.getCurrentState())
                .isInstanceOf(HeadsetClientStateMachine.Connecting.class);

        // Verify that one connection state broadcast is executed
        verify(mHeadsetClientService, timeout(HeadsetClientStateMachine.CONNECTING_TIMEOUT_MS * 2))
                .sendBroadcastMultiplePermissions(
                        MockitoHamcrest.argThat(
                                hasExtra(
                                        BluetoothProfile.EXTRA_STATE,
                                        BluetoothProfile.STATE_DISCONNECTED)),
                        any(String[].class),
                        any(BroadcastOptions.class));
        assertThat(mHeadsetClientStateMachine.getCurrentState())
                .isInstanceOf(HeadsetClientStateMachine.Disconnected.class);
        verify(mHeadsetService).updateInbandRinging(eq(mTestDevice), eq(false));
    }

    @Test
    public void testProcessAndroidSlcCommand() {
        initToConnectedState();

        // True on correct AT command and BluetothDevice
        assertThat(
                        mHeadsetClientStateMachine.processAndroidSlcCommand(
                                "+ANDROID: (SINKAUDIOPOLICY)", mTestDevice))
                .isTrue();
        assertThat(mHeadsetClientStateMachine.processAndroidSlcCommand("+ANDROID: ()", mTestDevice))
                .isTrue();
        assertThat(
                        mHeadsetClientStateMachine.processAndroidSlcCommand(
                                "+ANDROID: (,,,)", mTestDevice))
                .isTrue();
        assertThat(
                        mHeadsetClientStateMachine.processAndroidSlcCommand(
                                "+ANDROID: (SINKAUDIOPOLICY),(OTHERFEATURE)", mTestDevice))
                .isTrue();
        assertThat(
                        mHeadsetClientStateMachine.processAndroidSlcCommand(
                                "+ANDROID: (SINKAUDIOPOLICY),(OTHERFEATURE,1,2,3),(1,2,3)",
                                mTestDevice))
                .isTrue();
        assertThat(
                        mHeadsetClientStateMachine.processAndroidSlcCommand(
                                "+ANDROID: 123", mTestDevice))
                .isTrue();
        assertThat(mHeadsetClientStateMachine.processAndroidSlcCommand("+ANDROID: ", mTestDevice))
                .isTrue();

        // False on incorrect AT command format
        assertThat(
                        mHeadsetClientStateMachine.processAndroidSlcCommand(
                                "+ANDROID= (SINKAUDIOPOLICY)", mTestDevice))
                .isFalse();
        assertThat(
                        mHeadsetClientStateMachine.processAndroidSlcCommand(
                                "RANDOM ^%$# STRING", mTestDevice))
                .isFalse();
        assertThat(mHeadsetClientStateMachine.processAndroidSlcCommand("", mTestDevice)).isFalse();

        // False on incorrect BluetoothDevice
        assertThat(
                        mHeadsetClientStateMachine.processAndroidSlcCommand(
                                "+ANDROID: (SINKAUDIOPOLICY)",
                                mAdapter.getRemoteDevice("05:04:01:02:03:00")))
                .isFalse();
    }

    @Test
    public void testProcessAndroidSlcCommand_checkSinkAudioPolicy() {
        initToConnectedState();

        mHeadsetClientStateMachine.setAudioPolicyRemoteSupported(false);
        assertThat(
                        mHeadsetClientStateMachine.processAndroidSlcCommand(
                                "RANDOM ^%$# STRING", mTestDevice))
                .isFalse();
        assertThat(mHeadsetClientStateMachine.getAudioPolicyRemoteSupported())
                .isEqualTo(BluetoothStatusCodes.FEATURE_NOT_SUPPORTED);

        mHeadsetClientStateMachine.setAudioPolicyRemoteSupported(false);
        assertThat(
                        mHeadsetClientStateMachine.processAndroidSlcCommand(
                                "+ANDROID= (SINKAUDIOPOLICY)", mTestDevice))
                .isFalse();
        assertThat(mHeadsetClientStateMachine.getAudioPolicyRemoteSupported())
                .isEqualTo(BluetoothStatusCodes.FEATURE_NOT_SUPPORTED);

        mHeadsetClientStateMachine.setAudioPolicyRemoteSupported(false);
        assertThat(
                        mHeadsetClientStateMachine.processAndroidSlcCommand(
                                "+ANDROID: (SINKAUDIOPOLICY)", mTestDevice))
                .isTrue();
        assertThat(mHeadsetClientStateMachine.getAudioPolicyRemoteSupported())
                .isEqualTo(BluetoothStatusCodes.FEATURE_SUPPORTED);
    }

    /** Test that In Band Ringtone information is relayed from phone. */
    @LargeTest
    @Test
    @FlakyTest
    public void testInBandRingtone() {
        // Return true for priority.
        when(mHeadsetClientService.getConnectionPolicy(any(BluetoothDevice.class)))
                .thenReturn(BluetoothProfile.CONNECTION_POLICY_ALLOWED);

        assertThat(mHeadsetClientStateMachine.getInBandRing()).isFalse();

        // Inject an event for when incoming connection is requested
        StackEvent connStCh = new StackEvent(StackEvent.EVENT_TYPE_CONNECTION_STATE_CHANGED);
        connStCh.valueInt = HeadsetClientHalConstants.CONNECTION_STATE_CONNECTED;
        connStCh.device = mTestDevice;
        mHeadsetClientStateMachine.sendMessage(StackEvent.STACK_EVENT, connStCh);

        // Verify that one connection state broadcast is executed
        verifySendBroadcastMultiplePermissions(
                hasExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_CONNECTING));

        // Send a message to trigger SLC connection
        StackEvent slcEvent = new StackEvent(StackEvent.EVENT_TYPE_CONNECTION_STATE_CHANGED);
        slcEvent.valueInt = HeadsetClientHalConstants.CONNECTION_STATE_SLC_CONNECTED;
        slcEvent.valueInt2 = HeadsetClientHalConstants.PEER_FEAT_ECS;
        slcEvent.device = mTestDevice;
        mHeadsetClientStateMachine.sendMessage(StackEvent.STACK_EVENT, slcEvent);
        TestUtils.waitForLooperToFinishScheduledTask(mHandlerThread.getLooper());

        setUpAndroidAt(false);

        verifySendBroadcastMultiplePermissions(
                hasExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_CONNECTED));

        verify(mHeadsetService).updateInbandRinging(eq(mTestDevice), eq(true));

        StackEvent event = new StackEvent(StackEvent.EVENT_TYPE_IN_BAND_RINGTONE);
        event.valueInt = 0;
        event.device = mTestDevice;

        // Enable In Band Ring and verify state gets propagated.
        StackEvent eventInBandRing = new StackEvent(StackEvent.EVENT_TYPE_IN_BAND_RINGTONE);
        eventInBandRing.valueInt = 1;
        eventInBandRing.device = mTestDevice;
        mHeadsetClientStateMachine.sendMessage(StackEvent.STACK_EVENT, eventInBandRing);
        verifySendBroadcast(hasExtra(BluetoothHeadsetClient.EXTRA_IN_BAND_RING, 1));
        assertThat(mHeadsetClientStateMachine.getInBandRing()).isTrue();

        // Simulate a new incoming phone call
        StackEvent eventCallStatusUpdated = new StackEvent(StackEvent.EVENT_TYPE_CLIP);
        TestUtils.waitForLooperToFinishScheduledTask(mHandlerThread.getLooper());
        mHeadsetClientStateMachine.sendMessage(StackEvent.STACK_EVENT, eventCallStatusUpdated);
        TestUtils.waitForLooperToFinishScheduledTask(mHandlerThread.getLooper());
        verify(mHeadsetClientService, timeout(STANDARD_WAIT_MILLIS))
                .sendBroadcast(any(Intent.class), anyString(), any(Bundle.class));

        // Provide information about the new call
        StackEvent eventIncomingCall = new StackEvent(StackEvent.EVENT_TYPE_CURRENT_CALLS);
        eventIncomingCall.valueInt = 1; // index
        eventIncomingCall.valueInt2 = 1; // direction
        eventIncomingCall.valueInt3 = 4; // state
        eventIncomingCall.valueInt4 = 0; // multi party
        eventIncomingCall.valueString = "5551212"; // phone number
        eventIncomingCall.device = mTestDevice;

        mHeadsetClientStateMachine.sendMessage(StackEvent.STACK_EVENT, eventIncomingCall);
        verify(mHeadsetClientService, timeout(STANDARD_WAIT_MILLIS))
                .sendBroadcast(any(Intent.class), anyString(), any(Bundle.class));

        // Signal that the complete list of calls was received.
        StackEvent eventCommandStatus = new StackEvent(StackEvent.EVENT_TYPE_CMD_RESULT);
        eventCommandStatus.valueInt = AT_OK;
        mHeadsetClientStateMachine.sendMessage(StackEvent.STACK_EVENT, eventCommandStatus);
        TestUtils.waitForLooperToFinishScheduledTask(mHandlerThread.getLooper());

        ArgumentCaptor<Intent> intentArgument = ArgumentCaptor.forClass(Intent.class);
        verify(mHeadsetClientService, timeout(QUERY_CURRENT_CALLS_TEST_WAIT_MILLIS).times(2))
                .sendBroadcast(intentArgument.capture(), anyString(), any(Bundle.class));
        // Verify that the new call is being registered with the inBandRing flag set.
        assertThat(
                        ((HfpClientCall)
                                        intentArgument
                                                .getValue()
                                                .getParcelableExtra(
                                                        BluetoothHeadsetClient.EXTRA_CALL))
                                .isInBandRing())
                .isTrue();

        // Disable In Band Ring and verify state gets propagated.
        eventInBandRing.valueInt = 0;
        mHeadsetClientStateMachine.sendMessage(StackEvent.STACK_EVENT, eventInBandRing);
        verifySendBroadcast(hasExtra(BluetoothHeadsetClient.EXTRA_IN_BAND_RING, 0));
        assertThat(mHeadsetClientStateMachine.getInBandRing()).isFalse();
    }

    /** Test that wearables use {@code BluetoothHeadsetClientCall} in intent. */
    @Test
    public void testWearablesUseBluetoothHeadsetClientCallInIntent() {
        // Specify the watch form factor when package manager is asked
        when(mPackageManager.hasSystemFeature(FEATURE_WATCH)).thenReturn(true);

        // Skip over the Android AT commands to test this code path
        doReturn(false).when(mNativeInterface).sendAndroidAt(anyObject(), anyString());

        // Return true for connection policy to allow connections
        when(mHeadsetClientService.getConnectionPolicy(any(BluetoothDevice.class)))
                .thenReturn(BluetoothProfile.CONNECTION_POLICY_ALLOWED);

        // Send an incoming connection event
        StackEvent event = new StackEvent(StackEvent.EVENT_TYPE_CONNECTION_STATE_CHANGED);
        event.device = mTestDevice;
        event.valueInt = HeadsetClientHalConstants.CONNECTION_STATE_CONNECTED;
        mHeadsetClientStateMachine.sendMessage(StackEvent.STACK_EVENT, event);

        // Send a message to trigger service level connection using the required ECS feature
        event = new StackEvent(StackEvent.EVENT_TYPE_CONNECTION_STATE_CHANGED);
        event.device = mTestDevice;
        event.valueInt = HeadsetClientHalConstants.CONNECTION_STATE_SLC_CONNECTED;
        event.valueInt2 = HeadsetClientHalConstants.PEER_FEAT_ECS;
        mHeadsetClientStateMachine.sendMessage(StackEvent.STACK_EVENT, event);

        // Dial a phone call, which will fail as @{code dial} method is not specified in @{code
        // mNativeInterface} mock and trigger a call state changed broadcast
        mHeadsetClientStateMachine.sendMessage(
                HeadsetClientStateMachine.DIAL_NUMBER,
                new HfpClientCall(
                        mTestDevice,
                        0,
                        HfpClientCall.CALL_STATE_WAITING,
                        "1",
                        false,
                        false,
                        false));

        // Wait for processing
        TestUtils.waitForLooperToFinishScheduledTask(mHandlerThread.getLooper());

        // Verify the broadcast
        ArgumentCaptor<Intent> intentArgument = ArgumentCaptor.forClass(Intent.class);
        verify(mHeadsetClientService)
                .sendBroadcast(intentArgument.capture(), anyString(), any(Bundle.class));

        // Verify that the parcelable extra has a legacy {@code BluetoothHeadsetClientCall} type for
        // wearables.
        assertThat(
                        (Object)
                                intentArgument
                                        .getValue()
                                        .getParcelableExtra(BluetoothHeadsetClient.EXTRA_CALL))
                .isInstanceOf(BluetoothHeadsetClientCall.class);

        // To satisfy the @After verification
        verify(mHeadsetService).updateInbandRinging(eq(mTestDevice), eq(true));
    }

    /* Utility function to simulate HfpClient is connected. */
    private void setUpHfpClientConnection() {
        // Trigger an incoming connection is requested
        StackEvent connStCh = new StackEvent(StackEvent.EVENT_TYPE_CONNECTION_STATE_CHANGED);
        connStCh.valueInt = HeadsetClientHalConstants.CONNECTION_STATE_CONNECTED;
        connStCh.device = mTestDevice;
        mHeadsetClientStateMachine.sendMessage(StackEvent.STACK_EVENT, connStCh);
        verifySendBroadcastMultiplePermissions(
                hasExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_CONNECTING));
    }

    /* Utility function to simulate SLC connection. */
    private void setUpServiceLevelConnection() {
        setUpServiceLevelConnection(false);
    }

    private void setUpServiceLevelConnection(boolean androidAtSupported) {
        // Trigger SLC connection
        StackEvent slcEvent = new StackEvent(StackEvent.EVENT_TYPE_CONNECTION_STATE_CHANGED);
        slcEvent.valueInt = HeadsetClientHalConstants.CONNECTION_STATE_SLC_CONNECTED;
        slcEvent.valueInt2 = HeadsetClientHalConstants.PEER_FEAT_ECS;
        slcEvent.valueInt2 |= HeadsetClientHalConstants.PEER_FEAT_HF_IND;
        slcEvent.device = mTestDevice;
        mHeadsetClientStateMachine.sendMessage(StackEvent.STACK_EVENT, slcEvent);
        TestUtils.waitForLooperToFinishScheduledTask(mHandlerThread.getLooper());

        setUpAndroidAt(androidAtSupported);

        verifySendBroadcastMultiplePermissions(
                hasExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_CONNECTED));
        assertThat(mHeadsetClientStateMachine.getCurrentState())
                .isInstanceOf(HeadsetClientStateMachine.Connected.class);
        verify(mHeadsetService).updateInbandRinging(eq(mTestDevice), eq(true));
    }

    /**
     * Set up and verify AT Android related commands and events. Make sure this method is invoked
     * after SLC is setup.
     */
    private void setUpAndroidAt(boolean androidAtSupported) {
        verify(mNativeInterface).sendAndroidAt(mTestDevice, "+ANDROID=?");
        if (androidAtSupported) {
            // inject Android AT features
            StackEvent unknownEvt = new StackEvent(StackEvent.EVENT_TYPE_UNKNOWN_EVENT);
            unknownEvt.valueString = "+ANDROID: (SINKAUDIOPOLICY)";
            unknownEvt.device = mTestDevice;
            mHeadsetClientStateMachine.sendMessage(StackEvent.STACK_EVENT, unknownEvt);
            TestUtils.waitForLooperToFinishScheduledTask(mHandlerThread.getLooper());

            // receive CMD_RESULT OK after the Android AT command from remote
            StackEvent cmdResEvt = new StackEvent(StackEvent.EVENT_TYPE_CMD_RESULT);
            cmdResEvt.valueInt = StackEvent.CMD_RESULT_TYPE_OK;
            cmdResEvt.device = mTestDevice;
            mHeadsetClientStateMachine.sendMessage(StackEvent.STACK_EVENT, cmdResEvt);
            TestUtils.waitForLooperToFinishScheduledTask(mHandlerThread.getLooper());

            assertThat(mHeadsetClientStateMachine.getAudioPolicyRemoteSupported())
                    .isEqualTo(BluetoothStatusCodes.FEATURE_SUPPORTED);
        } else {
            // receive CMD_RESULT CME_ERROR due to remote not supporting Android AT
            StackEvent cmdResEvt = new StackEvent(StackEvent.EVENT_TYPE_CMD_RESULT);
            cmdResEvt.valueInt = StackEvent.CMD_RESULT_TYPE_CME_ERROR;
            cmdResEvt.device = mTestDevice;
            mHeadsetClientStateMachine.sendMessage(StackEvent.STACK_EVENT, cmdResEvt);
            TestUtils.waitForLooperToFinishScheduledTask(mHandlerThread.getLooper());

            assertThat(mHeadsetClientStateMachine.getAudioPolicyRemoteSupported())
                    .isEqualTo(BluetoothStatusCodes.FEATURE_NOT_SUPPORTED);
        }
    }

    /* Utility function: supported AT command should lead to native call */
    private void runSupportedVendorAtCommand(String atCommand, int vendorId) {
        // Return true for priority.
        when(mHeadsetClientService.getConnectionPolicy(any(BluetoothDevice.class)))
                .thenReturn(BluetoothProfile.CONNECTION_POLICY_ALLOWED);

        setUpHfpClientConnection();
        setUpServiceLevelConnection();

        Message msg =
                mHeadsetClientStateMachine.obtainMessage(
                        HeadsetClientStateMachine.SEND_VENDOR_AT_COMMAND, vendorId, 0, atCommand);
        mHeadsetClientStateMachine.sendMessage(msg);

        verify(mNativeInterface, timeout(STANDARD_WAIT_MILLIS))
                .sendATCmd(
                        mTestDevice,
                        HeadsetClientHalConstants.HANDSFREECLIENT_AT_CMD_VENDOR_SPECIFIC_CMD,
                        0,
                        0,
                        atCommand);
    }

    /** Test: supported vendor specific command: set operation */
    @LargeTest
    @Test
    public void testSupportedVendorAtCommandSet() {
        int vendorId = BluetoothAssignedNumbers.APPLE;
        String atCommand = "+XAPL=ABCD-1234-0100,100";
        runSupportedVendorAtCommand(atCommand, vendorId);
    }

    /** Test: supported vendor specific command: read operation */
    @LargeTest
    @Test
    public void testSupportedVendorAtCommandRead() {
        int vendorId = BluetoothAssignedNumbers.APPLE;
        String atCommand = "+APLSIRI?";
        runSupportedVendorAtCommand(atCommand, vendorId);
    }

    /* utility function: unsupported vendor specific command shall be filtered. */
    public void runUnsupportedVendorAtCommand(String atCommand, int vendorId) {
        // Return true for priority.
        when(mHeadsetClientService.getConnectionPolicy(any(BluetoothDevice.class)))
                .thenReturn(BluetoothProfile.CONNECTION_POLICY_ALLOWED);

        setUpHfpClientConnection();
        setUpServiceLevelConnection();

        Message msg =
                mHeadsetClientStateMachine.obtainMessage(
                        HeadsetClientStateMachine.SEND_VENDOR_AT_COMMAND, vendorId, 0, atCommand);
        mHeadsetClientStateMachine.sendMessage(msg);

        verify(mNativeInterface, timeout(STANDARD_WAIT_MILLIS).times(0))
                .sendATCmd(any(), anyInt(), anyInt(), anyInt(), any());
    }

    /** Test: unsupported vendor specific command shall be filtered: bad command code */
    @LargeTest
    @Test
    public void testUnsupportedVendorAtCommandBadCode() {
        String atCommand = "+XAAPL=ABCD-1234-0100,100";
        int vendorId = BluetoothAssignedNumbers.APPLE;
        runUnsupportedVendorAtCommand(atCommand, vendorId);
    }

    /** Test: unsupported vendor specific command shall be filtered: no back to back command */
    @LargeTest
    @Test
    public void testUnsupportedVendorAtCommandBackToBack() {
        String atCommand = "+XAPL=ABCD-1234-0100,100; +XAPL=ab";
        int vendorId = BluetoothAssignedNumbers.APPLE;
        runUnsupportedVendorAtCommand(atCommand, vendorId);
    }

    /* Utility test function: supported vendor specific event
     * shall lead to broadcast intent
     */
    private void runSupportedVendorEvent(
            int vendorId, String vendorEventCode, String vendorEventArgument) {
        // Setup connection state machine to be in connected state
        when(mHeadsetClientService.getConnectionPolicy(any(BluetoothDevice.class)))
                .thenReturn(BluetoothProfile.CONNECTION_POLICY_ALLOWED);
        setUpHfpClientConnection();
        setUpServiceLevelConnection();

        // Simulate a known event arrive
        String vendorEvent = vendorEventCode + vendorEventArgument;
        StackEvent event = new StackEvent(StackEvent.EVENT_TYPE_UNKNOWN_EVENT);
        event.device = mTestDevice;
        event.valueString = vendorEvent;
        mHeadsetClientStateMachine.sendMessage(StackEvent.STACK_EVENT, event);

        // Validate broadcast intent
        verifySendBroadcast(
                hasAction(BluetoothHeadsetClient.ACTION_VENDOR_SPECIFIC_HEADSETCLIENT_EVENT),
                hasExtra(BluetoothHeadsetClient.EXTRA_VENDOR_ID, vendorId),
                hasExtra(BluetoothHeadsetClient.EXTRA_VENDOR_EVENT_CODE, vendorEventCode),
                hasExtra(BluetoothHeadsetClient.EXTRA_VENDOR_EVENT_FULL_ARGS, vendorEvent));
    }

    /** Test: supported vendor specific response: response to read command */
    @LargeTest
    @Test
    public void testSupportedVendorEventReadResponse() {
        final int vendorId = BluetoothAssignedNumbers.APPLE;
        final String vendorResponseCode = "+XAPL=";
        final String vendorResponseArgument = "iPhone,2";
        runSupportedVendorEvent(vendorId, vendorResponseCode, vendorResponseArgument);
    }

    /** Test: supported vendor specific response: response to test command */
    @LargeTest
    @Test
    public void testSupportedVendorEventTestResponse() {
        final int vendorId = BluetoothAssignedNumbers.APPLE;
        final String vendorResponseCode = "+APLSIRI:";
        final String vendorResponseArgumentWithSpace = "  2";
        runSupportedVendorEvent(vendorId, vendorResponseCode, vendorResponseArgumentWithSpace);
    }

    /* Utility test function: unsupported vendor specific response shall be filtered out*/
    public void runUnsupportedVendorEvent(
            int vendorId, String vendorEventCode, String vendorEventArgument) {
        // Setup connection state machine to be in connected state
        when(mHeadsetClientService.getConnectionPolicy(any(BluetoothDevice.class)))
                .thenReturn(BluetoothProfile.CONNECTION_POLICY_ALLOWED);
        setUpHfpClientConnection();
        setUpServiceLevelConnection();

        // Simulate an unknown event arrive
        String vendorEvent = vendorEventCode + vendorEventArgument;
        StackEvent event = new StackEvent(StackEvent.EVENT_TYPE_UNKNOWN_EVENT);
        event.device = mTestDevice;
        event.valueString = vendorEvent;
        mHeadsetClientStateMachine.sendMessage(StackEvent.STACK_EVENT, event);

        // Validate no broadcast intent
        verify(mHeadsetClientService, atMost(2))
                .sendBroadcast(any(), anyString(), any(Bundle.class));
    }

    /** Test unsupported vendor response: bad read response */
    @LargeTest
    @Test
    public void testUnsupportedVendorEventBadReadResponse() {
        final int vendorId = BluetoothAssignedNumbers.APPLE;
        final String vendorResponseCode = "+XAAPL=";
        final String vendorResponseArgument = "iPhone,2";
        runUnsupportedVendorEvent(vendorId, vendorResponseCode, vendorResponseArgument);
    }

    /** Test unsupported vendor response: bad test response */
    @LargeTest
    @Test
    public void testUnsupportedVendorEventBadTestResponse() {
        final int vendorId = BluetoothAssignedNumbers.APPLE;
        final String vendorResponseCode = "+AAPLSIRI:";
        final String vendorResponseArgument = "2";
        runUnsupportedVendorEvent(vendorId, vendorResponseCode, vendorResponseArgument);
    }

    /** Test voice recognition state change broadcast. */
    @MediumTest
    @Test
    public void testVoiceRecognitionStateChange() {
        // Setup connection state machine to be in connected state
        when(mHeadsetClientService.getConnectionPolicy(any(BluetoothDevice.class)))
                .thenReturn(BluetoothProfile.CONNECTION_POLICY_ALLOWED);
        doReturn(true).when(mNativeInterface).startVoiceRecognition(any(BluetoothDevice.class));
        doReturn(true).when(mNativeInterface).stopVoiceRecognition(any(BluetoothDevice.class));

        setUpHfpClientConnection();
        setUpServiceLevelConnection();

        // Simulate a voice recognition start
        mHeadsetClientStateMachine.sendMessage(VOICE_RECOGNITION_START);

        // Signal that the complete list of actions was received.
        StackEvent event = new StackEvent(StackEvent.EVENT_TYPE_CMD_RESULT);
        event.device = mTestDevice;
        event.valueInt = AT_OK;
        mHeadsetClientStateMachine.sendMessage(StackEvent.STACK_EVENT, event);

        verifySendBroadcast(
                hasAction(BluetoothHeadsetClient.ACTION_AG_EVENT),
                hasExtra(
                        BluetoothHeadsetClient.EXTRA_VOICE_RECOGNITION,
                        HeadsetClientHalConstants.VR_STATE_STARTED));

        // Simulate a voice recognition stop
        mHeadsetClientStateMachine.sendMessage(VOICE_RECOGNITION_STOP);

        // Signal that the complete list of actions was received.
        event = new StackEvent(StackEvent.EVENT_TYPE_CMD_RESULT);
        event.device = mTestDevice;
        event.valueInt = AT_OK;
        mHeadsetClientStateMachine.sendMessage(StackEvent.STACK_EVENT, event);

        verifySendBroadcast(
                hasAction(BluetoothHeadsetClient.ACTION_AG_EVENT),
                hasExtra(
                        BluetoothHeadsetClient.EXTRA_VOICE_RECOGNITION,
                        HeadsetClientHalConstants.VR_STATE_STOPPED));
    }

    /** Test send BIEV command */
    @MediumTest
    @Test
    public void testSendBIEVCommand() {
        // Setup connection state machine to be in connected state
        when(mHeadsetClientService.getConnectionPolicy(any(BluetoothDevice.class)))
                .thenReturn(BluetoothProfile.CONNECTION_POLICY_ALLOWED);
        setUpHfpClientConnection();
        setUpServiceLevelConnection();

        int indicator_id = 2;
        int indicator_value = 50;

        Message msg = mHeadsetClientStateMachine.obtainMessage(HeadsetClientStateMachine.SEND_BIEV);
        msg.arg1 = indicator_id;
        msg.arg2 = indicator_value;

        mHeadsetClientStateMachine.sendMessage(msg);

        verify(mNativeInterface, timeout(STANDARD_WAIT_MILLIS))
                .sendATCmd(
                        mTestDevice,
                        HeadsetClientHalConstants.HANDSFREECLIENT_AT_CMD_BIEV,
                        indicator_id,
                        indicator_value,
                        null);
    }

    /**
     * Test state machine shall try to send AT+BIEV command to AG to update an init battery level.
     */
    @MediumTest
    @Test
    public void testSendBatteryUpdateIndicatorWhenConnect() {
        // Setup connection state machine to be in connected state
        when(mHeadsetClientService.getConnectionPolicy(any(BluetoothDevice.class)))
                .thenReturn(BluetoothProfile.CONNECTION_POLICY_ALLOWED);

        setUpHfpClientConnection();
        setUpServiceLevelConnection();

        verify(mHeadsetClientService, timeout(STANDARD_WAIT_MILLIS)).updateBatteryLevel();
    }

    @Test
    public void testBroadcastAudioState() {
        mHeadsetClientStateMachine.broadcastAudioState(
                mTestDevice,
                BluetoothHeadsetClient.STATE_AUDIO_CONNECTED,
                BluetoothHeadsetClient.STATE_AUDIO_CONNECTING);

        verify(mHeadsetClientService).sendBroadcast(any(), any(), any());
    }

    @Test
    public void testCallsInState() {
        HfpClientCall call =
                new HfpClientCall(
                        mTestDevice, 0, HfpClientCall.CALL_STATE_WAITING, "1", false, false, false);
        mHeadsetClientStateMachine.mCalls.put(0, call);

        assertThat(mHeadsetClientStateMachine.callsInState(HfpClientCall.CALL_STATE_WAITING))
                .isEqualTo(1);
    }

    @Test
    public void testEnterPrivateMode() {
        HfpClientCall call =
                new HfpClientCall(
                        mTestDevice, 0, HfpClientCall.CALL_STATE_ACTIVE, "1", true, false, false);
        mHeadsetClientStateMachine.mCalls.put(0, call);
        doReturn(true)
                .when(mNativeInterface)
                .handleCallAction(null, HeadsetClientHalConstants.CALL_ACTION_CHLD_2X, 0);

        mHeadsetClientStateMachine.enterPrivateMode(0);

        Pair expectedPair = new Pair<Integer, Object>(ENTER_PRIVATE_MODE, call);
        assertThat(mHeadsetClientStateMachine.mQueuedActions.peek()).isEqualTo(expectedPair);
    }

    @Test
    public void testExplicitCallTransfer() {
        HfpClientCall callOne =
                new HfpClientCall(
                        mTestDevice, 0, HfpClientCall.CALL_STATE_ACTIVE, "1", true, false, false);
        HfpClientCall callTwo =
                new HfpClientCall(
                        mTestDevice, 0, HfpClientCall.CALL_STATE_ACTIVE, "1", true, false, false);
        mHeadsetClientStateMachine.mCalls.put(0, callOne);
        mHeadsetClientStateMachine.mCalls.put(1, callTwo);
        doReturn(true)
                .when(mNativeInterface)
                .handleCallAction(null, HeadsetClientHalConstants.CALL_ACTION_CHLD_4, -1);

        mHeadsetClientStateMachine.explicitCallTransfer();

        Pair expectedPair = new Pair<Integer, Object>(EXPLICIT_CALL_TRANSFER, 0);
        assertThat(mHeadsetClientStateMachine.mQueuedActions.peek()).isEqualTo(expectedPair);
    }

    @Test
    public void testSetAudioRouteAllowed() {
        mHeadsetClientStateMachine.setAudioRouteAllowed(true);

        assertThat(mHeadsetClientStateMachine.getAudioRouteAllowed()).isTrue();

        // Case 1: if remote is not supported
        // Expect: Should not send +ANDROID to remote
        mHeadsetClientStateMachine.mCurrentDevice = mTestDevice;
        mHeadsetClientStateMachine.setAudioPolicyRemoteSupported(false);
        verify(mNativeInterface, never())
                .sendAndroidAt(mTestDevice, "+ANDROID=SINKAUDIOPOLICY,1,0,0");

        // Case 2: if remote is supported and mForceSetAudioPolicyProperty is false
        // Expect: Should send +ANDROID=SINKAUDIOPOLICY,1,0,0 to remote
        mHeadsetClientStateMachine.setAudioPolicyRemoteSupported(true);
        mHeadsetClientStateMachine.setForceSetAudioPolicyProperty(false);
        mHeadsetClientStateMachine.setAudioRouteAllowed(true);
        verify(mNativeInterface).sendAndroidAt(mTestDevice, "+ANDROID=SINKAUDIOPOLICY,1,0,0");

        mHeadsetClientStateMachine.setAudioRouteAllowed(false);
        verify(mNativeInterface).sendAndroidAt(mTestDevice, "+ANDROID=SINKAUDIOPOLICY,2,0,0");

        // Case 3: if remote is supported and mForceSetAudioPolicyProperty is true
        // Expect: Should send +ANDROID=SINKAUDIOPOLICY,1,2,1 to remote
        mHeadsetClientStateMachine.setForceSetAudioPolicyProperty(true);
        mHeadsetClientStateMachine.setAudioRouteAllowed(true);
        verify(mNativeInterface).sendAndroidAt(mTestDevice, "+ANDROID=SINKAUDIOPOLICY,1,1,1");
    }

    @Test
    public void testGetAudioState_withCurrentDeviceNull() {
        assertThat(mHeadsetClientStateMachine.mCurrentDevice).isNull();

        assertThat(mHeadsetClientStateMachine.getAudioState(mTestDevice))
                .isEqualTo(BluetoothHeadsetClient.STATE_AUDIO_DISCONNECTED);
    }

    @Test
    public void testGetAudioState_withCurrentDeviceNotNull() {
        int audioState = 1;
        mHeadsetClientStateMachine.mAudioState = audioState;
        mHeadsetClientStateMachine.mCurrentDevice = mTestDevice;

        assertThat(mHeadsetClientStateMachine.getAudioState(mTestDevice)).isEqualTo(audioState);
    }

    @Test
    public void testGetCall_withMatchingState() {
        HfpClientCall call =
                new HfpClientCall(
                        mTestDevice, 0, HfpClientCall.CALL_STATE_ACTIVE, "1", true, false, false);
        mHeadsetClientStateMachine.mCalls.put(0, call);
        int[] states = new int[1];
        states[0] = HfpClientCall.CALL_STATE_ACTIVE;

        assertThat(mHeadsetClientStateMachine.getCall(states)).isEqualTo(call);
    }

    @Test
    public void testGetCall_withNoMatchingState() {
        HfpClientCall call =
                new HfpClientCall(
                        mTestDevice, 0, HfpClientCall.CALL_STATE_WAITING, "1", true, false, false);
        mHeadsetClientStateMachine.mCalls.put(0, call);
        int[] states = new int[1];
        states[0] = HfpClientCall.CALL_STATE_ACTIVE;

        assertThat(mHeadsetClientStateMachine.getCall(states)).isNull();
    }

    @Test
    public void testGetConnectionState_withNullDevice() {
        assertThat(mHeadsetClientStateMachine.getConnectionState(null))
                .isEqualTo(BluetoothProfile.STATE_DISCONNECTED);
    }

    @Test
    public void testGetConnectionState_withNonNullDevice() {
        mHeadsetClientStateMachine.mCurrentDevice = mTestDevice;

        assertThat(mHeadsetClientStateMachine.getConnectionState(mTestDevice))
                .isEqualTo(BluetoothProfile.STATE_DISCONNECTED);
    }

    @Test
    public void testGetConnectionStateFromAudioState() {
        assertThat(
                        HeadsetClientStateMachine.getConnectionStateFromAudioState(
                                BluetoothHeadsetClient.STATE_AUDIO_CONNECTED))
                .isEqualTo(BluetoothAdapter.STATE_CONNECTED);
        assertThat(
                        HeadsetClientStateMachine.getConnectionStateFromAudioState(
                                BluetoothHeadsetClient.STATE_AUDIO_CONNECTING))
                .isEqualTo(BluetoothAdapter.STATE_CONNECTING);
        assertThat(
                        HeadsetClientStateMachine.getConnectionStateFromAudioState(
                                BluetoothHeadsetClient.STATE_AUDIO_DISCONNECTED))
                .isEqualTo(BluetoothAdapter.STATE_DISCONNECTED);
        int invalidAudioState = 3;
        assertThat(HeadsetClientStateMachine.getConnectionStateFromAudioState(invalidAudioState))
                .isEqualTo(BluetoothAdapter.STATE_DISCONNECTED);
    }

    @Test
    public void testGetCurrentAgEvents() {
        Bundle bundle = mHeadsetClientStateMachine.getCurrentAgEvents();

        assertThat(bundle.getString(BluetoothHeadsetClient.EXTRA_SUBSCRIBER_INFO))
                .isEqualTo(mHeadsetClientStateMachine.mSubscriberInfo);
    }

    @Test
    public void testGetCurrentAgFeatures() {
        mHeadsetClientStateMachine.mPeerFeatures = HeadsetClientHalConstants.PEER_FEAT_3WAY;
        mHeadsetClientStateMachine.mChldFeatures = HeadsetClientHalConstants.CHLD_FEAT_HOLD_ACC;
        Set<Integer> features = mHeadsetClientStateMachine.getCurrentAgFeatures();
        assertThat(features.contains(HeadsetClientHalConstants.PEER_FEAT_3WAY)).isTrue();
        assertThat(features.contains(HeadsetClientHalConstants.CHLD_FEAT_HOLD_ACC)).isTrue();

        mHeadsetClientStateMachine.mPeerFeatures = HeadsetClientHalConstants.PEER_FEAT_VREC;
        mHeadsetClientStateMachine.mChldFeatures = HeadsetClientHalConstants.CHLD_FEAT_REL;
        features = mHeadsetClientStateMachine.getCurrentAgFeatures();
        assertThat(features.contains(HeadsetClientHalConstants.PEER_FEAT_VREC)).isTrue();
        assertThat(features.contains(HeadsetClientHalConstants.CHLD_FEAT_REL)).isTrue();

        mHeadsetClientStateMachine.mPeerFeatures = HeadsetClientHalConstants.PEER_FEAT_REJECT;
        mHeadsetClientStateMachine.mChldFeatures = HeadsetClientHalConstants.CHLD_FEAT_REL_ACC;
        features = mHeadsetClientStateMachine.getCurrentAgFeatures();
        assertThat(features.contains(HeadsetClientHalConstants.PEER_FEAT_REJECT)).isTrue();
        assertThat(features.contains(HeadsetClientHalConstants.CHLD_FEAT_REL_ACC)).isTrue();

        mHeadsetClientStateMachine.mPeerFeatures = HeadsetClientHalConstants.PEER_FEAT_ECC;
        mHeadsetClientStateMachine.mChldFeatures = HeadsetClientHalConstants.CHLD_FEAT_MERGE;
        features = mHeadsetClientStateMachine.getCurrentAgFeatures();
        assertThat(features.contains(HeadsetClientHalConstants.PEER_FEAT_ECC)).isTrue();
        assertThat(features.contains(HeadsetClientHalConstants.CHLD_FEAT_MERGE)).isTrue();

        mHeadsetClientStateMachine.mChldFeatures = HeadsetClientHalConstants.CHLD_FEAT_MERGE_DETACH;
        features = mHeadsetClientStateMachine.getCurrentAgFeatures();
        assertThat(features.contains(HeadsetClientHalConstants.CHLD_FEAT_MERGE_DETACH)).isTrue();
    }

    @Test
    public void testGetCurrentAgFeaturesBundle() {
        mHeadsetClientStateMachine.mPeerFeatures = HeadsetClientHalConstants.PEER_FEAT_3WAY;
        mHeadsetClientStateMachine.mChldFeatures = HeadsetClientHalConstants.CHLD_FEAT_HOLD_ACC;
        Bundle bundle = mHeadsetClientStateMachine.getCurrentAgFeaturesBundle();
        assertThat(bundle.getBoolean(BluetoothHeadsetClient.EXTRA_AG_FEATURE_3WAY_CALLING))
                .isTrue();
        assertThat(
                        bundle.getBoolean(
                                BluetoothHeadsetClient
                                        .EXTRA_AG_FEATURE_ACCEPT_HELD_OR_WAITING_CALL))
                .isTrue();

        mHeadsetClientStateMachine.mPeerFeatures = HeadsetClientHalConstants.PEER_FEAT_VREC;
        mHeadsetClientStateMachine.mChldFeatures = HeadsetClientHalConstants.CHLD_FEAT_REL;
        bundle = mHeadsetClientStateMachine.getCurrentAgFeaturesBundle();
        assertThat(bundle.getBoolean(BluetoothHeadsetClient.EXTRA_AG_FEATURE_VOICE_RECOGNITION))
                .isTrue();
        assertThat(
                        bundle.getBoolean(
                                BluetoothHeadsetClient
                                        .EXTRA_AG_FEATURE_RELEASE_HELD_OR_WAITING_CALL))
                .isTrue();

        mHeadsetClientStateMachine.mPeerFeatures = HeadsetClientHalConstants.PEER_FEAT_REJECT;
        mHeadsetClientStateMachine.mChldFeatures = HeadsetClientHalConstants.CHLD_FEAT_REL_ACC;
        bundle = mHeadsetClientStateMachine.getCurrentAgFeaturesBundle();
        assertThat(bundle.getBoolean(BluetoothHeadsetClient.EXTRA_AG_FEATURE_REJECT_CALL)).isTrue();
        assertThat(bundle.getBoolean(BluetoothHeadsetClient.EXTRA_AG_FEATURE_RELEASE_AND_ACCEPT))
                .isTrue();

        mHeadsetClientStateMachine.mPeerFeatures = HeadsetClientHalConstants.PEER_FEAT_ECC;
        mHeadsetClientStateMachine.mChldFeatures = HeadsetClientHalConstants.CHLD_FEAT_MERGE;
        bundle = mHeadsetClientStateMachine.getCurrentAgFeaturesBundle();
        assertThat(bundle.getBoolean(BluetoothHeadsetClient.EXTRA_AG_FEATURE_ECC)).isTrue();
        assertThat(bundle.getBoolean(BluetoothHeadsetClient.EXTRA_AG_FEATURE_MERGE)).isTrue();

        mHeadsetClientStateMachine.mChldFeatures = HeadsetClientHalConstants.CHLD_FEAT_MERGE_DETACH;
        bundle = mHeadsetClientStateMachine.getCurrentAgFeaturesBundle();
        assertThat(bundle.getBoolean(BluetoothHeadsetClient.EXTRA_AG_FEATURE_MERGE_AND_DETACH))
                .isTrue();
    }

    @Test
    public void testGetCurrentCalls() {
        HfpClientCall call =
                new HfpClientCall(
                        mTestDevice, 0, HfpClientCall.CALL_STATE_WAITING, "1", true, false, false);
        mHeadsetClientStateMachine.mCalls.put(0, call);

        List<HfpClientCall> currentCalls = mHeadsetClientStateMachine.getCurrentCalls();

        assertThat(currentCalls.get(0)).isEqualTo(call);
    }

    @Test
    public void testGetMessageName() {
        assertThat(HeadsetClientStateMachine.getMessageName(StackEvent.STACK_EVENT))
                .isEqualTo("STACK_EVENT");
        assertThat(HeadsetClientStateMachine.getMessageName(HeadsetClientStateMachine.CONNECT))
                .isEqualTo("CONNECT");
        assertThat(HeadsetClientStateMachine.getMessageName(HeadsetClientStateMachine.DISCONNECT))
                .isEqualTo("DISCONNECT");
        assertThat(
                        HeadsetClientStateMachine.getMessageName(
                                HeadsetClientStateMachine.CONNECT_AUDIO))
                .isEqualTo("CONNECT_AUDIO");
        assertThat(
                        HeadsetClientStateMachine.getMessageName(
                                HeadsetClientStateMachine.DISCONNECT_AUDIO))
                .isEqualTo("DISCONNECT_AUDIO");
        assertThat(HeadsetClientStateMachine.getMessageName(VOICE_RECOGNITION_START))
                .isEqualTo("VOICE_RECOGNITION_START");
        assertThat(HeadsetClientStateMachine.getMessageName(VOICE_RECOGNITION_STOP))
                .isEqualTo("VOICE_RECOGNITION_STOP");
        assertThat(
                        HeadsetClientStateMachine.getMessageName(
                                HeadsetClientStateMachine.SET_MIC_VOLUME))
                .isEqualTo("SET_MIC_VOLUME");
        assertThat(
                        HeadsetClientStateMachine.getMessageName(
                                HeadsetClientStateMachine.SET_SPEAKER_VOLUME))
                .isEqualTo("SET_SPEAKER_VOLUME");
        assertThat(HeadsetClientStateMachine.getMessageName(HeadsetClientStateMachine.DIAL_NUMBER))
                .isEqualTo("DIAL_NUMBER");
        assertThat(HeadsetClientStateMachine.getMessageName(HeadsetClientStateMachine.ACCEPT_CALL))
                .isEqualTo("ACCEPT_CALL");
        assertThat(HeadsetClientStateMachine.getMessageName(HeadsetClientStateMachine.REJECT_CALL))
                .isEqualTo("REJECT_CALL");
        assertThat(HeadsetClientStateMachine.getMessageName(HeadsetClientStateMachine.HOLD_CALL))
                .isEqualTo("HOLD_CALL");
        assertThat(
                        HeadsetClientStateMachine.getMessageName(
                                HeadsetClientStateMachine.TERMINATE_CALL))
                .isEqualTo("TERMINATE_CALL");
        assertThat(HeadsetClientStateMachine.getMessageName(ENTER_PRIVATE_MODE))
                .isEqualTo("ENTER_PRIVATE_MODE");
        assertThat(HeadsetClientStateMachine.getMessageName(HeadsetClientStateMachine.SEND_DTMF))
                .isEqualTo("SEND_DTMF");
        assertThat(HeadsetClientStateMachine.getMessageName(EXPLICIT_CALL_TRANSFER))
                .isEqualTo("EXPLICIT_CALL_TRANSFER");
        assertThat(HeadsetClientStateMachine.getMessageName(HeadsetClientStateMachine.DISABLE_NREC))
                .isEqualTo("DISABLE_NREC");
        assertThat(
                        HeadsetClientStateMachine.getMessageName(
                                HeadsetClientStateMachine.SEND_VENDOR_AT_COMMAND))
                .isEqualTo("SEND_VENDOR_AT_COMMAND");
        assertThat(HeadsetClientStateMachine.getMessageName(HeadsetClientStateMachine.SEND_BIEV))
                .isEqualTo("SEND_BIEV");
        assertThat(
                        HeadsetClientStateMachine.getMessageName(
                                HeadsetClientStateMachine.QUERY_CURRENT_CALLS))
                .isEqualTo("QUERY_CURRENT_CALLS");
        assertThat(
                        HeadsetClientStateMachine.getMessageName(
                                HeadsetClientStateMachine.QUERY_OPERATOR_NAME))
                .isEqualTo("QUERY_OPERATOR_NAME");
        assertThat(
                        HeadsetClientStateMachine.getMessageName(
                                HeadsetClientStateMachine.SUBSCRIBER_INFO))
                .isEqualTo("SUBSCRIBER_INFO");
        assertThat(
                        HeadsetClientStateMachine.getMessageName(
                                HeadsetClientStateMachine.CONNECTING_TIMEOUT))
                .isEqualTo("CONNECTING_TIMEOUT");
        int unknownMessageInt = 54;
        assertThat(HeadsetClientStateMachine.getMessageName(unknownMessageInt))
                .isEqualTo("UNKNOWN(" + unknownMessageInt + ")");
    }

    /**
     * Tests and verify behavior of the case where remote device doesn't support At Android but
     * tries to send audio policy.
     */
    @Test
    public void testAndroidAtRemoteNotSupported_StateTransition_setAudioPolicy() {
        // Setup connection state machine to be in connected state
        when(mHeadsetClientService.getConnectionPolicy(any(BluetoothDevice.class)))
                .thenReturn(BluetoothProfile.CONNECTION_POLICY_ALLOWED);

        setUpHfpClientConnection();
        setUpServiceLevelConnection();

        BluetoothSinkAudioPolicy dummyAudioPolicy = new BluetoothSinkAudioPolicy.Builder().build();
        mHeadsetClientStateMachine.setAudioPolicy(dummyAudioPolicy);
        verify(mNativeInterface, never())
                .sendAndroidAt(mTestDevice, "+ANDROID=SINKAUDIOPOLICY,0,0,0");
    }

    @SmallTest
    @Test
    public void testSetGetCallAudioPolicy() {
        // Return true for priority.
        when(mHeadsetClientService.getConnectionPolicy(any(BluetoothDevice.class)))
                .thenReturn(BluetoothProfile.CONNECTION_POLICY_ALLOWED);

        setUpHfpClientConnection();
        setUpServiceLevelConnection(true);

        BluetoothSinkAudioPolicy dummyAudioPolicy =
                new BluetoothSinkAudioPolicy.Builder()
                        .setCallEstablishPolicy(BluetoothSinkAudioPolicy.POLICY_ALLOWED)
                        .setActiveDevicePolicyAfterConnection(
                                BluetoothSinkAudioPolicy.POLICY_NOT_ALLOWED)
                        .setInBandRingtonePolicy(BluetoothSinkAudioPolicy.POLICY_ALLOWED)
                        .build();

        // Test if not support audio policy feature
        mHeadsetClientStateMachine.setAudioPolicyRemoteSupported(false);
        mHeadsetClientStateMachine.setAudioPolicy(dummyAudioPolicy);
        verify(mNativeInterface, never())
                .sendAndroidAt(mTestDevice, "+ANDROID=SINKAUDIOPOLICY,1,2,1");
        assertThat(mHeadsetClientStateMachine.mQueuedActions).isEmpty();

        // Test setAudioPolicy
        mHeadsetClientStateMachine.setAudioPolicyRemoteSupported(true);
        mHeadsetClientStateMachine.setAudioPolicy(dummyAudioPolicy);
        verify(mNativeInterface).sendAndroidAt(mTestDevice, "+ANDROID=SINKAUDIOPOLICY,1,2,1");
        assertThat(mHeadsetClientStateMachine.mQueuedActions.size()).isEqualTo(1);
        mHeadsetClientStateMachine.mQueuedActions.clear();

        // Test if fail to sendAndroidAt
        doReturn(false).when(mNativeInterface).sendAndroidAt(anyObject(), anyString());
        mHeadsetClientStateMachine.setAudioPolicy(dummyAudioPolicy);
        assertThat(mHeadsetClientStateMachine.mQueuedActions).isEmpty();
    }

    @Test
    public void testTestDefaultAudioPolicy() {
        mHeadsetClientStateMachine.setForceSetAudioPolicyProperty(true);
        initToConnectedState();

        // Check if set default policy when Connecting -> Connected
        // The testing sys prop is 0. It is ok to check if set audio policy while leaving connecting
        // state.
        verify(mNativeInterface).sendAndroidAt(mTestDevice, "+ANDROID=SINKAUDIOPOLICY,0,0,0");

        // Check if won't set default policy when AudioOn -> Connected
        // Transit to AudioOn
        mHeadsetClientStateMachine.setAudioRouteAllowed(true);
        StackEvent event = new StackEvent(StackEvent.EVENT_TYPE_AUDIO_STATE_CHANGED);
        event.valueInt = HeadsetClientHalConstants.AUDIO_STATE_CONNECTED;
        event.device = mTestDevice;
        mHeadsetClientStateMachine.sendMessage(
                mHeadsetClientStateMachine.obtainMessage(StackEvent.STACK_EVENT, event));
        TestUtils.waitForLooperToFinishScheduledTask(mHandlerThread.getLooper());
        assertThat(mHeadsetClientStateMachine.getCurrentState())
                .isInstanceOf(HeadsetClientStateMachine.AudioOn.class);

        // Back to Connected
        event = new StackEvent(StackEvent.EVENT_TYPE_AUDIO_STATE_CHANGED);
        event.valueInt = HeadsetClientHalConstants.AUDIO_STATE_DISCONNECTED;
        event.device = mTestDevice;
        mHeadsetClientStateMachine.sendMessage(
                mHeadsetClientStateMachine.obtainMessage(StackEvent.STACK_EVENT, event));
        TestUtils.waitForLooperToFinishScheduledTask(mHandlerThread.getLooper());
        assertThat(mHeadsetClientStateMachine.getCurrentState())
                .isInstanceOf(HeadsetClientStateMachine.Connected.class);

        verify(mNativeInterface).sendAndroidAt(mTestDevice, "+ANDROID=SINKAUDIOPOLICY,0,0,0");
    }

    @Test
    public void testDumpDoesNotCrash() {
        mHeadsetClientStateMachine.dump(new StringBuilder());
    }

    @Test
    public void testProcessDisconnectMessage_onDisconnectedState() {
        mHeadsetClientStateMachine.sendMessage(HeadsetClientStateMachine.DISCONNECT);
        TestUtils.waitForLooperToFinishScheduledTask(mHandlerThread.getLooper());
        assertThat(mHeadsetClientStateMachine.getConnectionState(mTestDevice))
                .isEqualTo(STATE_DISCONNECTED);
    }

    @Test
    public void testProcessConnectMessage_onDisconnectedState() {
        doReturn(true).when(mNativeInterface).connect(any(BluetoothDevice.class));
        sendMessageAndVerifyTransition(
                mHeadsetClientStateMachine.obtainMessage(
                        HeadsetClientStateMachine.CONNECT, mTestDevice),
                HeadsetClientStateMachine.Connecting.class);
    }

    @Test
    public void testStackEvent_toConnectingState_onDisconnectedState() {
        allowConnection(true);
        StackEvent event = new StackEvent(StackEvent.EVENT_TYPE_CONNECTION_STATE_CHANGED);
        event.valueInt = HeadsetClientHalConstants.CONNECTION_STATE_CONNECTED;
        event.device = mTestDevice;
        sendMessageAndVerifyTransition(
                mHeadsetClientStateMachine.obtainMessage(StackEvent.STACK_EVENT, event),
                HeadsetClientStateMachine.Connecting.class);
    }

    @Test
    public void testStackEvent_toConnectingState_disallowConnection_onDisconnectedState() {
        allowConnection(false);
        StackEvent event = new StackEvent(StackEvent.EVENT_TYPE_CONNECTION_STATE_CHANGED);
        event.valueInt = HeadsetClientHalConstants.CONNECTION_STATE_CONNECTED;
        event.device = mTestDevice;
        sendMessageAndVerifyTransition(
                mHeadsetClientStateMachine.obtainMessage(StackEvent.STACK_EVENT, event),
                HeadsetClientStateMachine.Disconnected.class);
    }

    @Test
    public void testProcessConnectMessage_onConnectingState() {
        initToConnectingState();
        assertThat(
                        mHeadsetClientStateMachine.doesSuperHaveDeferredMessages(
                                HeadsetClientStateMachine.CONNECT))
                .isFalse();
        mHeadsetClientStateMachine.sendMessage(HeadsetClientStateMachine.CONNECT);
        TestUtils.waitForLooperToFinishScheduledTask(mHandlerThread.getLooper());
        assertThat(
                        mHeadsetClientStateMachine.doesSuperHaveDeferredMessages(
                                HeadsetClientStateMachine.CONNECT))
                .isTrue();
    }

    @Test
    public void testProcessStackEvent_ConnectionStateChanged_Disconnected_onConnectingState() {
        initToConnectingState();
        StackEvent event = new StackEvent(StackEvent.EVENT_TYPE_CONNECTION_STATE_CHANGED);
        event.valueInt = HeadsetClientHalConstants.CONNECTION_STATE_DISCONNECTED;
        event.device = mTestDevice;
        sendMessageAndVerifyTransition(
                mHeadsetClientStateMachine.obtainMessage(StackEvent.STACK_EVENT, event),
                HeadsetClientStateMachine.Disconnected.class);
        verify(mHeadsetService).updateInbandRinging(eq(mTestDevice), eq(false));
    }

    @Test
    public void testProcessStackEvent_ConnectionStateChanged_Connected_onConnectingState() {
        initToConnectingState();
        StackEvent event = new StackEvent(StackEvent.EVENT_TYPE_CONNECTION_STATE_CHANGED);
        event.valueInt = HeadsetClientHalConstants.CONNECTION_STATE_CONNECTED;
        event.device = mTestDevice;
        mHeadsetClientStateMachine.sendMessage(
                mHeadsetClientStateMachine.obtainMessage(StackEvent.STACK_EVENT, event));
        assertThat(mHeadsetClientStateMachine.getCurrentState())
                .isInstanceOf(HeadsetClientStateMachine.Connecting.class);
    }

    @Test
    public void testProcessStackEvent_ConnectionStateChanged_Connecting_onConnectingState() {
        initToConnectingState();
        StackEvent event = new StackEvent(StackEvent.EVENT_TYPE_CONNECTION_STATE_CHANGED);
        event.valueInt = HeadsetClientHalConstants.CONNECTION_STATE_CONNECTING;
        event.device = mTestDevice;
        mHeadsetClientStateMachine.sendMessage(
                mHeadsetClientStateMachine.obtainMessage(StackEvent.STACK_EVENT, event));
        assertThat(mHeadsetClientStateMachine.getCurrentState())
                .isInstanceOf(HeadsetClientStateMachine.Connecting.class);
    }

    @Test
    public void testProcessStackEvent_Call_onConnectingState() {
        initToConnectingState();
        StackEvent event = new StackEvent(StackEvent.EVENT_TYPE_CALL);
        event.valueInt = StackEvent.EVENT_TYPE_CALL;
        event.device = mTestDevice;
        assertThat(mHeadsetClientStateMachine.doesSuperHaveDeferredMessages(StackEvent.STACK_EVENT))
                .isFalse();
        mHeadsetClientStateMachine.sendMessage(
                mHeadsetClientStateMachine.obtainMessage(StackEvent.STACK_EVENT, event));
        TestUtils.waitForLooperToFinishScheduledTask(mHandlerThread.getLooper());
        assertThat(mHeadsetClientStateMachine.doesSuperHaveDeferredMessages(StackEvent.STACK_EVENT))
                .isTrue();
    }

    @Test
    public void testProcessStackEvent_CmdResultWithEmptyQueuedActions_onConnectingState() {
        initToConnectingState();
        StackEvent event = new StackEvent(StackEvent.EVENT_TYPE_CMD_RESULT);
        event.valueInt = StackEvent.CMD_RESULT_TYPE_OK;
        event.device = mTestDevice;
        mHeadsetClientStateMachine.sendMessage(
                mHeadsetClientStateMachine.obtainMessage(StackEvent.STACK_EVENT, event));
        assertThat(mHeadsetClientStateMachine.getCurrentState())
                .isInstanceOf(HeadsetClientStateMachine.Connecting.class);
    }

    @Test
    public void testProcessStackEvent_Unknown_onConnectingState() {
        String atCommand = "+ANDROID: (SINKAUDIOPOLICY)";

        initToConnectingState();
        StackEvent event = new StackEvent(StackEvent.EVENT_TYPE_UNKNOWN_EVENT);
        event.valueString = atCommand;
        event.device = mTestDevice;
        mHeadsetClientStateMachine.sendMessage(
                mHeadsetClientStateMachine.obtainMessage(StackEvent.STACK_EVENT, event));
        TestUtils.waitForLooperToFinishScheduledTask(mHandlerThread.getLooper());
        assertThat(mHeadsetClientStateMachine.getCurrentState())
                .isInstanceOf(HeadsetClientStateMachine.Connected.class);
        verify(mHeadsetService).updateInbandRinging(eq(mTestDevice), eq(true));
    }

    @Test
    public void testProcessConnectTimeoutMessage_onConnectingState() {
        initToConnectingState();
        Message msg =
                mHeadsetClientStateMachine.obtainMessage(
                        HeadsetClientStateMachine.CONNECTING_TIMEOUT);
        sendMessageAndVerifyTransition(msg, HeadsetClientStateMachine.Disconnected.class);
        verify(mHeadsetService).updateInbandRinging(eq(mTestDevice), eq(false));
    }

    @Test
    public void testProcessStackEvent_inBandRingTone_onConnectingState() {
        initToConnectingState();
        StackEvent event = new StackEvent(StackEvent.EVENT_TYPE_IN_BAND_RINGTONE);
        event.valueInt = StackEvent.EVENT_TYPE_IN_BAND_RINGTONE;
        event.device = mTestDevice;
        assertThat(mHeadsetClientStateMachine.doesSuperHaveDeferredMessages(StackEvent.STACK_EVENT))
                .isFalse();
        mHeadsetClientStateMachine.sendMessage(
                mHeadsetClientStateMachine.obtainMessage(StackEvent.STACK_EVENT, event));
        TestUtils.waitForLooperToFinishScheduledTask(mHandlerThread.getLooper());
        assertThat(mHeadsetClientStateMachine.doesSuperHaveDeferredMessages(StackEvent.STACK_EVENT))
                .isTrue();
    }

    @Test
    public void testProcessConnectMessage_onConnectedState() {
        initToConnectedState();
        mHeadsetClientStateMachine.sendMessage(HeadsetClientStateMachine.CONNECT);
        TestUtils.waitForLooperToFinishScheduledTask(mHandlerThread.getLooper());
        assertThat(mHeadsetClientStateMachine.getCurrentState())
                .isInstanceOf(HeadsetClientStateMachine.Connected.class);
    }

    @Test
    public void testProcessDisconnectMessage_onConnectedState() {
        initToConnectedState();
        mHeadsetClientStateMachine.sendMessage(HeadsetClientStateMachine.DISCONNECT, mTestDevice);
        TestUtils.waitForLooperToFinishScheduledTask(mHandlerThread.getLooper());
        verify(mNativeInterface).disconnect(any(BluetoothDevice.class));
    }

    @Test
    public void testProcessConnectAudioMessage_onConnectedState() {
        initToConnectedState();
        mHeadsetClientStateMachine.sendMessage(HeadsetClientStateMachine.CONNECT_AUDIO);
        TestUtils.waitForLooperToFinishScheduledTask(mHandlerThread.getLooper());
        verify(mNativeInterface).connectAudio(any(BluetoothDevice.class));
    }

    @Test
    public void testProcessDisconnectAudioMessage_onConnectedState() {
        initToConnectedState();
        mHeadsetClientStateMachine.sendMessage(HeadsetClientStateMachine.DISCONNECT_AUDIO);
        TestUtils.waitForLooperToFinishScheduledTask(mHandlerThread.getLooper());
        verify(mNativeInterface).disconnectAudio(any(BluetoothDevice.class));
    }

    @Test
    public void testProcessVoiceRecognitionStartMessage_onConnectedState() {
        initToConnectedState();
        mHeadsetClientStateMachine.sendMessage(HeadsetClientStateMachine.VOICE_RECOGNITION_START);
        TestUtils.waitForLooperToFinishScheduledTask(mHandlerThread.getLooper());
        verify(mNativeInterface).startVoiceRecognition(any(BluetoothDevice.class));
    }

    @Test
    public void testProcessDisconnectMessage_onAudioOnState() {
        initToAudioOnState();
        assertThat(
                        mHeadsetClientStateMachine.doesSuperHaveDeferredMessages(
                                HeadsetClientStateMachine.DISCONNECT))
                .isFalse();
        mHeadsetClientStateMachine.sendMessage(HeadsetClientStateMachine.DISCONNECT, mTestDevice);
        TestUtils.waitForLooperToFinishScheduledTask(mHandlerThread.getLooper());
        assertThat(
                        mHeadsetClientStateMachine.doesSuperHaveDeferredMessages(
                                HeadsetClientStateMachine.DISCONNECT))
                .isTrue();
    }

    @Test
    public void testProcessDisconnectAudioMessage_onAudioOnState() {
        initToAudioOnState();
        mHeadsetClientStateMachine.sendMessage(
                HeadsetClientStateMachine.DISCONNECT_AUDIO, mTestDevice);
        TestUtils.waitForLooperToFinishScheduledTask(mHandlerThread.getLooper());
        verify(mNativeInterface).disconnectAudio(any(BluetoothDevice.class));
    }

    @Test
    public void testProcessHoldCall_onAudioOnState() {
        initToAudioOnState();
        HfpClientCall call =
                new HfpClientCall(
                        mTestDevice, 0, HfpClientCall.CALL_STATE_ACTIVE, "1", true, false, false);
        mHeadsetClientStateMachine.mCalls.put(0, call);
        int[] states = new int[1];
        states[0] = HfpClientCall.CALL_STATE_ACTIVE;
        mHeadsetClientStateMachine.sendMessage(HeadsetClientStateMachine.HOLD_CALL, mTestDevice);
        TestUtils.waitForLooperToFinishScheduledTask(mHandlerThread.getLooper());
        verify(mNativeInterface).handleCallAction(any(BluetoothDevice.class), anyInt(), eq(0));
    }

    @Test
    public void testProcessStackEvent_ConnectionStateChanged_onAudioOnState() {
        initToAudioOnState();
        assertThat(mHeadsetClientStateMachine.getCurrentState())
                .isInstanceOf(HeadsetClientStateMachine.AudioOn.class);
        StackEvent event = new StackEvent(StackEvent.EVENT_TYPE_CONNECTION_STATE_CHANGED);
        event.valueInt = HeadsetClientHalConstants.CONNECTION_STATE_DISCONNECTED;
        event.device = mTestDevice;
        mHeadsetClientStateMachine.sendMessage(
                mHeadsetClientStateMachine.obtainMessage(StackEvent.STACK_EVENT, event));
        TestUtils.waitForLooperToFinishScheduledTask(mHandlerThread.getLooper());
        assertThat(mHeadsetClientStateMachine.getCurrentState())
                .isInstanceOf(HeadsetClientStateMachine.Disconnected.class);
        verify(mHeadsetService).updateInbandRinging(eq(mTestDevice), eq(false));
    }

    @Test
    public void testProcessStackEvent_AudioStateChanged_onAudioOnState() {
        initToAudioOnState();
        assertThat(mHeadsetClientStateMachine.getCurrentState())
                .isInstanceOf(HeadsetClientStateMachine.AudioOn.class);
        StackEvent event = new StackEvent(StackEvent.EVENT_TYPE_AUDIO_STATE_CHANGED);
        event.valueInt = HeadsetClientHalConstants.AUDIO_STATE_DISCONNECTED;
        event.device = mTestDevice;
        mHeadsetClientStateMachine.sendMessage(
                mHeadsetClientStateMachine.obtainMessage(StackEvent.STACK_EVENT, event));
        TestUtils.waitForLooperToFinishScheduledTask(mHandlerThread.getLooper());
        assertThat(mHeadsetClientStateMachine.getCurrentState())
                .isInstanceOf(HeadsetClientStateMachine.Connected.class);
    }

    @Test
    public void testProcessStackEvent_CodecSelection_onConnectedState() {
        initToConnectedState();
        assertThat(mHeadsetClientStateMachine.getCurrentState())
                .isInstanceOf(HeadsetClientStateMachine.Connected.class);

        // Trigger a MSBC codec stack event. Expect to mAudioWbs = true.
        mHeadsetClientStateMachine.mAudioWbs = false;
        mHeadsetClientStateMachine.mAudioSWB = false;
        StackEvent event = new StackEvent(StackEvent.EVENT_TYPE_AUDIO_STATE_CHANGED);
        event.valueInt = HeadsetClientHalConstants.AUDIO_STATE_CONNECTED_MSBC;
        event.device = mTestDevice;
        mHeadsetClientStateMachine.sendMessage(
                mHeadsetClientStateMachine.obtainMessage(StackEvent.STACK_EVENT, event));
        TestUtils.waitForLooperToFinishScheduledTask(mHandlerThread.getLooper());
        assertThat(mHeadsetClientStateMachine.mAudioWbs).isTrue();
        assertThat(mHeadsetClientStateMachine.mAudioSWB).isFalse();

        // Trigger a LC3 codec stack event. Expect to mAudioSWB = true.
        mHeadsetClientStateMachine.mAudioWbs = false;
        mHeadsetClientStateMachine.mAudioSWB = false;
        event = new StackEvent(StackEvent.EVENT_TYPE_AUDIO_STATE_CHANGED);
        event.valueInt = HeadsetClientHalConstants.AUDIO_STATE_CONNECTED_LC3;
        event.device = mTestDevice;
        mHeadsetClientStateMachine.sendMessage(
                mHeadsetClientStateMachine.obtainMessage(StackEvent.STACK_EVENT, event));
        TestUtils.waitForLooperToFinishScheduledTask(mHandlerThread.getLooper());
        assertThat(mHeadsetClientStateMachine.mAudioWbs).isFalse();
        assertThat(mHeadsetClientStateMachine.mAudioSWB).isTrue();
    }

    /**
     * Allow/disallow connection to any device
     *
     * @param allow if true, connection is allowed
     */
    private void allowConnection(boolean allow) {
        mHeadsetClientStateMachine.allowConnect = allow;
    }

    private void initToConnectingState() {
        doReturn(true).when(mNativeInterface).connect(any(BluetoothDevice.class));
        sendMessageAndVerifyTransition(
                mHeadsetClientStateMachine.obtainMessage(
                        HeadsetClientStateMachine.CONNECT, mTestDevice),
                HeadsetClientStateMachine.Connecting.class);
    }

    private void initToConnectedState() {
        String atCommand = "+ANDROID: (SINKAUDIOPOLICY)";
        initToConnectingState();
        StackEvent event = new StackEvent(StackEvent.EVENT_TYPE_UNKNOWN_EVENT);
        event.valueString = atCommand;
        event.device = mTestDevice;
        mHeadsetClientStateMachine.sendMessage(
                mHeadsetClientStateMachine.obtainMessage(StackEvent.STACK_EVENT, event));
        TestUtils.waitForLooperToFinishScheduledTask(mHandlerThread.getLooper());
        assertThat(mHeadsetClientStateMachine.getCurrentState())
                .isInstanceOf(HeadsetClientStateMachine.Connected.class);
        verify(mHeadsetService).updateInbandRinging(eq(mTestDevice), eq(true));
    }

    private void initToAudioOnState() {
        mHeadsetClientStateMachine.setAudioRouteAllowed(true);
        initToConnectedState();
        StackEvent event = new StackEvent(StackEvent.EVENT_TYPE_AUDIO_STATE_CHANGED);
        event.valueInt = HeadsetClientHalConstants.AUDIO_STATE_CONNECTED;
        event.device = mTestDevice;
        mHeadsetClientStateMachine.sendMessage(
                mHeadsetClientStateMachine.obtainMessage(StackEvent.STACK_EVENT, event));
        TestUtils.waitForLooperToFinishScheduledTask(mHandlerThread.getLooper());
        assertThat(mHeadsetClientStateMachine.getCurrentState())
                .isInstanceOf(HeadsetClientStateMachine.AudioOn.class);
    }

    private void verifySendBroadcastMultiplePermissions(Matcher<Intent>... matchers) {
        verify(mHeadsetClientService, timeout(STANDARD_WAIT_MILLIS))
                .sendBroadcastMultiplePermissions(
                        MockitoHamcrest.argThat(AllOf.allOf(matchers)),
                        any(String[].class),
                        any(BroadcastOptions.class));
    }

    private void verifySendBroadcast(Matcher<Intent>... matchers) {
        verify(mHeadsetClientService, timeout(STANDARD_WAIT_MILLIS))
                .sendBroadcast(
                        MockitoHamcrest.argThat(AllOf.allOf(matchers)),
                        anyString(),
                        any(Bundle.class));
    }

    private <T> void sendMessageAndVerifyTransition(Message msg, Class<T> type) {
        Mockito.clearInvocations(mHeadsetClientService);
        mHeadsetClientStateMachine.sendMessage(msg);
        // Verify that one connection state broadcast is executed
        verify(mHeadsetClientService, timeout(TIMEOUT_MS))
                .sendBroadcastMultiplePermissions(
                        any(Intent.class), any(String[].class), any(BroadcastOptions.class));
        assertThat(mHeadsetClientStateMachine.getCurrentState()).isInstanceOf(type);
    }

    public static class TestHeadsetClientStateMachine extends HeadsetClientStateMachine {

        Boolean allowConnect = null;
        boolean mForceSetAudioPolicyProperty = false;

        TestHeadsetClientStateMachine(
                AdapterService adapterService,
                HeadsetClientService context,
                HeadsetService headsetService,
                Looper looper,
                NativeInterface nativeInterface) {
            super(adapterService, context, headsetService, looper, nativeInterface);
        }

        public boolean doesSuperHaveDeferredMessages(int what) {
            return super.hasDeferredMessages(what);
        }

        @Override
        public boolean okToConnect(BluetoothDevice device) {
            return allowConnect != null ? allowConnect : super.okToConnect(device);
        }

        @Override
        public int getConnectingTimePolicyProperty() {
            return 2;
        }

        @Override
        public int getInBandRingtonePolicyProperty() {
            return 1;
        }

        void setForceSetAudioPolicyProperty(boolean flag) {
            mForceSetAudioPolicyProperty = flag;
        }

        @Override
        boolean getForceSetAudioPolicyProperty() {
            return mForceSetAudioPolicyProperty;
        }
    }
}
