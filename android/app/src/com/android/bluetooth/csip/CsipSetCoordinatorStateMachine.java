/*
 * Copyright 2019 HIMSA II K/S - www.himsa.com.
 * Represented by EHIMA - www.ehima.com
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

package com.android.bluetooth.csip;

import static android.Manifest.permission.BLUETOOTH_CONNECT;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTING;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTING;
import static android.bluetooth.BluetoothProfile.getConnectionStateName;

import android.bluetooth.BluetoothCsipSetCoordinator;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.android.bluetooth.btservice.ProfileService;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Scanner;

/** CSIP Set Coordinator role device state machine */
public class CsipSetCoordinatorStateMachine extends StateMachine {
    private static final String TAG = CsipSetCoordinatorStateMachine.class.getSimpleName();

    static final int CONNECT = 1;
    static final int DISCONNECT = 2;
    @VisibleForTesting static final int STACK_EVENT = 101;
    @VisibleForTesting static final int CONNECT_TIMEOUT = 201;

    // NOTE: the value is not "final" - it is modified in the unit tests
    @VisibleForTesting static int sConnectTimeoutMs = 30000; // 30s

    private Disconnected mDisconnected;
    private Connecting mConnecting;
    private Disconnecting mDisconnecting;
    private Connected mConnected;
    private int mLastConnectionState = -1;

    private CsipSetCoordinatorService mService;
    private CsipSetCoordinatorNativeInterface mNativeInterface;

    private final BluetoothDevice mDevice;

    CsipSetCoordinatorStateMachine(
            BluetoothDevice device,
            CsipSetCoordinatorService svc,
            CsipSetCoordinatorNativeInterface nativeInterface,
            Looper looper) {
        super(TAG, looper);
        mDevice = device;
        mService = svc;
        mNativeInterface = nativeInterface;

        mDisconnected = new Disconnected();
        mConnecting = new Connecting();
        mDisconnecting = new Disconnecting();
        mConnected = new Connected();

        addState(mDisconnected);
        addState(mConnecting);
        addState(mDisconnecting);
        addState(mConnected);

        setInitialState(mDisconnected);
    }

    static CsipSetCoordinatorStateMachine make(
            BluetoothDevice device,
            CsipSetCoordinatorService svc,
            CsipSetCoordinatorNativeInterface nativeInterface,
            Looper looper) {
        Log.i(TAG, "make for device " + device);
        CsipSetCoordinatorStateMachine CsisSm =
                new CsipSetCoordinatorStateMachine(device, svc, nativeInterface, looper);
        CsisSm.start();
        return CsisSm;
    }

    @VisibleForTesting
    boolean doesSuperHaveDeferredMessages(int what) {
        return super.hasDeferredMessages(what);
    }

    /** Quit state machine execution */
    public void doQuit() {
        log("doQuit for device " + mDevice);
        quitNow();
    }

    /** Clean up */
    public void cleanup() {
        log("cleanup for device " + mDevice);
    }

    @VisibleForTesting
    class Disconnected extends State {
        @Override
        public void enter() {
            Log.i(
                    TAG,
                    "Enter Disconnected("
                            + mDevice
                            + "): "
                            + messageWhatToString(getCurrentMessage().what));

            removeDeferredMessages(DISCONNECT);

            if (mLastConnectionState != -1) {
                csipConnectionState(STATE_DISCONNECTED, mLastConnectionState);
            }
        }

        @Override
        public void exit() {
            log(
                    "Exit Disconnected("
                            + mDevice
                            + "): "
                            + messageWhatToString(getCurrentMessage().what));
            mLastConnectionState = STATE_DISCONNECTED;
        }

        @Override
        public boolean processMessage(Message message) {
            log(
                    "Disconnected process message("
                            + mDevice
                            + "): "
                            + messageWhatToString(message.what));

            switch (message.what) {
                case CONNECT:
                    log("Connecting to " + mDevice);
                    if (!mNativeInterface.connect(mDevice)) {
                        Log.e(TAG, "Disconnected: error connecting to " + mDevice);
                        break;
                    }
                    if (mService.okToConnect(mDevice)) {
                        transitionTo(mConnecting);
                    } else {
                        // Reject the request and stay in Disconnected state
                        Log.w(
                                TAG,
                                "Outgoing CsipSetCoordinator Connecting request rejected: "
                                        + mDevice);
                    }
                    break;
                case DISCONNECT:
                    Log.w(TAG, "Disconnected: DISCONNECT ignored: " + mDevice);
                    break;
                case STACK_EVENT:
                    CsipSetCoordinatorStackEvent event = (CsipSetCoordinatorStackEvent) message.obj;
                    Log.d(TAG, "Disconnected: stack event: " + event);
                    if (!mDevice.equals(event.device)) {
                        Log.wtf(TAG, "Device(" + mDevice + "): event mismatch: " + event);
                    }
                    switch (event.type) {
                        case CsipSetCoordinatorStackEvent.EVENT_TYPE_CONNECTION_STATE_CHANGED:
                            processConnectionEvent(event.valueInt1);
                            break;
                        default:
                            Log.e(TAG, "Disconnected: ignoring stack event: " + event);
                            break;
                    }
                    break;
                default:
                    return NOT_HANDLED;
            }
            return HANDLED;
        }

        // in Disconnected state
        private void processConnectionEvent(int state) {
            switch (state) {
                case CsipSetCoordinatorStackEvent.CONNECTION_STATE_DISCONNECTED:
                    Log.w(TAG, "Ignore CsipSetCoordinator DISCONNECTED event: " + mDevice);
                    break;
                case CsipSetCoordinatorStackEvent.CONNECTION_STATE_CONNECTING:
                    if (mService.okToConnect(mDevice)) {
                        Log.i(
                                TAG,
                                "Incoming CsipSetCoordinator Connecting request accepted: "
                                        + mDevice);
                        transitionTo(mConnecting);
                    } else {
                        // Reject the connection and stay in Disconnected state itself
                        Log.w(
                                TAG,
                                "Incoming CsipSetCoordinator Connecting request rejected: "
                                        + mDevice);
                        mNativeInterface.disconnect(mDevice);
                    }
                    break;
                case CsipSetCoordinatorStackEvent.CONNECTION_STATE_CONNECTED:
                    Log.w(TAG, "CsipSetCoordinator Connected from Disconnected state: " + mDevice);
                    if (mService.okToConnect(mDevice)) {
                        Log.i(
                                TAG,
                                "Incoming CsipSetCoordinator Connected request accepted: "
                                        + mDevice);
                        transitionTo(mConnected);
                    } else {
                        // Reject the connection and stay in Disconnected state itself
                        Log.w(
                                TAG,
                                "Incoming CsipSetCoordinator Connected request rejected: "
                                        + mDevice);
                        mNativeInterface.disconnect(mDevice);
                    }
                    break;
                case CsipSetCoordinatorStackEvent.CONNECTION_STATE_DISCONNECTING:
                    Log.w(TAG, "Ignore CsipSetCoordinator DISCONNECTING event: " + mDevice);
                    break;
                default:
                    Log.e(TAG, "Incorrect state: " + state + " device: " + mDevice);
                    break;
            }
        }
    }

    @VisibleForTesting
    class Connecting extends State {
        @Override
        public void enter() {
            Log.i(
                    TAG,
                    "Enter Connecting("
                            + mDevice
                            + "): "
                            + messageWhatToString(getCurrentMessage().what));
            sendMessageDelayed(CONNECT_TIMEOUT, sConnectTimeoutMs);
            csipConnectionState(STATE_CONNECTING, mLastConnectionState);
        }

        @Override
        public void exit() {
            log(
                    "Exit Connecting("
                            + mDevice
                            + "): "
                            + messageWhatToString(getCurrentMessage().what));
            mLastConnectionState = STATE_CONNECTING;
            removeMessages(CONNECT_TIMEOUT);
        }

        @Override
        public boolean processMessage(Message message) {
            log(
                    "Connecting process message("
                            + mDevice
                            + "): "
                            + messageWhatToString(message.what));

            switch (message.what) {
                case CONNECT:
                    deferMessage(message);
                    break;
                case CONNECT_TIMEOUT:
                    Log.w(TAG, "Connecting connection timeout: " + mDevice);
                    mNativeInterface.disconnect(mDevice);
                    CsipSetCoordinatorStackEvent disconnectEvent =
                            new CsipSetCoordinatorStackEvent(
                                    CsipSetCoordinatorStackEvent
                                            .EVENT_TYPE_CONNECTION_STATE_CHANGED);
                    disconnectEvent.device = mDevice;
                    disconnectEvent.valueInt1 =
                            CsipSetCoordinatorStackEvent.CONNECTION_STATE_DISCONNECTED;
                    sendMessage(STACK_EVENT, disconnectEvent);
                    break;
                case DISCONNECT:
                    log("Connecting: connection canceled to " + mDevice);
                    mNativeInterface.disconnect(mDevice);
                    transitionTo(mDisconnected);
                    break;
                case STACK_EVENT:
                    CsipSetCoordinatorStackEvent event = (CsipSetCoordinatorStackEvent) message.obj;
                    log("Connecting: stack event: " + event);
                    if (!mDevice.equals(event.device)) {
                        Log.wtf(TAG, "Device(" + mDevice + "): event mismatch: " + event);
                    }
                    switch (event.type) {
                        case CsipSetCoordinatorStackEvent.EVENT_TYPE_CONNECTION_STATE_CHANGED:
                            processConnectionEvent(event.valueInt1);
                            break;
                        default:
                            Log.e(TAG, "Connecting: ignoring stack event: " + event);
                            break;
                    }
                    break;
                default:
                    return NOT_HANDLED;
            }
            return HANDLED;
        }

        // in Connecting state
        private void processConnectionEvent(int state) {
            switch (state) {
                case CsipSetCoordinatorStackEvent.CONNECTION_STATE_DISCONNECTED:
                    Log.w(TAG, "Connecting device disconnected: " + mDevice);
                    transitionTo(mDisconnected);
                    break;
                case CsipSetCoordinatorStackEvent.CONNECTION_STATE_CONNECTED:
                    transitionTo(mConnected);
                    break;
                case CsipSetCoordinatorStackEvent.CONNECTION_STATE_CONNECTING:
                    break;
                case CsipSetCoordinatorStackEvent.CONNECTION_STATE_DISCONNECTING:
                    Log.w(TAG, "Connecting interrupted: device is disconnecting: " + mDevice);
                    transitionTo(mDisconnecting);
                    break;
                default:
                    Log.e(TAG, "Incorrect state: " + state);
                    break;
            }
        }
    }

    @VisibleForTesting
    class Disconnecting extends State {
        @Override
        public void enter() {
            Log.i(
                    TAG,
                    "Enter Disconnecting("
                            + mDevice
                            + "): "
                            + messageWhatToString(getCurrentMessage().what));
            sendMessageDelayed(CONNECT_TIMEOUT, sConnectTimeoutMs);
            csipConnectionState(STATE_DISCONNECTING, mLastConnectionState);
        }

        @Override
        public void exit() {
            log(
                    "Exit Disconnecting("
                            + mDevice
                            + "): "
                            + messageWhatToString(getCurrentMessage().what));
            mLastConnectionState = STATE_DISCONNECTING;
            removeMessages(CONNECT_TIMEOUT);
        }

        @Override
        public boolean processMessage(Message message) {
            log(
                    "Disconnecting process message("
                            + mDevice
                            + "): "
                            + messageWhatToString(message.what));

            switch (message.what) {
                case CONNECT:
                    deferMessage(message);
                    break;
                case CONNECT_TIMEOUT:
                    {
                        Log.w(TAG, "Disconnecting connection timeout: " + mDevice);
                        mNativeInterface.disconnect(mDevice);
                        CsipSetCoordinatorStackEvent disconnectEvent =
                                new CsipSetCoordinatorStackEvent(
                                        CsipSetCoordinatorStackEvent
                                                .EVENT_TYPE_CONNECTION_STATE_CHANGED);
                        disconnectEvent.device = mDevice;
                        disconnectEvent.valueInt1 =
                                CsipSetCoordinatorStackEvent.CONNECTION_STATE_DISCONNECTED;
                        sendMessage(STACK_EVENT, disconnectEvent);
                        break;
                    }
                case DISCONNECT:
                    deferMessage(message);
                    break;
                case STACK_EVENT:
                    CsipSetCoordinatorStackEvent event = (CsipSetCoordinatorStackEvent) message.obj;
                    log("Disconnecting: stack event: " + event);
                    if (!mDevice.equals(event.device)) {
                        Log.wtf(TAG, "Device(" + mDevice + "): event mismatch: " + event);
                    }
                    switch (event.type) {
                        case CsipSetCoordinatorStackEvent.EVENT_TYPE_CONNECTION_STATE_CHANGED:
                            processConnectionEvent(event.valueInt1);
                            break;
                        default:
                            Log.e(TAG, "Disconnecting: ignoring stack event: " + event);
                            break;
                    }
                    break;
                default:
                    return NOT_HANDLED;
            }
            return HANDLED;
        }

        // in Disconnecting state
        private void processConnectionEvent(int state) {
            switch (state) {
                case CsipSetCoordinatorStackEvent.CONNECTION_STATE_DISCONNECTED:
                    Log.i(TAG, "Disconnected: " + mDevice);
                    transitionTo(mDisconnected);
                    break;
                case CsipSetCoordinatorStackEvent.CONNECTION_STATE_CONNECTED:
                    if (mService.okToConnect(mDevice)) {
                        Log.w(TAG, "Disconnecting interrupted: device is connected: " + mDevice);
                        transitionTo(mConnected);
                    } else {
                        // Reject the connection and stay in Disconnecting state
                        Log.w(
                                TAG,
                                "Incoming CsipSetCoordinator Connected request rejected: "
                                        + mDevice);
                        mNativeInterface.disconnect(mDevice);
                    }
                    break;
                case CsipSetCoordinatorStackEvent.CONNECTION_STATE_CONNECTING:
                    if (mService.okToConnect(mDevice)) {
                        Log.i(TAG, "Disconnecting interrupted: try to reconnect: " + mDevice);
                        transitionTo(mConnecting);
                    } else {
                        // Reject the connection and stay in Disconnecting state
                        Log.w(
                                TAG,
                                "Incoming CsipSetCoordinator Connecting request rejected: "
                                        + mDevice);
                        mNativeInterface.disconnect(mDevice);
                    }
                    break;
                case CsipSetCoordinatorStackEvent.CONNECTION_STATE_DISCONNECTING:
                    break;
                default:
                    Log.e(TAG, "Incorrect state: " + state);
                    break;
            }
        }
    }

    @VisibleForTesting
    class Connected extends State {
        @Override
        public void enter() {
            Log.i(
                    TAG,
                    "Enter Connected("
                            + mDevice
                            + "): "
                            + messageWhatToString(getCurrentMessage().what));
            removeDeferredMessages(CONNECT);
            csipConnectionState(STATE_CONNECTED, mLastConnectionState);
        }

        @Override
        public void exit() {
            log(
                    "Exit Connected("
                            + mDevice
                            + "): "
                            + messageWhatToString(getCurrentMessage().what));
            mLastConnectionState = STATE_CONNECTED;
        }

        @Override
        public boolean processMessage(Message message) {
            log("Connected process message(" + mDevice + "): " + messageWhatToString(message.what));

            switch (message.what) {
                case CONNECT:
                    Log.w(TAG, "Connected: CONNECT ignored: " + mDevice);
                    break;
                case DISCONNECT:
                    log("Disconnecting from " + mDevice);
                    if (!mNativeInterface.disconnect(mDevice)) {
                        // If error in the native stack, transition directly to Disconnected state.
                        Log.e(TAG, "Connected: error disconnecting from " + mDevice);
                        transitionTo(mDisconnected);
                        break;
                    }
                    transitionTo(mDisconnecting);
                    break;
                case STACK_EVENT:
                    CsipSetCoordinatorStackEvent event = (CsipSetCoordinatorStackEvent) message.obj;
                    log("Connected: stack event: " + event);
                    if (!mDevice.equals(event.device)) {
                        Log.wtf(TAG, "Device(" + mDevice + "): event mismatch: " + event);
                    }
                    switch (event.type) {
                        case CsipSetCoordinatorStackEvent.EVENT_TYPE_CONNECTION_STATE_CHANGED:
                            processConnectionEvent(event.valueInt1);
                            break;
                        default:
                            Log.e(TAG, "Connected: ignoring stack event: " + event);
                            break;
                    }
                    break;
                default:
                    return NOT_HANDLED;
            }
            return HANDLED;
        }

        // in Connected state
        private void processConnectionEvent(int state) {
            switch (state) {
                case CsipSetCoordinatorStackEvent.CONNECTION_STATE_DISCONNECTED:
                    Log.i(TAG, "Disconnected from " + mDevice);
                    transitionTo(mDisconnected);
                    break;
                case CsipSetCoordinatorStackEvent.CONNECTION_STATE_DISCONNECTING:
                    Log.i(TAG, "Disconnecting from " + mDevice);
                    transitionTo(mDisconnecting);
                    break;
                default:
                    Log.e(TAG, "Connection State Device: " + mDevice + " bad state: " + state);
                    break;
            }
        }
    }

    BluetoothDevice getDevice() {
        return mDevice;
    }

    synchronized boolean isConnected() {
        return getCurrentState() == mConnected;
    }

    int getConnectionState() {
        String currentState = getCurrentState().getName();
        switch (currentState) {
            case "Disconnected":
                return STATE_DISCONNECTED;
            case "Connecting":
                return STATE_CONNECTING;
            case "Connected":
                return STATE_CONNECTED;
            case "Disconnecting":
                return STATE_DISCONNECTING;
            default:
                Log.e(TAG, "Bad currentState: " + currentState);
                return STATE_DISCONNECTED;
        }
    }

    // This method does not check for error condition (newState == prevState)
    private void csipConnectionState(int newState, int prevState) {
        log(
                "Connection state "
                        + mDevice
                        + ": "
                        + getConnectionStateName(prevState)
                        + "->"
                        + getConnectionStateName(newState));
        mService.handleConnectionStateChanged(mDevice, prevState, newState);

        Intent intent =
                new Intent(BluetoothCsipSetCoordinator.ACTION_CSIS_CONNECTION_STATE_CHANGED);
        intent.putExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, prevState);
        intent.putExtra(BluetoothProfile.EXTRA_STATE, newState);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, mDevice);
        intent.addFlags(
                Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT
                        | Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND);
        mService.sendBroadcast(intent, BLUETOOTH_CONNECT);
    }

    private static String messageWhatToString(int what) {
        switch (what) {
            case CONNECT:
                return "CONNECT";
            case DISCONNECT:
                return "DISCONNECT";
            case STACK_EVENT:
                return "STACK_EVENT";
            case CONNECT_TIMEOUT:
                return "CONNECT_TIMEOUT";
            default:
                break;
        }
        return Integer.toString(what);
    }

    /** Dump the state machine logs */
    public void dump(StringBuilder sb) {
        ProfileService.println(sb, "mDevice: " + mDevice);
        ProfileService.println(sb, "  StateMachine: " + this);
        // Dump the state machine logs
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        super.dump(new FileDescriptor(), printWriter, new String[] {});
        printWriter.flush();
        stringWriter.flush();
        ProfileService.println(sb, "  StateMachineLog:");
        Scanner scanner = new Scanner(stringWriter.toString());
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            ProfileService.println(sb, "    " + line);
        }
        scanner.close();
    }

    @Override
    protected void log(String msg) {
        super.log(msg);
    }
}
