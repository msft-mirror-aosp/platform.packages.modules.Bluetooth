/*
 * Copyright 2017 The Android Open Source Project
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

package com.android.bluetooth.pbap;

import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTING;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;

import static com.android.bluetooth.TestUtils.MockitoRule;
import static com.android.bluetooth.TestUtils.getTestDevice;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.*;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.test.filters.MediumTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class PbapStateMachineTest {
    @Rule public final MockitoRule mMockitoRule = new MockitoRule();

    @Mock private BluetoothPbapService mBluetoothPbapService;

    private static final int TEST_NOTIFICATION_ID = 1000000;

    private final BluetoothDevice mDevice = getTestDevice(36);

    private HandlerThread mHandlerThread;
    private PbapStateMachine mPbapStateMachine;
    private Handler mHandler;
    private BluetoothSocket mSocket;

    @Before
    public void setUp() {
        mHandlerThread = new HandlerThread("PbapTestHandlerThread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());

        mPbapStateMachine =
                PbapStateMachine.make(
                        mBluetoothPbapService,
                        mHandlerThread.getLooper(),
                        mDevice,
                        mSocket,
                        mHandler,
                        TEST_NOTIFICATION_ID);
    }

    @After
    public void tearDown() throws InterruptedException {
        mHandlerThread.quitSafely();
        mHandlerThread.join();
    }

    /** Test that initial state is WaitingForAuth */
    @Test
    public void testInitialState() {
        assertThat(mPbapStateMachine.getConnectionState()).isEqualTo(STATE_CONNECTING);
        assertThat(mPbapStateMachine.getCurrentState())
                .isInstanceOf(PbapStateMachine.WaitingForAuth.class);
    }

    /** Test state transition from WaitingForAuth to Finished when the user rejected */
    @Ignore("Class BluetoothSocket is final and cannot be mocked. b/71512958: re-enable it.")
    @Test
    public void testStateTransition_WaitingForAuthToFinished() {
        mPbapStateMachine.sendMessage(PbapStateMachine.REJECTED);
        assertThat(mPbapStateMachine.getConnectionState()).isEqualTo(STATE_DISCONNECTED);
        assertThat(mPbapStateMachine.getCurrentState())
                .isInstanceOf(PbapStateMachine.Finished.class);
    }

    /** Test state transition from WaitingForAuth to Finished when the user rejected */
    @Ignore("Class BluetoothSocket is final and cannot be mocked. b/71512958: re-enable it.")
    @Test
    public void testStateTransition_WaitingForAuthToConnected() {
        mPbapStateMachine.sendMessage(PbapStateMachine.AUTHORIZED);
        assertThat(mPbapStateMachine.getConnectionState()).isEqualTo(STATE_CONNECTED);
        assertThat(mPbapStateMachine.getCurrentState())
                .isInstanceOf(PbapStateMachine.Connected.class);
    }

    /** Test state transition from Connected to Finished when the OBEX server is done */
    @Ignore("Class BluetoothSocket is final and cannot be mocked. b/71512958: re-enable it.")
    @Test
    public void testStateTransition_ConnectedToFinished() {
        mPbapStateMachine.sendMessage(PbapStateMachine.AUTHORIZED);
        assertThat(mPbapStateMachine.getConnectionState()).isEqualTo(STATE_CONNECTED);
        assertThat(mPbapStateMachine.getCurrentState())
                .isInstanceOf(PbapStateMachine.Connected.class);

        // PBAP OBEX transport is done.
        mPbapStateMachine.sendMessage(PbapStateMachine.DISCONNECT);
        assertThat(mPbapStateMachine.getConnectionState()).isEqualTo(STATE_DISCONNECTED);
        assertThat(mPbapStateMachine.getCurrentState())
                .isInstanceOf(PbapStateMachine.Finished.class);
    }
}
