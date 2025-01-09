/*
 * Copyright 2025 The Android Open Source Project
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

package com.android.bluetooth.gatt;

import static android.Manifest.permission.BLUETOOTH_CONNECT;
import static android.Manifest.permission.BLUETOOTH_PRIVILEGED;

import static com.android.bluetooth.Utils.callerIsSystemOrActiveOrManagedUser;

import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothStatusCodes;
import android.bluetooth.IDistanceMeasurement;
import android.bluetooth.le.ChannelSoundingParams;
import android.bluetooth.le.DistanceMeasurementMethod;
import android.bluetooth.le.DistanceMeasurementParams;
import android.bluetooth.le.IDistanceMeasurementCallback;
import android.content.AttributionSource;
import android.content.Context;
import android.os.ParcelUuid;

import com.android.bluetooth.Utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DistanceMeasurementBinder extends IDistanceMeasurement.Stub {
    private static final String TAG = DistanceMeasurementBinder.class.getSimpleName();
    private final Context mContext;
    private final DistanceMeasurementManager mDistanceMeasurementManager;
    private volatile boolean mIsAvailable = true;

    DistanceMeasurementBinder(Context context, DistanceMeasurementManager manager) {
        mContext = context;
        mDistanceMeasurementManager = manager;
    }

    void cleanup() {
        mIsAvailable = false;
    }

    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    @Nullable
    private DistanceMeasurementManager getManager(AttributionSource source, String method) {
        if (!mIsAvailable
                || !callerIsSystemOrActiveOrManagedUser(
                        mContext, TAG, "DistanceMeasurement " + method)
                || !Utils.checkConnectPermissionForDataDelivery(
                        mContext, source, "DistanceMeasurement " + method)) {
            return null;
        }
        mContext.enforceCallingOrSelfPermission(BLUETOOTH_PRIVILEGED, null);
        return mDistanceMeasurementManager;
    }

    @Override
    public List<DistanceMeasurementMethod> getSupportedDistanceMeasurementMethods(
            AttributionSource source) {
        DistanceMeasurementManager manager =
                getManager(source, "getSupportedDistanceMeasurementMethods");
        if (manager == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(manager.getSupportedDistanceMeasurementMethods());
    }

    @Override
    public void startDistanceMeasurement(
            ParcelUuid uuid,
            DistanceMeasurementParams distanceMeasurementParams,
            IDistanceMeasurementCallback callback,
            AttributionSource source) {
        DistanceMeasurementManager manager = getManager(source, "startDistanceMeasurement");
        if (manager == null) {
            return;
        }
        manager.startDistanceMeasurement(uuid.getUuid(), distanceMeasurementParams, callback);
    }

    @Override
    public int stopDistanceMeasurement(
            ParcelUuid uuid, BluetoothDevice device, int method, AttributionSource source) {
        if (!mIsAvailable) {
            return BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED;
        } else if (!callerIsSystemOrActiveOrManagedUser(mContext, TAG, "stopDistanceMeasurement")) {
            return BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ALLOWED;
        } else if (!Utils.checkConnectPermissionForDataDelivery(
                mContext, source, "DistanceMeasurement stopDistanceMeasurement")) {
            return BluetoothStatusCodes.ERROR_MISSING_BLUETOOTH_CONNECT_PERMISSION;
        }
        mContext.enforceCallingOrSelfPermission(BLUETOOTH_PRIVILEGED, null);
        return mDistanceMeasurementManager.stopDistanceMeasurement(
                uuid.getUuid(), device, method, false);
    }

    @Override
    public int getChannelSoundingMaxSupportedSecurityLevel(
            BluetoothDevice remoteDevice, AttributionSource source) {
        DistanceMeasurementManager manager =
                getManager(source, "getChannelSoundingMaxSupportedSecurityLevel");
        if (manager == null) {
            return ChannelSoundingParams.CS_SECURITY_LEVEL_UNKNOWN;
        }
        return manager.getChannelSoundingMaxSupportedSecurityLevel(remoteDevice);
    }

    @Override
    public int getLocalChannelSoundingMaxSupportedSecurityLevel(AttributionSource source) {
        DistanceMeasurementManager manager =
                getManager(source, "getLocalChannelSoundingMaxSupportedSecurityLevel");
        if (manager == null) {
            return ChannelSoundingParams.CS_SECURITY_LEVEL_UNKNOWN;
        }
        return manager.getLocalChannelSoundingMaxSupportedSecurityLevel();
    }

    @Override
    public int[] getChannelSoundingSupportedSecurityLevels(AttributionSource source) {
        DistanceMeasurementManager manager =
                getManager(source, "getChannelSoundingSupportedSecurityLevels");

        if (manager == null) {
            return new int[0];
        }
        return manager.getChannelSoundingSupportedSecurityLevels().stream()
                .mapToInt(i -> i)
                .toArray();
    }
}
