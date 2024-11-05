/*
 * Copyright 2024 The Android Open Source Project
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

package com.android.bluetooth.vc;

import static com.android.bluetooth.vc.VolumeControlStackEvent.EVENT_TYPE_CONNECTION_STATE_CHANGED;
import static com.android.bluetooth.vc.VolumeControlStackEvent.EVENT_TYPE_DEVICE_AVAILABLE;
import static com.android.bluetooth.vc.VolumeControlStackEvent.EVENT_TYPE_EXT_AUDIO_IN_DESCR_CHANGED;
import static com.android.bluetooth.vc.VolumeControlStackEvent.EVENT_TYPE_EXT_AUDIO_IN_GAIN_PROPS_CHANGED;
import static com.android.bluetooth.vc.VolumeControlStackEvent.EVENT_TYPE_EXT_AUDIO_IN_STATE_CHANGED;
import static com.android.bluetooth.vc.VolumeControlStackEvent.EVENT_TYPE_EXT_AUDIO_IN_STATUS_CHANGED;
import static com.android.bluetooth.vc.VolumeControlStackEvent.EVENT_TYPE_EXT_AUDIO_IN_TYPE_CHANGED;
import static com.android.bluetooth.vc.VolumeControlStackEvent.EVENT_TYPE_EXT_AUDIO_OUT_DESCRIPTION_CHANGED;
import static com.android.bluetooth.vc.VolumeControlStackEvent.EVENT_TYPE_EXT_AUDIO_OUT_LOCATION_CHANGED;
import static com.android.bluetooth.vc.VolumeControlStackEvent.EVENT_TYPE_EXT_AUDIO_OUT_VOL_OFFSET_CHANGED;
import static com.android.bluetooth.vc.VolumeControlStackEvent.EVENT_TYPE_VOLUME_STATE_CHANGED;

import static java.util.Objects.requireNonNull;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import com.android.bluetooth.btservice.AdapterService;
import com.android.internal.annotations.VisibleForTesting;

class VolumeControlNativeCallback {
    private static final String TAG = VolumeControlNativeCallback.class.getSimpleName();

    private final AdapterService mAdapterService;
    private final VolumeControlService mVolumeControlService;

    VolumeControlNativeCallback(
            AdapterService adapterService, VolumeControlService volumeControlService) {
        mAdapterService = requireNonNull(adapterService);
        mVolumeControlService = requireNonNull(volumeControlService);
    }

    private BluetoothDevice getDevice(byte[] address) {
        return mAdapterService.getDeviceFromByte(address);
    }

    @VisibleForTesting
    void onConnectionStateChanged(int state, byte[] address) {
        VolumeControlStackEvent event =
                new VolumeControlStackEvent(EVENT_TYPE_CONNECTION_STATE_CHANGED);
        event.device = getDevice(address);
        event.valueInt1 = state;

        Log.d(TAG, "onConnectionStateChanged: " + event);
        mVolumeControlService.messageFromNative(event);
    }

    @VisibleForTesting
    void onVolumeStateChanged(
            int volume, boolean mute, int flags, byte[] address, boolean isAutonomous) {
        VolumeControlStackEvent event =
                new VolumeControlStackEvent(EVENT_TYPE_VOLUME_STATE_CHANGED);
        event.device = getDevice(address);
        event.valueInt1 = -1;
        event.valueInt2 = volume;
        event.valueInt3 = flags;
        event.valueBool1 = mute;
        event.valueBool2 = isAutonomous;

        Log.d(TAG, "onVolumeStateChanged: " + event);
        mVolumeControlService.messageFromNative(event);
    }

    @VisibleForTesting
    void onGroupVolumeStateChanged(int volume, boolean mute, int groupId, boolean isAutonomous) {
        VolumeControlStackEvent event =
                new VolumeControlStackEvent(EVENT_TYPE_VOLUME_STATE_CHANGED);
        event.device = null;
        event.valueInt1 = groupId;
        event.valueInt2 = volume;
        event.valueBool1 = mute;
        event.valueBool2 = isAutonomous;

        Log.d(TAG, "onGroupVolumeStateChanged: " + event);
        mVolumeControlService.messageFromNative(event);
    }

    @VisibleForTesting
    void onDeviceAvailable(int numOfExternalOutputs, int numOfExternalInputs, byte[] address) {
        VolumeControlStackEvent event = new VolumeControlStackEvent(EVENT_TYPE_DEVICE_AVAILABLE);
        event.device = getDevice(address);
        event.valueInt1 = numOfExternalOutputs;
        event.valueInt2 = numOfExternalInputs;

        Log.d(TAG, "onDeviceAvailable: " + event);
        mVolumeControlService.messageFromNative(event);
    }

    @VisibleForTesting
    void onExtAudioOutVolumeOffsetChanged(int externalOutputId, int offset, byte[] address) {
        VolumeControlStackEvent event =
                new VolumeControlStackEvent(EVENT_TYPE_EXT_AUDIO_OUT_VOL_OFFSET_CHANGED);
        event.device = getDevice(address);
        event.valueInt1 = externalOutputId;
        event.valueInt2 = offset;

        Log.d(TAG, "onExtAudioOutVolumeOffsetChanged: " + event);
        mVolumeControlService.messageFromNative(event);
    }

    @VisibleForTesting
    void onExtAudioOutLocationChanged(int externalOutputId, int location, byte[] address) {
        VolumeControlStackEvent event =
                new VolumeControlStackEvent(EVENT_TYPE_EXT_AUDIO_OUT_LOCATION_CHANGED);
        event.device = getDevice(address);
        event.valueInt1 = externalOutputId;
        event.valueInt2 = location;

        Log.d(TAG, "onExtAudioOutLocationChanged: " + event);
        mVolumeControlService.messageFromNative(event);
    }

    @VisibleForTesting
    void onExtAudioOutDescriptionChanged(int externalOutputId, String descr, byte[] address) {
        VolumeControlStackEvent event =
                new VolumeControlStackEvent(EVENT_TYPE_EXT_AUDIO_OUT_DESCRIPTION_CHANGED);
        event.device = getDevice(address);
        event.valueInt1 = externalOutputId;
        event.valueString1 = descr;

        Log.d(TAG, "onExtAudioOutLocationChanged: " + event);
        mVolumeControlService.messageFromNative(event);
    }

    @VisibleForTesting
    void onExtAudioInStateChanged(
            int externalInputId, int gainSetting, int mute, int gainMode, byte[] address) {
        VolumeControlStackEvent event =
                new VolumeControlStackEvent(EVENT_TYPE_EXT_AUDIO_IN_STATE_CHANGED);
        event.device = getDevice(address);
        event.valueInt1 = externalInputId;
        event.valueInt2 = gainSetting;
        event.valueInt3 = gainMode;
        event.valueInt4 = mute;

        Log.d(TAG, "onExtAudioInStateChanged: " + event);
        mVolumeControlService.messageFromNative(event);
    }

    @VisibleForTesting
    void onExtAudioInStatusChanged(int externalInputId, int status, byte[] address) {
        VolumeControlStackEvent event =
                new VolumeControlStackEvent(EVENT_TYPE_EXT_AUDIO_IN_STATUS_CHANGED);
        event.device = getDevice(address);
        event.valueInt1 = externalInputId;
        event.valueInt2 = status;

        Log.d(TAG, "onExtAudioInStatusChanged: " + event);
        mVolumeControlService.messageFromNative(event);
    }

    @VisibleForTesting
    void onExtAudioInTypeChanged(int externalInputId, int type, byte[] address) {
        VolumeControlStackEvent event =
                new VolumeControlStackEvent(EVENT_TYPE_EXT_AUDIO_IN_TYPE_CHANGED);
        event.device = getDevice(address);
        event.valueInt1 = externalInputId;
        event.valueInt2 = type;

        Log.d(TAG, "onExtAudioInTypeChanged: " + event);
        mVolumeControlService.messageFromNative(event);
    }

    @VisibleForTesting
    void onExtAudioInDescriptionChanged(int externalInputId, String descr, byte[] address) {
        VolumeControlStackEvent event =
                new VolumeControlStackEvent(EVENT_TYPE_EXT_AUDIO_IN_DESCR_CHANGED);
        event.device = getDevice(address);
        event.valueInt1 = externalInputId;
        event.valueString1 = descr;

        Log.d(TAG, "onExtAudioInDescriptionChanged: " + event);
        mVolumeControlService.messageFromNative(event);
    }

    @VisibleForTesting
    void onExtAudioInGainPropsChanged(
            int externalInputId, int unit, int min, int max, byte[] address) {
        VolumeControlStackEvent event =
                new VolumeControlStackEvent(EVENT_TYPE_EXT_AUDIO_IN_GAIN_PROPS_CHANGED);
        event.device = getDevice(address);
        event.valueInt1 = externalInputId;
        event.valueInt2 = unit;
        event.valueInt3 = min;
        event.valueInt4 = max;

        Log.d(TAG, "onExtAudioInGainPropsChanged: " + event);
        mVolumeControlService.messageFromNative(event);
    }
}
