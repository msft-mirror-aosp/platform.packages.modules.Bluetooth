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

package com.android.bluetooth.vc;

import static com.android.bluetooth.Utils.RemoteExceptionIgnoringConsumer;

import static java.util.Objects.requireNonNull;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.IAudioInputCallback;
import android.os.RemoteCallbackList;
import android.util.Log;

import com.android.bluetooth.btservice.ProfileService;

import bluetooth.constants.AudioInputType;
import bluetooth.constants.aics.AudioInputStatus;
import bluetooth.constants.aics.GainMode;
import bluetooth.constants.aics.Mute;

class VolumeControlInputDescriptor {
    private static final String TAG = VolumeControlInputDescriptor.class.getSimpleName();

    final VolumeControlNativeInterface mNativeInterface;
    final BluetoothDevice mDevice;
    final Descriptor[] mVolumeInputs;

    VolumeControlInputDescriptor(
            VolumeControlNativeInterface nativeInterface,
            BluetoothDevice device,
            int numberOfExternalInputs) {
        mNativeInterface = requireNonNull(nativeInterface);
        mDevice = requireNonNull(device);
        mVolumeInputs = new Descriptor[numberOfExternalInputs];
        // Stack delivers us number of audio inputs. ids are countinous from [0;n[
        for (int i = 0; i < numberOfExternalInputs; i++) {
            mVolumeInputs[i] = new Descriptor();
        }
    }

    private static class Descriptor {
        int mStatus = AudioInputStatus.INACTIVE;

        int mType = AudioInputType.UNSPECIFIED;

        int mGainSetting = 0;

        int mGainMode = GainMode.MANUAL_ONLY;

        int mMute = Mute.DISABLED;

        /* See AICS 1.0
         * The Gain_Setting (mGainSetting) field is a signed value for which a single increment or
         * decrement should result in a corresponding increase or decrease of the input amplitude by
         * the value of the Gain_Setting_Units (mGainSettingsUnits) field of the Gain Setting
         * Properties characteristic value.
         */
        int mGainSettingsUnits = 0;

        int mGainSettingsMax = 0;
        int mGainSettingsMin = 0;

        String mDescription = "";
        boolean mDescriptionIsWritable = false;

        private final RemoteCallbackList<IAudioInputCallback> mCallbacks =
                new RemoteCallbackList<>();

        void registerCallback(IAudioInputCallback callback) {
            mCallbacks.register(callback);
        }

        void unregisterCallback(IAudioInputCallback callback) {
            mCallbacks.unregister(callback);
        }

        synchronized void broadcast(
                String logAction, RemoteExceptionIgnoringConsumer<IAudioInputCallback> action) {
            final int itemCount = mCallbacks.beginBroadcast();
            Log.d(TAG, "Broadcasting " + logAction + "() to " + itemCount + " receivers.");
            for (int i = 0; i < itemCount; i++) {
                action.accept(mCallbacks.getBroadcastItem(i));
            }
            mCallbacks.finishBroadcast();
        }
    }

    int size() {
        return mVolumeInputs.length;
    }

    private boolean isValidId(int id) {
        if (id >= size() || id < 0) {
            Log.e(TAG, "Request fail. Illegal id argument: " + id);
            return false;
        }
        return true;
    }

    void registerCallback(int id, IAudioInputCallback callback) {
        if (!isValidId(id)) return;
        mVolumeInputs[id].registerCallback(callback);
    }

    void unregisterCallback(int id, IAudioInputCallback callback) {
        if (!isValidId(id)) return;
        mVolumeInputs[id].unregisterCallback(callback);
    }

    void setStatus(int id, int status) {
        if (!isValidId(id)) return;
        mVolumeInputs[id].mStatus = status;
    }

    int getStatus(int id) {
        if (!isValidId(id)) return AudioInputStatus.INACTIVE;
        return mVolumeInputs[id].mStatus;
    }

    boolean isDescriptionWritable(int id) {
        if (!isValidId(id)) return false;
        return mVolumeInputs[id].mDescriptionIsWritable;
    }

    boolean setDescription(int id, String description) {
        if (!isValidId(id)) return false;

        if (!mVolumeInputs[id].mDescriptionIsWritable) {
            throw new IllegalStateException("Description is not writable");
        }

        return mNativeInterface.setExtAudioInDescription(mDevice, id, description);
    }

    String getDescription(int id) {
        if (!isValidId(id)) return null;
        return mVolumeInputs[id].mDescription;
    }

    void onDescriptionChanged(int id, String description, boolean isWritable) {
        if (!isValidId(id)) return;
        Descriptor desc = mVolumeInputs[id];

        desc.mDescription = description;
        desc.mDescriptionIsWritable = isWritable;
        desc.broadcast("onDescriptionChanged", c -> c.onDescriptionChanged(description));
    }

    void setType(int id, int type) {
        if (!isValidId(id)) return;
        mVolumeInputs[id].mType = type;
    }

    int getType(int id) {
        if (!isValidId(id)) return AudioInputType.UNSPECIFIED;
        return mVolumeInputs[id].mType;
    }

    int getGainSetting(int id) {
        if (!isValidId(id)) return 0;
        return mVolumeInputs[id].mGainSetting;
    }

    int getMute(int id) {
        if (!isValidId(id)) return Mute.DISABLED;
        return mVolumeInputs[id].mMute;
    }

    void setPropSettings(int id, int gainUnit, int gainMin, int gainMax) {
        if (!isValidId(id)) return;

        mVolumeInputs[id].mGainSettingsUnits = gainUnit;
        mVolumeInputs[id].mGainSettingsMin = gainMin;
        mVolumeInputs[id].mGainSettingsMax = gainMax;
    }

    void setState(int id, int gainSetting, int mute, int gainMode) {
        if (!isValidId(id)) return;

        Descriptor desc = mVolumeInputs[id];

        if (gainSetting > desc.mGainSettingsMax || gainSetting < desc.mGainSettingsMin) {
            Log.e(TAG, "Request fail. Illegal gainSetting argument: " + gainSetting);
            return;
        }

        desc.mGainSetting = gainSetting;
        desc.mGainMode = gainMode;
        desc.mMute = mute;
    }

    void dump(StringBuilder sb) {
        for (int i = 0; i < mVolumeInputs.length; i++) {
            Descriptor desc = mVolumeInputs[i];
            ProfileService.println(sb, "      id: " + i);
            ProfileService.println(sb, "        description: " + desc.mDescription);
            ProfileService.println(sb, "        type: " + desc.mType);
            ProfileService.println(sb, "        status: " + desc.mStatus);
            ProfileService.println(sb, "        gainSetting: " + desc.mGainSetting);
            ProfileService.println(sb, "        gainMode: " + desc.mGainMode);
            ProfileService.println(sb, "        mute: " + desc.mMute);
            ProfileService.println(sb, "        units:" + desc.mGainSettingsUnits);
            ProfileService.println(sb, "        minGain:" + desc.mGainSettingsMin);
            ProfileService.println(sb, "        maxGain:" + desc.mGainSettingsMax);
        }
    }
}
