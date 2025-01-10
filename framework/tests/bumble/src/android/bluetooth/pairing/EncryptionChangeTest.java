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

package android.bluetooth.pairing;

import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothHidHost;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.PandoraDevice;
import android.bluetooth.StreamObserverSpliterator;
import android.bluetooth.test_utils.EnableBluetoothRule;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.ParcelUuid;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.bluetooth.flags.Flags;
import com.android.compatibility.common.util.AdoptShellPermissionsRule;

import io.grpc.stub.StreamObserver;

import org.hamcrest.Matcher;
import org.hamcrest.core.AllOf;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.hamcrest.MockitoHamcrest;

import pandora.GattProto;
import pandora.HostProto.AdvertiseRequest;
import pandora.HostProto.OwnAddressType;
import pandora.SecurityProto.PairingEvent;
import pandora.SecurityProto.PairingEventAnswer;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class EncryptionChangeTest {
    private static final String TAG = "EncryptionChangeTest";
    private static final Duration BOND_INTENT_TIMEOUT = Duration.ofSeconds(10);

    private static final ParcelUuid BATTERY_UUID =
            ParcelUuid.fromString("0000180F-0000-1000-8000-00805F9B34FB");

    private static final ParcelUuid HOGP_UUID =
            ParcelUuid.fromString("00001812-0000-1000-8000-00805F9B34FB");

    private static final Context sTargetContext =
            InstrumentationRegistry.getInstrumentation().getTargetContext();
    private static final BluetoothAdapter sAdapter =
            sTargetContext.getSystemService(BluetoothManager.class).getAdapter();

    @Rule(order = 0)
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule(order = 1)
    public final AdoptShellPermissionsRule mPermissionRule = new AdoptShellPermissionsRule();

    @Rule(order = 2)
    public final PandoraDevice mBumble = new PandoraDevice();

    @Rule(order = 3)
    public final EnableBluetoothRule mEnableBluetoothRule =
            new EnableBluetoothRule(false /* enableTestMode */, true /* toggleBluetooth */);

    private final Map<String, Integer> mActionRegistrationCounts = new HashMap<>();
    private final StreamObserverSpliterator<PairingEvent> mPairingEventStreamObserver =
            new StreamObserverSpliterator<>();
    @Mock private BroadcastReceiver mReceiver;
    @Mock private BluetoothProfile.ServiceListener mProfileServiceListener;
    private InOrder mInOrder = null;
    private BluetoothDevice mBumbleDevice;
    private BluetoothHidHost mHidService;
    private BluetoothHeadset mHfpService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        doAnswer(
                        inv -> {
                            Log.d(
                                    TAG,
                                    "onReceive(): intent=" + Arrays.toString(inv.getArguments()));
                            Intent intent = inv.getArgument(1);
                            String action = intent.getAction();
                            if (BluetoothDevice.ACTION_UUID.equals(action)) {
                                ParcelUuid[] uuids =
                                        intent.getParcelableArrayExtra(
                                                BluetoothDevice.EXTRA_UUID, ParcelUuid.class);
                                Log.d(TAG, "onReceive(): UUID=" + Arrays.toString(uuids));
                            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                                int bondState =
                                        intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                                Log.d(TAG, "onReceive(): bondState=" + bondState);
                            }
                            return null;
                        })
                .when(mReceiver)
                .onReceive(any(), any());

        mInOrder = inOrder(mReceiver);

        // Get profile proxies
        mHidService = (BluetoothHidHost) getProfileProxy(BluetoothProfile.HID_HOST);
        mHfpService = (BluetoothHeadset) getProfileProxy(BluetoothProfile.HEADSET);

        mBumbleDevice = mBumble.getRemoteDevice();
        Set<BluetoothDevice> bondedDevices = sAdapter.getBondedDevices();
        if (bondedDevices.contains(mBumbleDevice)) {
            removeBond(mBumbleDevice);
        }
    }

    @After
    public void tearDown() throws Exception {
        Set<BluetoothDevice> bondedDevices = sAdapter.getBondedDevices();
        if (bondedDevices.contains(mBumbleDevice)) {
            removeBond(mBumbleDevice);
        }
        mBumbleDevice = null;
        if (getTotalActionRegistrationCounts() > 0) {
            sTargetContext.unregisterReceiver(mReceiver);
            mActionRegistrationCounts.clear();
        }
    }

    /**
     * Test Encryption change event on LE Secure link:
     *
     * <ol>
     *   <li>1. Android initiate create bond over LE link
     *   <li>2. Android confirms the pairing via pairing request intent
     *   <li>3. Bumble confirms the pairing internally (optional, added only for test confirmation)
     *   <li>4. Android verifies Encryption change Intent and bonded intent
     * </ol>
     */
    @Test
    @RequiresFlagsEnabled({Flags.FLAG_ENCRYPTION_CHANGE_BROADCAST})
    public void encryptionChangeSecureLeLink() {
        registerIntentActions(
                BluetoothDevice.ACTION_BOND_STATE_CHANGED,
                BluetoothDevice.ACTION_ENCRYPTION_CHANGE,
                BluetoothDevice.ACTION_PAIRING_REQUEST);
        mBumble.gattBlocking()
                .registerService(
                        GattProto.RegisterServiceRequest.newBuilder()
                                .setService(
                                        GattProto.GattServiceParams.newBuilder()
                                                .setUuid(HOGP_UUID.toString())
                                                .build())
                                .build());

        mBumble.hostBlocking()
                .advertise(
                        AdvertiseRequest.newBuilder()
                                .setLegacy(true)
                                .setConnectable(true)
                                .setOwnAddressType(OwnAddressType.PUBLIC)
                                .build());

        StreamObserver<PairingEventAnswer> pairingEventAnswerObserver =
                mBumble.security()
                        .withDeadlineAfter(BOND_INTENT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
                        .onPairing(mPairingEventStreamObserver);

        assertThat(mBumbleDevice.createBond(BluetoothDevice.TRANSPORT_LE)).isTrue();
        verifyIntentReceived(
                hasAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
                hasExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_BONDING));

        verifyIntentReceived(
                hasAction(BluetoothDevice.ACTION_PAIRING_REQUEST),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
                hasExtra(
                        BluetoothDevice.EXTRA_PAIRING_VARIANT,
                        BluetoothDevice.PAIRING_VARIANT_CONSENT));
        mBumbleDevice.setPairingConfirmation(true);

        PairingEvent pairingEvent = mPairingEventStreamObserver.iterator().next();
        assertThat(pairingEvent.hasJustWorks()).isTrue();
        pairingEventAnswerObserver.onNext(
                PairingEventAnswer.newBuilder().setEvent(pairingEvent).setConfirm(true).build());

        verifyIntentReceived(
                hasAction(BluetoothDevice.ACTION_ENCRYPTION_CHANGE),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
                hasExtra(BluetoothDevice.EXTRA_TRANSPORT, BluetoothDevice.TRANSPORT_LE),
                hasExtra(BluetoothDevice.EXTRA_ENCRYPTION_STATUS, 0),
                hasExtra(BluetoothDevice.EXTRA_ENCRYPTION_ENABLED, true),
                hasExtra(BluetoothDevice.EXTRA_KEY_SIZE, 16),
                hasExtra(
                        BluetoothDevice.EXTRA_ENCRYPTION_ALGORITHM,
                        BluetoothDevice.ENCRYPTION_ALGORITHM_AES));

        verifyIntentReceived(
                hasAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
                hasExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_BONDED));

        verifyNoMoreInteractions(mReceiver);

        unregisterIntentActions(
                BluetoothDevice.ACTION_BOND_STATE_CHANGED,
                BluetoothDevice.ACTION_ENCRYPTION_CHANGE,
                BluetoothDevice.ACTION_PAIRING_REQUEST);
    }

    /**
     * Test Encryption change event on classic link:
     *
     * <ol>
     *   <li>1. Android initiate create bond over Classic link
     *   <li>2. Android confirms the pairing via pairing request intent
     *   <li>3. Bumble confirms the pairing internally (optional, added only for test confirmation)
     *   <li>4. Android verifies Encryption change Intent and bonded intent
     * </ol>
     */
    @Test
    @RequiresFlagsEnabled({Flags.FLAG_ENCRYPTION_CHANGE_BROADCAST})
    public void encryptionChangeSecureClassicLink() {
        registerIntentActions(
                BluetoothDevice.ACTION_BOND_STATE_CHANGED,
                BluetoothDevice.ACTION_ENCRYPTION_CHANGE,
                BluetoothDevice.ACTION_PAIRING_REQUEST);

        StreamObserver<PairingEventAnswer> pairingEventAnswerObserver =
                mBumble.security()
                        .withDeadlineAfter(BOND_INTENT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
                        .onPairing(mPairingEventStreamObserver);

        assertThat(mBumbleDevice.createBond(BluetoothDevice.TRANSPORT_BREDR)).isTrue();
        verifyIntentReceived(
                hasAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
                hasExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_BONDING));

        verifyIntentReceived(
                hasAction(BluetoothDevice.ACTION_PAIRING_REQUEST),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
                hasExtra(
                        BluetoothDevice.EXTRA_PAIRING_VARIANT,
                        BluetoothDevice.PAIRING_VARIANT_CONSENT));
        mBumbleDevice.setPairingConfirmation(true);

        PairingEvent pairingEvent = mPairingEventStreamObserver.iterator().next();
        assertThat(pairingEvent.hasJustWorks()).isTrue();
        pairingEventAnswerObserver.onNext(
                PairingEventAnswer.newBuilder().setEvent(pairingEvent).setConfirm(true).build());

        verifyIntentReceived(
                hasAction(BluetoothDevice.ACTION_ENCRYPTION_CHANGE),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
                hasExtra(BluetoothDevice.EXTRA_TRANSPORT, BluetoothDevice.TRANSPORT_BREDR),
                hasExtra(BluetoothDevice.EXTRA_ENCRYPTION_STATUS, 0),
                hasExtra(BluetoothDevice.EXTRA_ENCRYPTION_ENABLED, true),
                hasExtra(BluetoothDevice.EXTRA_KEY_SIZE, 16),
                hasExtra(
                        BluetoothDevice.EXTRA_ENCRYPTION_ALGORITHM,
                        BluetoothDevice.ENCRYPTION_ALGORITHM_AES));

        verifyIntentReceived(
                hasAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
                hasExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_BONDED));

        verifyNoMoreInteractions(mReceiver);
        unregisterIntentActions(
                BluetoothDevice.ACTION_BOND_STATE_CHANGED,
                BluetoothDevice.ACTION_ENCRYPTION_CHANGE,
                BluetoothDevice.ACTION_PAIRING_REQUEST);
    }

    private void removeBond(BluetoothDevice device) {
        registerIntentActions(BluetoothDevice.ACTION_BOND_STATE_CHANGED);

        assertThat(device.removeBond()).isTrue();
        verifyIntentReceived(
                hasAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleDevice),
                hasExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE));

        unregisterIntentActions(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
    }

    @SafeVarargs
    private void verifyIntentReceived(Matcher<Intent>... matchers) {
        mInOrder.verify(mReceiver, timeout(BOND_INTENT_TIMEOUT.toMillis()))
                .onReceive(any(Context.class), MockitoHamcrest.argThat(AllOf.allOf(matchers)));
    }

    @SafeVarargs
    private void verifyIntentReceivedUnordered(int num, Matcher<Intent>... matchers) {
        verify(mReceiver, timeout(BOND_INTENT_TIMEOUT.toMillis()).times(num))
                .onReceive(any(Context.class), MockitoHamcrest.argThat(AllOf.allOf(matchers)));
    }

    @SafeVarargs
    private void verifyIntentReceivedUnordered(Matcher<Intent>... matchers) {
        verifyIntentReceivedUnordered(1, matchers);
    }

    /**
     * Helper function to add reference count to registered intent actions
     *
     * @param actions new intent actions to add. If the array is empty, it is a no-op.
     */
    private void registerIntentActions(String... actions) {
        if (actions.length == 0) {
            return;
        }
        if (getTotalActionRegistrationCounts() > 0) {
            Log.d(TAG, "registerIntentActions(): unregister ALL intents");
            sTargetContext.unregisterReceiver(mReceiver);
        }
        for (String action : actions) {
            mActionRegistrationCounts.merge(action, 1, Integer::sum);
        }
        IntentFilter filter = new IntentFilter();
        mActionRegistrationCounts.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .forEach(
                        entry -> {
                            Log.d(
                                    TAG,
                                    "registerIntentActions(): Registering action = "
                                            + entry.getKey());
                            filter.addAction(entry.getKey());
                        });
        sTargetContext.registerReceiver(mReceiver, filter, Context.RECEIVER_EXPORTED);
    }

    /**
     * Helper function to reduce reference count to registered intent actions If total reference
     * count is zero after removal, no broadcast receiver will be registered.
     *
     * @param actions intent actions to be removed. If some action is not registered, it is no-op
     *     for that action. If the actions array is empty, it is also a no-op.
     */
    private void unregisterIntentActions(String... actions) {
        if (actions.length == 0) {
            return;
        }
        if (getTotalActionRegistrationCounts() <= 0) {
            return;
        }
        Log.d(TAG, "unregisterIntentActions(): unregister ALL intents");
        sTargetContext.unregisterReceiver(mReceiver);
        for (String action : actions) {
            if (!mActionRegistrationCounts.containsKey(action)) {
                continue;
            }
            mActionRegistrationCounts.put(action, mActionRegistrationCounts.get(action) - 1);
            if (mActionRegistrationCounts.get(action) <= 0) {
                mActionRegistrationCounts.remove(action);
            }
        }
        if (getTotalActionRegistrationCounts() > 0) {
            IntentFilter filter = new IntentFilter();
            mActionRegistrationCounts.entrySet().stream()
                    .filter(entry -> entry.getValue() > 0)
                    .forEach(
                            entry -> {
                                Log.d(
                                        TAG,
                                        "unregisterIntentActions(): Registering action = "
                                                + entry.getKey());
                                filter.addAction(entry.getKey());
                            });
            sTargetContext.registerReceiver(mReceiver, filter, Context.RECEIVER_EXPORTED);
        }
    }

    /**
     * Get sum of reference count from all registered actions
     *
     * @return sum of reference count from all registered actions
     */
    private int getTotalActionRegistrationCounts() {
        return mActionRegistrationCounts.values().stream().reduce(0, Integer::sum);
    }

    private BluetoothProfile getProfileProxy(int profile) {
        sAdapter.getProfileProxy(sTargetContext, mProfileServiceListener, profile);
        ArgumentCaptor<BluetoothProfile> proxyCaptor =
                ArgumentCaptor.forClass(BluetoothProfile.class);
        verify(mProfileServiceListener, timeout(BOND_INTENT_TIMEOUT.toMillis()))
                .onServiceConnected(eq(profile), proxyCaptor.capture());
        return proxyCaptor.getValue();
    }
}
