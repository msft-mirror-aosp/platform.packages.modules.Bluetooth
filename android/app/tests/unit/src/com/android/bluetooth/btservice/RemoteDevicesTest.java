package com.android.bluetooth.btservice;

import static android.Manifest.permission.BLUETOOTH_CONNECT;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.*;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAssignedNumbers;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSinkAudioPolicy;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Message;
import android.os.TestLooperManager;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.filters.MediumTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import com.android.bluetooth.Utils;
import com.android.bluetooth.bas.BatteryService;
import com.android.bluetooth.btservice.RemoteDevices.DeviceProperties;
import com.android.bluetooth.flags.Flags;
import com.android.bluetooth.hfp.HeadsetHalConstants;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class RemoteDevicesTest {
    private static final String TEST_BT_ADDR_1 = "00:11:22:33:44:55";

    private ArgumentCaptor<Intent> mIntentArgument = ArgumentCaptor.forClass(Intent.class);
    private ArgumentCaptor<String> mStringArgument = ArgumentCaptor.forClass(String.class);
    private BluetoothDevice mDevice1;
    private RemoteDevices mRemoteDevices;
    private HandlerThread mHandlerThread;
    private TestLooperManager mTestLooperManager;

    private Context mTargetContext;
    private BluetoothManager mBluetoothManager;
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock private AdapterService mAdapterService;

    @Before
    public void setUp() {
        mTargetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        mDevice1 = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(TEST_BT_ADDR_1);
        mHandlerThread = new HandlerThread("RemoteDevicesTestHandlerThread");
        mHandlerThread.start();
        mTestLooperManager =
                InstrumentationRegistry.getInstrumentation()
                        .acquireLooperManager(mHandlerThread.getLooper());

        mBluetoothManager = mTargetContext.getSystemService(BluetoothManager.class);
        when(mAdapterService.getSystemService(Context.BLUETOOTH_SERVICE))
                .thenReturn(mBluetoothManager);
        when(mAdapterService.getSystemServiceName(BluetoothManager.class))
                .thenReturn(Context.BLUETOOTH_SERVICE);

        mRemoteDevices = new RemoteDevices(mAdapterService, mHandlerThread.getLooper());
        verify(mAdapterService).getSystemService(Context.BLUETOOTH_SERVICE);
        verify(mAdapterService).getSystemService(BluetoothManager.class);
    }

    @After
    public void tearDown() {
        mTestLooperManager.release();
        mHandlerThread.quit();
    }

    @Test
    public void testSendUuidIntent() {
        doNothing().when(mAdapterService).sendUuidsInternal(any(), any());

        // Verify that a handler message is sent by the method call
        mRemoteDevices.updateUuids(mDevice1);
        Message msg = mTestLooperManager.next();
        assertThat(msg).isNotNull();

        // Verify that executing that message results in a direct call and broadcast intent
        mTestLooperManager.execute(msg);
        verify(mAdapterService).sendUuidsInternal(any(), any());
        verify(mAdapterService).sendBroadcast(any(), anyString(), any());
        verifyNoMoreInteractions(mAdapterService);
    }

    @Test
    public void testUpdateBatteryLevel_normalSequence() {
        int batteryLevel = 10;

        // Verify that device property is null initially
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1)).isNull();

        // Verify that updating battery level triggers ACTION_BATTERY_LEVEL_CHANGED intent
        mRemoteDevices.updateBatteryLevel(mDevice1, batteryLevel, /* fromBas= */ false);
        verify(mAdapterService)
                .sendBroadcast(
                        mIntentArgument.capture(), mStringArgument.capture(), any(Bundle.class));
        verifyBatteryLevelChangedIntent(mDevice1, batteryLevel, mIntentArgument);
        assertThat(mStringArgument.getValue()).isEqualTo(BLUETOOTH_CONNECT);

        // Verify that user can get battery level after the update
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1)).isNotNull();
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1).getBatteryLevel())
                .isEqualTo(batteryLevel);

        // Verify that update same battery level for the same device does not trigger intent
        mRemoteDevices.updateBatteryLevel(mDevice1, batteryLevel, /* fromBas= */ false);
        verify(mAdapterService).sendBroadcast(any(), anyString(), any());

        // Verify that updating battery level to different value triggers the intent again
        batteryLevel = 15;
        mRemoteDevices.updateBatteryLevel(mDevice1, batteryLevel, /* fromBas= */ false);
        verify(mAdapterService, times(2))
                .sendBroadcast(
                        mIntentArgument.capture(), mStringArgument.capture(), any(Bundle.class));
        assertThat(mStringArgument.getValue()).isEqualTo(BLUETOOTH_CONNECT);
        verifyBatteryLevelChangedIntent(mDevice1, batteryLevel, mIntentArgument);

        // Verify that user can get battery level after the update
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1).getBatteryLevel())
                .isEqualTo(batteryLevel);

        verifyNoMoreInteractions(mAdapterService);
    }

    @Test
    public void testUpdateBatteryLevel_errorNegativeValue() {
        int batteryLevel = BluetoothDevice.BATTERY_LEVEL_UNKNOWN;

        // Verify that device property is null initially
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1)).isNull();

        // Verify that updating with invalid battery level does not trigger the intent
        mRemoteDevices.updateBatteryLevel(mDevice1, batteryLevel, /* fromBas= */ false);
        verify(mAdapterService, never()).sendBroadcast(any(), anyString(), any());

        // Verify that device property stays null after invalid update
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1)).isNull();

        verifyNoMoreInteractions(mAdapterService);
    }

    @Test
    public void testUpdateBatteryLevel_errorTooLargeValue() {
        int batteryLevel = 101;

        // Verify that device property is null initially
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1)).isNull();

        // Verify that updating invalid battery level does not trigger the intent
        mRemoteDevices.updateBatteryLevel(mDevice1, batteryLevel, /* fromBas= */ false);
        verify(mAdapterService, never()).sendBroadcast(any(), anyString(), any());

        // Verify that device property stays null after invalid update
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1)).isNull();

        verifyNoMoreInteractions(mAdapterService);
    }

    @Test
    public void testResetBatteryLevel_testResetBeforeUpdate() {
        // Verify that device property is null initially
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1)).isNull();

        // Verify that resetting battery level keeps device property null
        mRemoteDevices.resetBatteryLevel(mDevice1, /* fromBas= */ false);
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1)).isNull();

        verifyNoMoreInteractions(mAdapterService);
    }

    @Test
    public void testResetBatteryLevel_testResetAfterUpdate() {
        int batteryLevel = 10;

        // Verify that device property is null initially
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1)).isNull();

        // Verify that updating battery level triggers ACTION_BATTERY_LEVEL_CHANGED intent
        mRemoteDevices.updateBatteryLevel(mDevice1, batteryLevel, /* fromBas= */ false);
        verify(mAdapterService)
                .sendBroadcast(
                        mIntentArgument.capture(), mStringArgument.capture(), any(Bundle.class));
        verifyBatteryLevelChangedIntent(mDevice1, batteryLevel, mIntentArgument);
        assertThat(mStringArgument.getValue()).isEqualTo(BLUETOOTH_CONNECT);

        // Verify that user can get battery level after the update
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1)).isNotNull();
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1).getBatteryLevel())
                .isEqualTo(batteryLevel);

        // Verify that resetting battery level changes it back to BluetoothDevice
        // .BATTERY_LEVEL_UNKNOWN
        mRemoteDevices.resetBatteryLevel(mDevice1, /* fromBas= */ false);
        // Verify BATTERY_LEVEL_CHANGED intent is sent after first reset
        verify(mAdapterService, times(2))
                .sendBroadcast(
                        mIntentArgument.capture(), mStringArgument.capture(), any(Bundle.class));
        verifyBatteryLevelChangedIntent(
                mDevice1, BluetoothDevice.BATTERY_LEVEL_UNKNOWN, mIntentArgument);
        assertThat(mStringArgument.getValue()).isEqualTo(BLUETOOTH_CONNECT);
        // Verify value is reset in properties
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1)).isNotNull();
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1).getBatteryLevel())
                .isEqualTo(BluetoothDevice.BATTERY_LEVEL_UNKNOWN);

        // Verify no intent is sent after second reset
        mRemoteDevices.resetBatteryLevel(mDevice1, /* fromBas= */ false);
        verify(mAdapterService, times(2)).sendBroadcast(any(), anyString(), any());

        // Verify that updating battery level triggers ACTION_BATTERY_LEVEL_CHANGED intent again
        mRemoteDevices.updateBatteryLevel(mDevice1, batteryLevel, /* fromBas= */ false);
        verify(mAdapterService, times(3))
                .sendBroadcast(
                        mIntentArgument.capture(), mStringArgument.capture(), any(Bundle.class));
        verifyBatteryLevelChangedIntent(mDevice1, batteryLevel, mIntentArgument);
        assertThat(mStringArgument.getValue()).isEqualTo(BLUETOOTH_CONNECT);

        verifyNoMoreInteractions(mAdapterService);
    }

    @Test
    public void testResetBatteryLevelOnHeadsetStateChange() {
        int batteryLevel = 10;

        // Verify that device property is null initially
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1)).isNull();

        // Verify that updating battery level triggers ACTION_BATTERY_LEVEL_CHANGED intent
        mRemoteDevices.updateBatteryLevel(mDevice1, batteryLevel, /* fromBas= */ false);
        verify(mAdapterService)
                .sendBroadcast(
                        mIntentArgument.capture(), mStringArgument.capture(), any(Bundle.class));
        verifyBatteryLevelChangedIntent(mDevice1, batteryLevel, mIntentArgument);
        assertThat(mStringArgument.getValue()).isEqualTo(BLUETOOTH_CONNECT);

        // Verify that user can get battery level after the update
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1)).isNotNull();
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1).getBatteryLevel())
                .isEqualTo(batteryLevel);

        // Verify that resetting battery level changes it back to BluetoothDevice
        // .BATTERY_LEVEL_UNKNOWN
        mRemoteDevices.onHeadsetConnectionStateChanged(
                mDevice1,
                BluetoothProfile.STATE_DISCONNECTING,
                BluetoothProfile.STATE_DISCONNECTED);
        // Verify BATTERY_LEVEL_CHANGED intent is sent after first reset
        verify(mAdapterService, times(2))
                .sendBroadcast(
                        mIntentArgument.capture(), mStringArgument.capture(), any(Bundle.class));
        verifyBatteryLevelChangedIntent(
                mDevice1, BluetoothDevice.BATTERY_LEVEL_UNKNOWN, mIntentArgument);
        assertThat(mStringArgument.getValue()).isEqualTo(BLUETOOTH_CONNECT);
        // Verify value is reset in properties
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1)).isNotNull();
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1).getBatteryLevel())
                .isEqualTo(BluetoothDevice.BATTERY_LEVEL_UNKNOWN);

        // Verify that updating battery level triggers ACTION_BATTERY_LEVEL_CHANGED intent again
        mRemoteDevices.updateBatteryLevel(mDevice1, batteryLevel, /* fromBas= */ false);
        verify(mAdapterService, times(3))
                .sendBroadcast(
                        mIntentArgument.capture(), mStringArgument.capture(), any(Bundle.class));
        verifyBatteryLevelChangedIntent(mDevice1, batteryLevel, mIntentArgument);
        assertThat(mStringArgument.getValue()).isEqualTo(BLUETOOTH_CONNECT);

        verifyNoMoreInteractions(mAdapterService);
    }

    @Test
    public void testOnHeadsetStateChangeWithBatteryService_NotResetBatteryLevel() {
        int batteryLevel = 10;

        BatteryService oldBatteryService = setBatteryServiceForTesting(mDevice1);
        assertThat(mRemoteDevices.hasBatteryService(mDevice1)).isTrue();

        // Verify that device property is null initially
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1)).isNull();

        // Verify that updating battery level triggers ACTION_BATTERY_LEVEL_CHANGED intent
        mRemoteDevices.updateBatteryLevel(mDevice1, batteryLevel, /* fromBas= */ false);
        verify(mAdapterService)
                .sendBroadcast(
                        mIntentArgument.capture(), mStringArgument.capture(), any(Bundle.class));
        verifyBatteryLevelChangedIntent(mDevice1, batteryLevel, mIntentArgument);
        assertThat(mStringArgument.getValue()).isEqualTo(BLUETOOTH_CONNECT);

        // Verify that user can get battery level after the update
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1)).isNotNull();
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1).getBatteryLevel())
                .isEqualTo(batteryLevel);

        // Verify that battery level is not reset
        mRemoteDevices.onHeadsetConnectionStateChanged(
                mDevice1,
                BluetoothProfile.STATE_DISCONNECTING,
                BluetoothProfile.STATE_DISCONNECTED);

        assertThat(mRemoteDevices.getDeviceProperties(mDevice1)).isNotNull();
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1).getBatteryLevel())
                .isEqualTo(batteryLevel);

        // Recover the previous battery service if exists
        clearBatteryServiceForTesting(oldBatteryService);

        verifyNoMoreInteractions(mAdapterService);
    }

    @Test
    @Ignore("b/266128644")
    public void testResetBatteryLevel_testAclStateChangeCallback() {
        int batteryLevel = 10;

        // Verify that device property is null initially
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1)).isNull();

        // Verify that updating battery level triggers ACTION_BATTERY_LEVEL_CHANGED intent
        mRemoteDevices.updateBatteryLevel(mDevice1, batteryLevel, /* fromBas= */ false);
        verify(mAdapterService)
                .sendBroadcast(
                        mIntentArgument.capture(), mStringArgument.capture(), any(Bundle.class));
        verifyBatteryLevelChangedIntent(mDevice1, batteryLevel, mIntentArgument);
        assertThat(mStringArgument.getValue()).isEqualTo(BLUETOOTH_CONNECT);

        // Verify that user can get battery level after the update
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1)).isNotNull();
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1).getBatteryLevel())
                .isEqualTo(batteryLevel);

        // Verify that when device is completely disconnected, RemoteDevices reset battery level to
        // BluetoothDevice.BATTERY_LEVEL_UNKNOWN
        when(mAdapterService.getState()).thenReturn(BluetoothAdapter.STATE_ON);
        mRemoteDevices.aclStateChangeCallback(
                0,
                Utils.getByteAddress(mDevice1),
                AbstractionLayer.BT_ACL_STATE_DISCONNECTED,
                2,
                19,
                BluetoothDevice.ERROR); // HCI code 19 remote terminated
        // Verify ACTION_ACL_DISCONNECTED and BATTERY_LEVEL_CHANGED intent are sent
        verify(mAdapterService, times(3))
                .sendBroadcast(
                        mIntentArgument.capture(), mStringArgument.capture(), any(Bundle.class));
        verify(mAdapterService, times(2)).obfuscateAddress(mDevice1);
        verifyBatteryLevelChangedIntent(
                mDevice1,
                BluetoothDevice.BATTERY_LEVEL_UNKNOWN,
                mIntentArgument.getAllValues().get(mIntentArgument.getAllValues().size() - 2));
        assertThat(mStringArgument.getAllValues().get(mStringArgument.getAllValues().size() - 2))
                .isEqualTo(BLUETOOTH_CONNECT);
        assertThat(mIntentArgument.getValue().getAction())
                .isEqualTo(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        assertThat(mStringArgument.getValue()).isEqualTo(BLUETOOTH_CONNECT);
        // Verify value is reset in properties
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1)).isNotNull();
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1).getBatteryLevel())
                .isEqualTo(BluetoothDevice.BATTERY_LEVEL_UNKNOWN);

        // Verify that updating battery level triggers ACTION_BATTERY_LEVEL_CHANGED intent again
        mRemoteDevices.updateBatteryLevel(mDevice1, batteryLevel, /* fromBas= */ false);
        verify(mAdapterService, times(4))
                .sendBroadcast(
                        mIntentArgument.capture(), mStringArgument.capture(), any(Bundle.class));
        verifyBatteryLevelChangedIntent(mDevice1, batteryLevel, mIntentArgument);
        assertThat(mStringArgument.getValue()).isEqualTo(BLUETOOTH_CONNECT);
    }

    @Test
    public void testHfIndicatorParser_testCorrectValue() {
        int batteryLevel = 10;

        // Verify that device property is null initially
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1)).isNull();

        // Verify that ACTION_HF_INDICATORS_VALUE_CHANGED intent updates battery level
        mRemoteDevices.onHfIndicatorValueChanged(
                mDevice1, HeadsetHalConstants.HF_INDICATOR_BATTERY_LEVEL_STATUS, batteryLevel);
        verify(mAdapterService)
                .sendBroadcast(
                        mIntentArgument.capture(), mStringArgument.capture(), any(Bundle.class));
        verifyBatteryLevelChangedIntent(mDevice1, batteryLevel, mIntentArgument);
        assertThat(mStringArgument.getValue()).isEqualTo(BLUETOOTH_CONNECT);
    }

    @Test
    public void testHfIndicatorParser_testWrongIndicatorId() {
        int batteryLevel = 10;

        // Verify that device property is null initially
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1)).isNull();

        // Verify that ACTION_HF_INDICATORS_VALUE_CHANGED intent updates battery level
        mRemoteDevices.onHfIndicatorValueChanged(mDevice1, batteryLevel, 3);
        verify(mAdapterService, never()).sendBroadcast(any(), anyString());
        // Verify that device property is still null after invalid update
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1)).isNull();
    }

    @Test
    public void testOnVendorSpecificHeadsetEvent_testCorrectPlantronicsXEvent() {
        // Verify that device property is null initially
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1)).isNull();

        // Verify that correct ACTION_VENDOR_SPECIFIC_HEADSET_EVENT updates battery level
        mRemoteDevices.onVendorSpecificHeadsetEvent(
                mDevice1,
                BluetoothHeadset.VENDOR_SPECIFIC_HEADSET_EVENT_XEVENT,
                BluetoothAssignedNumbers.PLANTRONICS,
                BluetoothHeadset.AT_CMD_TYPE_SET,
                getXEventArray(3, 8));
        verify(mAdapterService)
                .sendBroadcast(
                        mIntentArgument.capture(), mStringArgument.capture(), any(Bundle.class));
        verifyBatteryLevelChangedIntent(mDevice1, 42, mIntentArgument);
        assertThat(mStringArgument.getValue()).isEqualTo(BLUETOOTH_CONNECT);
    }

    @Test
    public void testOnVendorSpecificHeadsetEvent_testCorrectAppleBatteryVsc() {
        // Verify that device property is null initially
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1)).isNull();

        // Verify that correct ACTION_VENDOR_SPECIFIC_HEADSET_EVENT updates battery level
        mRemoteDevices.onVendorSpecificHeadsetEvent(
                mDevice1,
                BluetoothHeadset.VENDOR_SPECIFIC_HEADSET_EVENT_IPHONEACCEV,
                BluetoothAssignedNumbers.APPLE,
                BluetoothHeadset.AT_CMD_TYPE_SET,
                new Object[] {
                    3,
                    BluetoothHeadset.VENDOR_SPECIFIC_HEADSET_EVENT_IPHONEACCEV_BATTERY_LEVEL,
                    5,
                    2,
                    1,
                    3,
                    10
                });
        verify(mAdapterService)
                .sendBroadcast(
                        mIntentArgument.capture(), mStringArgument.capture(), any(Bundle.class));
        verifyBatteryLevelChangedIntent(mDevice1, 60, mIntentArgument);
        assertThat(mStringArgument.getValue()).isEqualTo(BLUETOOTH_CONNECT);
    }

    @Test
    public void testGetBatteryLevelFromXEventVsc() {
        assertThat(RemoteDevices.getBatteryLevelFromXEventVsc(getXEventArray(3, 8))).isEqualTo(42);
        assertThat(RemoteDevices.getBatteryLevelFromXEventVsc(getXEventArray(10, 11)))
                .isEqualTo(100);
        assertThat(RemoteDevices.getBatteryLevelFromXEventVsc(getXEventArray(1, 1)))
                .isEqualTo(BluetoothDevice.BATTERY_LEVEL_UNKNOWN);
        assertThat(RemoteDevices.getBatteryLevelFromXEventVsc(getXEventArray(3, 1)))
                .isEqualTo(BluetoothDevice.BATTERY_LEVEL_UNKNOWN);
        assertThat(RemoteDevices.getBatteryLevelFromXEventVsc(getXEventArray(-1, 1)))
                .isEqualTo(BluetoothDevice.BATTERY_LEVEL_UNKNOWN);
        assertThat(RemoteDevices.getBatteryLevelFromXEventVsc(getXEventArray(-1, -1)))
                .isEqualTo(BluetoothDevice.BATTERY_LEVEL_UNKNOWN);
    }

    @Test
    public void testGetBatteryLevelFromAppleBatteryVsc() {
        assertThat(
                        RemoteDevices.getBatteryLevelFromAppleBatteryVsc(
                                new Object[] {
                                    1,
                                    BluetoothHeadset
                                            .VENDOR_SPECIFIC_HEADSET_EVENT_IPHONEACCEV_BATTERY_LEVEL,
                                    0
                                }))
                .isEqualTo(10);
        assertThat(
                        RemoteDevices.getBatteryLevelFromAppleBatteryVsc(
                                new Object[] {
                                    1,
                                    BluetoothHeadset
                                            .VENDOR_SPECIFIC_HEADSET_EVENT_IPHONEACCEV_BATTERY_LEVEL,
                                    9
                                }))
                .isEqualTo(100);
        assertThat(
                        RemoteDevices.getBatteryLevelFromAppleBatteryVsc(
                                new Object[] {
                                    3,
                                    BluetoothHeadset
                                            .VENDOR_SPECIFIC_HEADSET_EVENT_IPHONEACCEV_BATTERY_LEVEL,
                                    5,
                                    2,
                                    1,
                                    3,
                                    10
                                }))
                .isEqualTo(60);
        assertThat(
                        RemoteDevices.getBatteryLevelFromAppleBatteryVsc(
                                new Object[] {
                                    3,
                                    BluetoothHeadset
                                            .VENDOR_SPECIFIC_HEADSET_EVENT_IPHONEACCEV_BATTERY_LEVEL,
                                    5,
                                    2,
                                    1,
                                    3
                                }))
                .isEqualTo(BluetoothDevice.BATTERY_LEVEL_UNKNOWN);
        assertThat(
                        RemoteDevices.getBatteryLevelFromAppleBatteryVsc(
                                new Object[] {
                                    1,
                                    BluetoothHeadset
                                            .VENDOR_SPECIFIC_HEADSET_EVENT_IPHONEACCEV_BATTERY_LEVEL,
                                    10
                                }))
                .isEqualTo(BluetoothDevice.BATTERY_LEVEL_UNKNOWN);
        assertThat(
                        RemoteDevices.getBatteryLevelFromAppleBatteryVsc(
                                new Object[] {
                                    1,
                                    BluetoothHeadset
                                            .VENDOR_SPECIFIC_HEADSET_EVENT_IPHONEACCEV_BATTERY_LEVEL,
                                    -1
                                }))
                .isEqualTo(BluetoothDevice.BATTERY_LEVEL_UNKNOWN);
        assertThat(
                        RemoteDevices.getBatteryLevelFromAppleBatteryVsc(
                                new Object[] {
                                    1,
                                    BluetoothHeadset
                                            .VENDOR_SPECIFIC_HEADSET_EVENT_IPHONEACCEV_BATTERY_LEVEL,
                                    "5"
                                }))
                .isEqualTo(BluetoothDevice.BATTERY_LEVEL_UNKNOWN);
        assertThat(RemoteDevices.getBatteryLevelFromAppleBatteryVsc(new Object[] {1, 35, 37}))
                .isEqualTo(BluetoothDevice.BATTERY_LEVEL_UNKNOWN);
        assertThat(
                        RemoteDevices.getBatteryLevelFromAppleBatteryVsc(
                                new Object[] {1, "WRONG", "WRONG"}))
                .isEqualTo(BluetoothDevice.BATTERY_LEVEL_UNKNOWN);
    }

    @Test
    public void testResetBatteryLevelOnHeadsetClientStateChange() {
        int batteryLevel = 10;

        // Verify that device property is null initially
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1)).isNull();

        // Verify that updating battery level triggers ACTION_BATTERY_LEVEL_CHANGED intent
        mRemoteDevices.updateBatteryLevel(mDevice1, batteryLevel, /* fromBas= */ false);
        verify(mAdapterService)
                .sendBroadcast(
                        mIntentArgument.capture(), mStringArgument.capture(), any(Bundle.class));
        verifyBatteryLevelChangedIntent(mDevice1, batteryLevel, mIntentArgument);
        assertThat(mStringArgument.getValue()).isEqualTo(BLUETOOTH_CONNECT);

        // Verify that user can get battery level after the update
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1)).isNotNull();
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1).getBatteryLevel())
                .isEqualTo(batteryLevel);

        // Verify that resetting battery level changes it back to BluetoothDevice
        // .BATTERY_LEVEL_UNKNOWN
        mRemoteDevices.onHeadsetClientConnectionStateChanged(
                mDevice1,
                BluetoothProfile.STATE_DISCONNECTING,
                BluetoothProfile.STATE_DISCONNECTED);

        // Verify BATTERY_LEVEL_CHANGED intent is sent after first reset
        verify(mAdapterService, times(2))
                .sendBroadcast(
                        mIntentArgument.capture(), mStringArgument.capture(), any(Bundle.class));
        verifyBatteryLevelChangedIntent(
                mDevice1, BluetoothDevice.BATTERY_LEVEL_UNKNOWN, mIntentArgument);
        assertThat(mStringArgument.getValue()).isEqualTo(BLUETOOTH_CONNECT);

        // Verify value is reset in properties
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1)).isNotNull();
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1).getBatteryLevel())
                .isEqualTo(BluetoothDevice.BATTERY_LEVEL_UNKNOWN);

        // Verify that updating battery level triggers ACTION_BATTERY_LEVEL_CHANGED intent again
        mRemoteDevices.updateBatteryLevel(mDevice1, batteryLevel, /* fromBas= */ false);
        verify(mAdapterService, times(3))
                .sendBroadcast(
                        mIntentArgument.capture(), mStringArgument.capture(), any(Bundle.class));
        verifyBatteryLevelChangedIntent(mDevice1, batteryLevel, mIntentArgument);
        assertThat(mStringArgument.getValue()).isEqualTo(BLUETOOTH_CONNECT);

        verifyNoMoreInteractions(mAdapterService);
    }

    @Test
    public void testHeadsetClientDisconnectedWithBatteryService_NotResetBatteryLevel() {
        int batteryLevel = 10;

        BatteryService oldBatteryService = setBatteryServiceForTesting(mDevice1);
        assertThat(mRemoteDevices.hasBatteryService(mDevice1)).isTrue();

        // Verify that device property is null initially
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1)).isNull();

        // Verify that updating battery level triggers ACTION_BATTERY_LEVEL_CHANGED intent
        mRemoteDevices.updateBatteryLevel(mDevice1, batteryLevel, /* fromBas= */ false);
        verify(mAdapterService)
                .sendBroadcast(
                        mIntentArgument.capture(), mStringArgument.capture(), any(Bundle.class));
        verifyBatteryLevelChangedIntent(mDevice1, batteryLevel, mIntentArgument);
        assertThat(mStringArgument.getValue()).isEqualTo(BLUETOOTH_CONNECT);

        // Verify that user can get battery level after the update
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1)).isNotNull();
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1).getBatteryLevel())
                .isEqualTo(batteryLevel);

        // Verify that battery level is not reset.
        mRemoteDevices.onHeadsetClientConnectionStateChanged(
                mDevice1,
                BluetoothProfile.STATE_DISCONNECTING,
                BluetoothProfile.STATE_DISCONNECTED);

        assertThat(mRemoteDevices.getDeviceProperties(mDevice1)).isNotNull();
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1).getBatteryLevel())
                .isEqualTo(batteryLevel);

        clearBatteryServiceForTesting(oldBatteryService);

        verifyNoMoreInteractions(mAdapterService);
    }

    @Test
    public void testUpdateBatteryLevelWithBas_overridesHfpBatteryLevel() {
        int batteryLevel = 10;
        int batteryLevel2 = 20;

        BatteryService oldBatteryService = setBatteryServiceForTesting(mDevice1);
        assertThat(mRemoteDevices.hasBatteryService(mDevice1)).isTrue();

        // Verify that device property is null initially
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1)).isNull();

        // Verify that updating battery level triggers ACTION_BATTERY_LEVEL_CHANGED intent
        mRemoteDevices.updateBatteryLevel(mDevice1, batteryLevel, /* fromBas= */ false);
        verify(mAdapterService)
                .sendBroadcast(
                        mIntentArgument.capture(), mStringArgument.capture(), any(Bundle.class));
        verifyBatteryLevelChangedIntent(mDevice1, batteryLevel, mIntentArgument);
        assertThat(mStringArgument.getValue()).isEqualTo(BLUETOOTH_CONNECT);

        // Verify that user can get battery level after the update
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1)).isNotNull();
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1).getBatteryLevel())
                .isEqualTo(batteryLevel);

        // Verify that updating battery service overrides hfp battery level
        mRemoteDevices.updateBatteryLevel(mDevice1, batteryLevel2, /* fromBas= */ true);
        verify(mAdapterService, times(2))
                .sendBroadcast(
                        mIntentArgument.capture(), mStringArgument.capture(), any(Bundle.class));
        verifyBatteryLevelChangedIntent(mDevice1, batteryLevel2, mIntentArgument);

        // Verify that the battery level isn't reset
        mRemoteDevices.resetBatteryLevel(mDevice1, /* fromBas= */ true);
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1).getBatteryLevel())
                .isEqualTo(batteryLevel);
        verify(mAdapterService, times(3))
                .sendBroadcast(
                        mIntentArgument.capture(), mStringArgument.capture(), any(Bundle.class));
        verifyBatteryLevelChangedIntent(mDevice1, batteryLevel, mIntentArgument);

        clearBatteryServiceForTesting(oldBatteryService);

        verifyNoMoreInteractions(mAdapterService);
    }

    @Test
    public void testUpdateBatteryLevelWithSameValue_notSendBroadcast() {
        int batteryLevel = 10;

        BatteryService oldBatteryService = setBatteryServiceForTesting(mDevice1);
        assertThat(mRemoteDevices.hasBatteryService(mDevice1)).isTrue();

        // Verify that device property is null initially
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1)).isNull();

        // Verify that updating battery level triggers ACTION_BATTERY_LEVEL_CHANGED intent
        mRemoteDevices.updateBatteryLevel(mDevice1, batteryLevel, /* fromBas= */ false);
        verify(mAdapterService)
                .sendBroadcast(
                        mIntentArgument.capture(), mStringArgument.capture(), any(Bundle.class));
        verifyBatteryLevelChangedIntent(mDevice1, batteryLevel, mIntentArgument);
        assertThat(mStringArgument.getValue()).isEqualTo(BLUETOOTH_CONNECT);

        // Verify that user can get battery level after the update
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1)).isNotNull();
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1).getBatteryLevel())
                .isEqualTo(batteryLevel);

        // Verify that updating battery service doesn't send broadcast
        mRemoteDevices.updateBatteryLevel(mDevice1, batteryLevel, /* fromBas= */ true);
        verifyNoMoreInteractions(mAdapterService);

        // Verify that the battery level isn't reset
        mRemoteDevices.resetBatteryLevel(mDevice1, /* fromBas= */ true);
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1).getBatteryLevel())
                .isEqualTo(batteryLevel);
        verifyNoMoreInteractions(mAdapterService);

        clearBatteryServiceForTesting(oldBatteryService);

        verifyNoMoreInteractions(mAdapterService);
    }

    @Test
    public void testAgBatteryLevelIndicator_testCorrectValue() {
        int batteryLevel = 3;

        // Verify that device property is null initially
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1)).isNull();

        // Verify that ACTION_AG_EVENT intent updates battery level
        mRemoteDevices.onAgBatteryLevelChanged(mDevice1, batteryLevel);
        verify(mAdapterService)
                .sendBroadcast(
                        mIntentArgument.capture(), mStringArgument.capture(), any(Bundle.class));
        verifyBatteryLevelChangedIntent(
                mDevice1,
                RemoteDevices.batteryChargeIndicatorToPercentge(batteryLevel),
                mIntentArgument);
        assertThat(mStringArgument.getValue()).isEqualTo(BLUETOOTH_CONNECT);
    }

    @Test
    @EnableFlags(Flags.FLAG_FIX_ADD_DEVICE_PROPERTIES)
    public void testMultipleAddDeviceProperties() {
        // Verify that device property is null initially
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1)).isNull();
        DeviceProperties prop1 =
                mRemoteDevices.addDeviceProperties(Utils.getBytesFromAddress(TEST_BT_ADDR_1));
        DeviceProperties prop2 =
                mRemoteDevices.addDeviceProperties(Utils.getBytesFromAddress(TEST_BT_ADDR_1));
        assertThat(prop1).isEqualTo(prop2);
    }

    @Test
    public void testSetgetHfAudioPolicyForRemoteAg() {
        // Verify that device property is null initially
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1)).isNull();

        mRemoteDevices.addDeviceProperties(Utils.getBytesFromAddress(TEST_BT_ADDR_1));

        DeviceProperties deviceProp = mRemoteDevices.getDeviceProperties(mDevice1);
        BluetoothSinkAudioPolicy policies =
                new BluetoothSinkAudioPolicy.Builder()
                        .setCallEstablishPolicy(BluetoothSinkAudioPolicy.POLICY_ALLOWED)
                        .setActiveDevicePolicyAfterConnection(
                                BluetoothSinkAudioPolicy.POLICY_ALLOWED)
                        .setInBandRingtonePolicy(BluetoothSinkAudioPolicy.POLICY_ALLOWED)
                        .build();
        deviceProp.setHfAudioPolicyForRemoteAg(policies);

        // Verify that the audio policy properties are set and get properly
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1).getHfAudioPolicyForRemoteAg())
                .isEqualTo(policies);
    }

    @Test
    public void testIsCoordinatedSetMemberAsLeAudioEnabled() {
        doReturn((long) (1 << BluetoothProfile.CSIP_SET_COORDINATOR))
                .when(mAdapterService)
                .getSupportedProfilesBitMask();

        // Verify that device property is null initially
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1)).isNull();
        mRemoteDevices.addDeviceProperties(Utils.getBytesFromAddress(TEST_BT_ADDR_1));

        DeviceProperties deviceProp = mRemoteDevices.getDeviceProperties(mDevice1);
        deviceProp.setIsCoordinatedSetMember(true);

        assertThat(deviceProp.isCoordinatedSetMember()).isTrue();
    }

    @Test
    public void testIsCoordinatedSetMemberAsLeAudioDisabled() {
        doReturn((long) (0 << BluetoothProfile.CSIP_SET_COORDINATOR))
                .when(mAdapterService)
                .getSupportedProfilesBitMask();

        // Verify that device property is null initially
        assertThat(mRemoteDevices.getDeviceProperties(mDevice1)).isNull();
        mRemoteDevices.addDeviceProperties(Utils.getBytesFromAddress(TEST_BT_ADDR_1));

        DeviceProperties deviceProp = mRemoteDevices.getDeviceProperties(mDevice1);
        deviceProp.setIsCoordinatedSetMember(true);

        assertThat(deviceProp.isCoordinatedSetMember()).isFalse();
    }

    @Test
    public void testIsDeviceNull() {
        assertThat(mRemoteDevices.getDeviceProperties(null)).isNull();
    }

    private static void verifyBatteryLevelChangedIntent(
            BluetoothDevice device, int batteryLevel, ArgumentCaptor<Intent> intentArgument) {
        verifyBatteryLevelChangedIntent(device, batteryLevel, intentArgument.getValue());
    }

    private static void verifyBatteryLevelChangedIntent(
            BluetoothDevice device, int batteryLevel, Intent intent) {
        assertThat(intent.getAction()).isEqualTo(BluetoothDevice.ACTION_BATTERY_LEVEL_CHANGED);
        assertThat(intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class))
                .isEqualTo(device);
        assertThat(intent.getIntExtra(BluetoothDevice.EXTRA_BATTERY_LEVEL, -15))
                .isEqualTo(batteryLevel);
        assertThat(intent.getFlags())
                .isEqualTo(
                        Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT
                                | Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND);
    }

    private static Object[] getXEventArray(int batteryLevel, int numLevels) {
        ArrayList<Object> list = new ArrayList<>();
        list.add(BluetoothHeadset.VENDOR_SPECIFIC_HEADSET_EVENT_XEVENT_BATTERY_LEVEL);
        list.add(batteryLevel);
        list.add(numLevels);
        list.add(0);
        list.add(0);
        return list.toArray();
    }

    private static BatteryService setBatteryServiceForTesting(BluetoothDevice device) {
        BatteryService newService = mock(BatteryService.class);
        when(newService.getConnectionState(device)).thenReturn(BluetoothProfile.STATE_CONNECTED);
        when(newService.isAvailable()).thenReturn(true);

        BatteryService oldService = BatteryService.getBatteryService();
        BatteryService.setBatteryService(newService);

        return oldService;
    }

    private static void clearBatteryServiceForTesting(BatteryService service) {
        BatteryService.setBatteryService(service);
    }
}
