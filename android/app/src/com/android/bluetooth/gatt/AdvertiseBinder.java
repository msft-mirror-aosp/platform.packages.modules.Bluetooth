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

import static android.Manifest.permission.BLUETOOTH_ADVERTISE;
import static android.Manifest.permission.BLUETOOTH_PRIVILEGED;

import static java.util.Objects.requireNonNull;

import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.bluetooth.IBluetoothAdvertise;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertisingSetParameters;
import android.bluetooth.le.IAdvertisingSetCallback;
import android.bluetooth.le.PeriodicAdvertisingParameters;
import android.content.AttributionSource;
import android.content.Context;

import com.android.bluetooth.Utils;

class AdvertiseBinder extends IBluetoothAdvertise.Stub {
    private final Context mContext;
    private final AdvertiseManager mAdvertiseManager;
    private volatile boolean mIsAvailable = true;

    AdvertiseBinder(Context context, AdvertiseManager manager) {
        mContext = context;
        mAdvertiseManager = manager;
    }

    void cleanup() {
        mIsAvailable = false;
    }

    @RequiresPermission(BLUETOOTH_ADVERTISE)
    @Nullable
    private AdvertiseManager getManager(AttributionSource source) {
        requireNonNull(source);
        if (!Utils.checkAdvertisePermissionForDataDelivery(
                mContext, source, "AdvertiseManager startAdvertisingSet")) {
            return null;
        }
        return mIsAvailable ? mAdvertiseManager : null;
    }

    @Override
    public void startAdvertisingSet(
            AdvertisingSetParameters parameters,
            @Nullable AdvertiseData advertiseData,
            @Nullable AdvertiseData scanResponse,
            @Nullable PeriodicAdvertisingParameters periodicParameters,
            @Nullable AdvertiseData periodicData,
            int duration,
            int maxExtAdvEvents,
            int serverIf,
            IAdvertisingSetCallback callback,
            AttributionSource source) {
        requireNonNull(parameters);
        requireNonNull(callback);
        AdvertiseManager manager = getManager(source);
        if (manager == null) {
            return;
        }
        if (parameters.getOwnAddressType() != AdvertisingSetParameters.ADDRESS_TYPE_DEFAULT
                || serverIf != 0
                || parameters.isDirected()) {
            mContext.enforceCallingOrSelfPermission(BLUETOOTH_PRIVILEGED, null);
        }
        manager.startAdvertisingSet(
                parameters,
                advertiseData,
                scanResponse,
                periodicParameters,
                periodicData,
                duration,
                maxExtAdvEvents,
                serverIf,
                callback,
                source);
    }

    @Override
    public void stopAdvertisingSet(IAdvertisingSetCallback callback, AttributionSource source) {
        requireNonNull(callback);
        AdvertiseManager manager = getManager(source);
        if (manager == null) {
            return;
        }
        manager.stopAdvertisingSet(callback);
    }

    @Override
    public void getOwnAddress(int advertiserId, AttributionSource source) {
        AdvertiseManager manager = getManager(source);
        if (manager == null) {
            return;
        }
        mContext.enforceCallingOrSelfPermission(BLUETOOTH_PRIVILEGED, null);
        manager.getOwnAddress(advertiserId);
    }

    @Override
    public void enableAdvertisingSet(
            int advertiserId,
            boolean enable,
            int duration,
            int maxExtAdvEvents,
            AttributionSource source) {
        AdvertiseManager manager = getManager(source);
        if (manager == null) {
            return;
        }
        manager.enableAdvertisingSet(advertiserId, enable, duration, maxExtAdvEvents);
    }

    @Override
    public void setAdvertisingData(
            int advertiserId, @Nullable AdvertiseData data, AttributionSource source) {
        AdvertiseManager manager = getManager(source);
        if (manager == null) {
            return;
        }
        manager.setAdvertisingData(advertiserId, data);
    }

    @Override
    public void setScanResponseData(
            int advertiserId, @Nullable AdvertiseData data, AttributionSource source) {
        AdvertiseManager manager = getManager(source);
        if (manager == null) {
            return;
        }
        manager.setScanResponseData(advertiserId, data);
    }

    @Override
    public void setAdvertisingParameters(
            int advertiserId, AdvertisingSetParameters parameters, AttributionSource source) {
        requireNonNull(parameters);
        AdvertiseManager manager = getManager(source);
        if (manager == null) {
            return;
        }
        if (parameters.getOwnAddressType() != AdvertisingSetParameters.ADDRESS_TYPE_DEFAULT
                || parameters.isDirected()) {
            mContext.enforceCallingOrSelfPermission(BLUETOOTH_PRIVILEGED, null);
        }
        manager.setAdvertisingParameters(advertiserId, parameters);
    }

    @Override
    public void setPeriodicAdvertisingParameters(
            int advertiserId,
            @Nullable PeriodicAdvertisingParameters parameters,
            AttributionSource source) {
        AdvertiseManager manager = getManager(source);
        if (manager == null) {
            return;
        }
        manager.setPeriodicAdvertisingParameters(advertiserId, parameters);
    }

    @Override
    public void setPeriodicAdvertisingData(
            int advertiserId, @Nullable AdvertiseData data, AttributionSource source) {
        AdvertiseManager manager = getManager(source);
        if (manager == null) {
            return;
        }
        manager.setPeriodicAdvertisingData(advertiserId, data);
    }

    @Override
    public void setPeriodicAdvertisingEnable(
            int advertiserId, boolean enable, AttributionSource source) {
        AdvertiseManager manager = getManager(source);
        if (manager == null) {
            return;
        }
        manager.setPeriodicAdvertisingEnable(advertiserId, enable);
    }
}
