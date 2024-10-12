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

import android.util.Log;

import com.android.bluetooth.btservice.ProfileService;

class VolumeControlInputDescriptor {
    private static final String TAG = VolumeControlInputDescriptor.class.getSimpleName();

    final Descriptor[] mVolumeInputs;

    VolumeControlInputDescriptor(int numberOfExternalInputs) {
        mVolumeInputs = new Descriptor[numberOfExternalInputs];
        // Stack delivers us number of audio inputs. ids are countinous from [0;n[
        for (int i = 0; i < numberOfExternalInputs; i++) {
            mVolumeInputs[i] = new Descriptor();
        }
    }

    private static class Descriptor {
        int mStatus = 0; // AudioInputStatus.INACTIVE;

        int mType = 0; // AudioInputType.UNSPECIFIED;

        int mGainValue = 0;

        /* See AICS 1.0 - 3.1.3. Gain_Mode field
         * The Gain_Mode field shall be set to a value that reflects whether gain modes are manual
         * or automatic.
         * - Manual Only, the server allows only manual gain.
         * - Automatic Only, the server allows only automatic gain.
         *
         * For all other Gain_Mode field values, the server allows switchable automatic/manual gain.
         */
        int mGainMode = 0;

        boolean mIsMute = false;

        /* See AICS 1.0
         * The Gain_Setting (mGainValue) field is a signed value for which a single increment or
         * decrement should result in a corresponding increase or decrease of the input amplitude by
         * the value of the Gain_Setting_Units (mGainSettingsUnits) field of the Gain Setting
         * Properties characteristic value.
         */
        int mGainSettingsUnits = 0;

        int mGainSettingsMaxSetting = 0;
        int mGainSettingsMinSetting = 0;

        String mDescription = "";
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

    void setStatus(int id, int status) {
        if (!isValidId(id)) return;
        mVolumeInputs[id].mStatus = status;
    }

    int getStatus(int id) {
        if (!isValidId(id)) return 0; // AudioInputStatus.INACTIVE;
        return mVolumeInputs[id].mStatus;
    }

    void setDescription(int id, String description) {
        if (!isValidId(id)) return;
        mVolumeInputs[id].mDescription = description;
    }

    String getDescription(int id) {
        if (!isValidId(id)) return null;
        return mVolumeInputs[id].mDescription;
    }

    void setType(int id, int type) {
        if (!isValidId(id)) return;
        mVolumeInputs[id].mType = type;
    }

    int getType(int id) {
        if (!isValidId(id)) return 0; // AudioInputType.UNSPECIFIED;
        return mVolumeInputs[id].mType;
    }

    int getGain(int id) {
        if (!isValidId(id)) return 0;
        return mVolumeInputs[id].mGainValue;
    }

    boolean isMuted(int id) {
        if (!isValidId(id)) return false;
        return mVolumeInputs[id].mIsMute;
    }

    void setPropSettings(int id, int gainUnit, int gainMin, int gainMax) {
        if (!isValidId(id)) return;

        mVolumeInputs[id].mGainSettingsUnits = gainUnit;
        mVolumeInputs[id].mGainSettingsMinSetting = gainMin;
        mVolumeInputs[id].mGainSettingsMaxSetting = gainMax;
    }

    void setState(int id, int gainValue, int gainMode, boolean mute) {
        if (!isValidId(id)) return;

        Descriptor desc = mVolumeInputs[id];

        if (gainValue > desc.mGainSettingsMaxSetting || gainValue < desc.mGainSettingsMinSetting) {
            Log.e(TAG, "Request fail. Illegal gainValue argument: " + gainValue);
            return;
        }

        desc.mGainValue = gainValue;
        desc.mGainMode = gainMode;
        desc.mIsMute = mute;
    }

    void dump(StringBuilder sb) {
        for (int i = 0; i < mVolumeInputs.length; i++) {
            Descriptor desc = mVolumeInputs[i];
            ProfileService.println(sb, "      id: " + i);
            ProfileService.println(sb, "        description: " + desc.mDescription);
            ProfileService.println(sb, "        type: " + desc.mType);
            ProfileService.println(sb, "        status: " + desc.mStatus);
            ProfileService.println(sb, "        gainValue: " + desc.mGainValue);
            ProfileService.println(sb, "        gainMode: " + desc.mGainMode);
            ProfileService.println(sb, "        mute: " + desc.mIsMute);
            ProfileService.println(sb, "        units:" + desc.mGainSettingsUnits);
            ProfileService.println(sb, "        minGain:" + desc.mGainSettingsMinSetting);
            ProfileService.println(sb, "        maxGain:" + desc.mGainSettingsMaxSetting);
        }
    }
}
