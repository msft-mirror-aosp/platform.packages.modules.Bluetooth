/*
 * Copyright 2023 The Android Open Source Project
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

import static com.android.bluetooth.TestUtils.MockitoRule;
import static com.android.bluetooth.TestUtils.getTestDevice;
import static com.android.bluetooth.TestUtils.mockGetSystemService;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.app.BroadcastOptions;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.UserManager;
import android.util.Log;

import androidx.test.filters.MediumTest;
import androidx.test.runner.AndroidJUnit4;

import com.android.bluetooth.TestUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class PbapClientStateMachineOldTest {
    private static final String TAG = PbapClientStateMachineOldTest.class.getSimpleName();

    @Rule public final MockitoRule mMockitoRule = new MockitoRule();

    @Mock private PbapClientService mMockPbapClientService;
    @Mock private PbapClientConnectionHandler mMockHandler;

    private static final int DISCONNECT_TIMEOUT = 5000;

    private final BluetoothDevice mDevice = getTestDevice(40);
    private final ArgumentCaptor<Intent> mIntentArgument = ArgumentCaptor.forClass(Intent.class);

    private PbapClientStateMachineOld mPbapClientStateMachine = null;

    @Before
    public void setUp() throws Exception {
        mockGetSystemService(mMockPbapClientService, Context.USER_SERVICE, UserManager.class);

        mPbapClientStateMachine =
                new PbapClientStateMachineOld(mMockPbapClientService, mDevice, mMockHandler);
        mPbapClientStateMachine.start();
    }

    @After
    public void tearDown() throws Exception {
        if (mPbapClientStateMachine != null) {
            mPbapClientStateMachine.doQuit();
        }
    }

    /** Test that default state is STATE_CONNECTING */
    @Test
    public void testDefaultConnectingState() {
        Log.i(TAG, "in testDefaultConnectingState");
        // it appears that enter and exit can overlap sometimes when calling doQuit()
        // currently solved by waiting for looper to finish task
        TestUtils.waitForLooperToFinishScheduledTask(
                mPbapClientStateMachine.getHandler().getLooper());
        assertThat(mPbapClientStateMachine.getConnectionState())
                .isEqualTo(BluetoothProfile.STATE_CONNECTING);
    }

    /**
     * Test transition from STATE_CONNECTING to STATE_DISCONNECTING and then to STATE_DISCONNECTED
     * after timeout.
     */
    @Test
    public void testStateTransitionFromConnectingToDisconnected() {
        assertThat(mPbapClientStateMachine.getConnectionState())
                .isEqualTo(BluetoothProfile.STATE_CONNECTING);

        mPbapClientStateMachine.disconnect(mDevice);

        TestUtils.waitForLooperToFinishScheduledTask(
                mPbapClientStateMachine.getHandler().getLooper());
        assertThat(mPbapClientStateMachine.getConnectionState())
                .isEqualTo(BluetoothProfile.STATE_DISCONNECTING);

        // wait until timeout occurs
        Mockito.clearInvocations(mMockPbapClientService);
        verify(mMockPbapClientService, timeout(DISCONNECT_TIMEOUT))
                .sendBroadcastMultiplePermissions(
                        mIntentArgument.capture(),
                        any(String[].class),
                        any(BroadcastOptions.class));
        assertThat(mPbapClientStateMachine.getConnectionState())
                .isEqualTo(BluetoothProfile.STATE_DISCONNECTED);
    }
}
