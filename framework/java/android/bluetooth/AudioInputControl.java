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

package android.bluetooth;

import static android.Manifest.permission.BLUETOOTH_CONNECT;
import static android.Manifest.permission.BLUETOOTH_PRIVILEGED;
import static android.bluetooth.BluetoothUtils.callService;
import static android.bluetooth.BluetoothUtils.logRemoteException;

import static java.util.Objects.requireNonNull;

import android.annotation.CallbackExecutor;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.RequiresNoPermission;
import android.annotation.RequiresPermission;
import android.bluetooth.annotations.RequiresBluetoothConnectPermission;
import android.content.AttributionSource;
import android.os.RemoteException;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class provides APIs to control a remote AICS(Audio Input Control Service)
 *
 * @see BluetoothVolumeControl#getAudioInputControlPoints
 * @hide
 */
public final class AudioInputControl {
    private static final String TAG = AudioInputControl.class.getSimpleName();

    /** Unspecified Input */
    public static final int AUDIO_INPUT_TYPE_UNSPECIFIED =
            bluetooth.constants.AudioInputType.UNSPECIFIED;

    /** Bluetooth Audio Stream */
    public static final int AUDIO_INPUT_TYPE_BLUETOOTH =
            bluetooth.constants.AudioInputType.BLUETOOTH;

    /** Microphone */
    public static final int AUDIO_INPUT_TYPE_MICROPHONE =
            bluetooth.constants.AudioInputType.MICROPHONE;

    /** Analog Interface */
    public static final int AUDIO_INPUT_TYPE_ANALOG = bluetooth.constants.AudioInputType.ANALOG;

    /** Digital Interface */
    public static final int AUDIO_INPUT_TYPE_DIGITAL = bluetooth.constants.AudioInputType.DIGITAL;

    /** AM/FM/XM/etc. */
    public static final int AUDIO_INPUT_TYPE_RADIO = bluetooth.constants.AudioInputType.RADIO;

    /** Streaming Audio Source */
    public static final int AUDIO_INPUT_TYPE_STREAMING =
            bluetooth.constants.AudioInputType.STREAMING;

    /** Transparency/Pass-through */
    public static final int AUDIO_INPUT_TYPE_AMBIENT = bluetooth.constants.AudioInputType.AMBIENT;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            prefix = {"AUDIO_INPUT_TYPE_"},
            value = {
                AUDIO_INPUT_TYPE_UNSPECIFIED,
                AUDIO_INPUT_TYPE_BLUETOOTH,
                AUDIO_INPUT_TYPE_MICROPHONE,
                AUDIO_INPUT_TYPE_ANALOG,
                AUDIO_INPUT_TYPE_DIGITAL,
                AUDIO_INPUT_TYPE_RADIO,
                AUDIO_INPUT_TYPE_STREAMING,
                AUDIO_INPUT_TYPE_AMBIENT,
            })
    public @interface AudioInputType {}

    /** Inactive */
    public static final int AUDIO_INPUT_STATUS_INACTIVE =
            bluetooth.constants.aics.AudioInputStatus.INACTIVE;

    /** Active */
    public static final int AUDIO_INPUT_STATUS_ACTIVE =
            bluetooth.constants.aics.AudioInputStatus.ACTIVE;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            prefix = {"AUDIO_INPUT_STATUS_"},
            value = {
                AUDIO_INPUT_STATUS_INACTIVE,
                AUDIO_INPUT_STATUS_ACTIVE,
            })
    public @interface AudioInputStatus {}

    /** Not Muted */
    public static final int MUTE_NOT_MUTED = bluetooth.constants.aics.Mute.NOT_MUTED;

    /** Muted */
    public static final int MUTE_MUTED = bluetooth.constants.aics.Mute.MUTED;

    /** Disabled */
    public static final int MUTE_DISABLED = bluetooth.constants.aics.Mute.DISABLED;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            prefix = {"MUTE_"},
            value = {
                MUTE_NOT_MUTED,
                MUTE_MUTED,
                MUTE_DISABLED,
            })
    public @interface Mute {}

    /** Manual Only */
    public static final int GAIN_MODE_MANUAL_ONLY = bluetooth.constants.aics.GainMode.MANUAL_ONLY;

    /** Automatic Only */
    public static final int GAIN_MODE_AUTOMATIC_ONLY =
            bluetooth.constants.aics.GainMode.AUTOMATIC_ONLY;

    /** Manual */
    public static final int GAIN_MODE_MANUAL = bluetooth.constants.aics.GainMode.MANUAL;

    /** Automatic */
    public static final int GAIN_MODE_AUTOMATIC = bluetooth.constants.aics.GainMode.AUTOMATIC;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            prefix = {"GAIN_MODE_"},
            value = {
                GAIN_MODE_MANUAL_ONLY,
                GAIN_MODE_AUTOMATIC_ONLY,
                GAIN_MODE_MANUAL,
                GAIN_MODE_AUTOMATIC,
            })
    public @interface GainMode {}

    private final IBluetoothVolumeControl mService;
    private final @NonNull BluetoothDevice mDevice;
    private final int mInstanceId;
    private final AttributionSource mAttributionSource;
    private final CallbackWrapper<AudioInputCallback, IBluetoothVolumeControl> mCallbackWrapper;

    /** @hide */
    public AudioInputControl(
            @NonNull BluetoothDevice device,
            int id,
            @NonNull IBluetoothVolumeControl service,
            @NonNull AttributionSource source) {
        mDevice = requireNonNull(device);
        mInstanceId = id;
        mService = requireNonNull(service);
        mAttributionSource = requireNonNull(source);
        mCallbackWrapper =
                new CallbackWrapper<AudioInputCallback, IBluetoothVolumeControl>(
                        this::registerCallbackFn, this::unregisterCallbackFn);
    }

    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    private void registerCallbackFn(IBluetoothVolumeControl vcs) {
        try {
            vcs.registerAudioInputControlCallback(
                    mAttributionSource, mDevice, mInstanceId, mCallback);
        } catch (RemoteException e) {
            logRemoteException(TAG, e);
        }
    }

    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    private void unregisterCallbackFn(IBluetoothVolumeControl vcs) {
        try {
            vcs.unregisterAudioInputControlCallback(
                    mAttributionSource, mDevice, mInstanceId, mCallback);
        } catch (RemoteException e) {
            logRemoteException(TAG, e);
        }
    }

    private final IAudioInputCallback mCallback =
            new IAudioInputCallback.Stub() {
                @Override
                @RequiresNoPermission
                public void onDescriptionChanged(String description) {
                    mCallbackWrapper.forEach(cb -> cb.onDescriptionChanged(description));
                }

                @Override
                @RequiresNoPermission
                public void onStatusChanged(int status) {
                    mCallbackWrapper.forEach(cb -> cb.onStatusChanged(status));
                }

                @Override
                @RequiresNoPermission
                public void onStateChanged(int gainSetting, int mute, int gainMode) {
                    mCallbackWrapper.forEach(cb -> cb.onGainSettingChanged(gainSetting));
                    mCallbackWrapper.forEach(cb -> cb.onMuteChanged(mute));
                    mCallbackWrapper.forEach(cb -> cb.onGainModeChanged(gainMode));
                }

                @Override
                @RequiresNoPermission
                public void onSetGainSettingFailed() {
                    mCallbackWrapper.forEach(cb -> cb.onSetGainSettingFailed());
                }

                @Override
                @RequiresNoPermission
                public void onSetGainModeFailed() {
                    mCallbackWrapper.forEach(cb -> cb.onSetGainModeFailed());
                }

                @Override
                @RequiresNoPermission
                public void onSetMuteFailed() {
                    mCallbackWrapper.forEach(cb -> cb.onSetMuteFailed());
                }
            };

    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    static List<AudioInputControl> getAudioInputControlServices(
            @NonNull IBluetoothVolumeControl service,
            @NonNull AttributionSource source,
            @NonNull BluetoothDevice device) {
        requireNonNull(service);
        requireNonNull(source);
        requireNonNull(device);
        int numberOfAics = 0;
        try {
            numberOfAics = service.getNumberOfAudioInputControlServices(source, device);
        } catch (RemoteException e) {
            logRemoteException(TAG, e);
        }
        return IntStream.range(0, numberOfAics)
                .mapToObj(i -> new AudioInputControl(device, i, service, source))
                .collect(Collectors.toList());
    }

    /**
     * This class provides a callback that is invoked when value changes on the remote device.
     *
     * @hide
     */
    public interface AudioInputCallback {
        /** @hide */
        default void onDescriptionChanged(@NonNull String description) {}

        /** @hide */
        default void onStatusChanged(@AudioInputStatus int status) {}

        /** @hide */
        default void onGainModeChanged(@GainMode int gainMode) {}

        /** @hide */
        default void onMuteChanged(@Mute int mute) {}

        /** @hide */
        default void onGainSettingChanged(int gainSetting) {}

        /** @hide */
        default void onSetGainSettingFailed() {}

        /** @hide */
        default void onSetGainModeFailed() {}

        /** @hide */
        default void onSetMuteFailed() {}
    }

    /**
     * Register a {@link AudioInputCallback}
     *
     * <p>Repeated registration of the same <var>callback</var> object will have no effect after the
     * first call to this method, even when the <var>executor</var> is different. API caller would
     * have to call {@link #unregisterCallback(Callback)} with the same callback object before
     * registering it again.
     *
     * @param executor an {@link Executor} to execute given callback
     * @param callback user implementation of the {@link AudioInputCallback}
     * @throws IllegalArgumentException if callback is already registered
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public void registerCallback(
            @NonNull @CallbackExecutor Executor executor, @NonNull AudioInputCallback callback) {
        mCallbackWrapper.registerCallback(mService, callback, executor);
    }

    /**
     * Unregister the specified {@link AudioInputCallback}.
     *
     * <p>The same {@link AudioInputCallback} object used when calling {@link
     * #registerCallback(Executor, AudioInputCallback)} must be used.
     *
     * <p>Callbacks are automatically unregistered when application process goes away
     *
     * @param callback user implementation of the {@link AudioInputCallback}
     * @throws IllegalArgumentException when no callback is registered
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public void unregisterCallback(@NonNull AudioInputCallback callback) {
        mCallbackWrapper.unregisterCallback(mService, callback);
    }

    /**
     * @return The Audio Input Type as defined in Audio Input Control Service 1.0 - 3.3.
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public @AudioInputType int getType() {
        return callService(
                mService,
                s -> s.getAudioInputType(mAttributionSource, mDevice, mInstanceId),
                bluetooth.constants.AudioInputType.UNSPECIFIED);
    }

    /**
     * @return The Gain Setting Units as defined in Audio Input Control Service 1.0 - 3.2.1
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public int getGainSettingUnit() {
        return callService(
                mService,
                s -> s.getAudioInputGainSettingUnit(mAttributionSource, mDevice, mInstanceId),
                0);
    }

    /**
     * @return The Gain Setting Units as defined in Audio Input Control Service 1.0 - 3.2.1
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public int getGainSettingMin() {
        return callService(
                mService,
                s -> s.getAudioInputGainSettingMin(mAttributionSource, mDevice, mInstanceId),
                0);
    }

    /**
     * @return The Gain Setting Units as defined in Audio Input Control Service 1.0 - 3.2.1
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public int getGainSettingMax() {
        return callService(
                mService,
                s -> s.getAudioInputGainSettingMax(mAttributionSource, mDevice, mInstanceId),
                0);
    }

    /**
     * @return The Gain Setting Units as defined in Audio Input Control Service 1.0 - 3.2.1
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public @NonNull String getDescription() {
        return callService(
                mService,
                s -> s.getAudioInputDescription(mAttributionSource, mDevice, mInstanceId),
                "");
    }

    /**
     * @return The Gain Setting Units as defined in Audio Input Control Service 1.0 - 3.2.1
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public boolean isDescriptionWritable() {
        return callService(
                mService,
                s -> s.isAudioInputDescriptionWritable(mAttributionSource, mDevice, mInstanceId),
                false);
    }

    /**
     * @return The Gain Setting Units as defined in Audio Input Control Service 1.0 - 3.2.1
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public boolean setDescription(@NonNull String description) {
        return callService(
                mService,
                s ->
                        s.setAudioInputDescription(
                                mAttributionSource, mDevice, mInstanceId, description),
                false);
    }

    /**
     * @return The Audio Input Status as defined in Audio Input Control Service 1.0 - 3.4.
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public @AudioInputStatus int getStatus() {
        return callService(
                mService,
                s -> s.getAudioInputStatus(mAttributionSource, mDevice, mInstanceId),
                (int) bluetooth.constants.aics.AudioInputStatus.INACTIVE);
    }

    /**
     * @return The Audio Input Status as defined in Audio Input Control Service 1.0 - 3.4.
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public int getGainSetting() {
        return callService(
                mService,
                s -> s.getAudioInputGainSetting(mAttributionSource, mDevice, mInstanceId),
                0);
    }

    /**
     * @return The Audio Input Status as defined in Audio Input Control Service 1.0 - 3.4.
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public boolean setGainSetting(int gainSetting) {
        return callService(
                mService,
                s ->
                        s.setAudioInputGainSetting(
                                mAttributionSource, mDevice, mInstanceId, gainSetting),
                false);
    }

    /**
     * @return The Audio Input Status as defined in Audio Input Control Service 1.0 - 3.4.
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public @GainMode int getGainMode() {
        return callService(
                mService,
                s -> s.getAudioInputGainMode(mAttributionSource, mDevice, mInstanceId),
                (int) bluetooth.constants.aics.GainMode.AUTOMATIC_ONLY);
    }

    /**
     * @return The Audio Input Status as defined in Audio Input Control Service 1.0 - 3.4.
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public boolean setGainMode(@GainMode int gainMode) {
        if (gainMode < GAIN_MODE_MANUAL_ONLY || gainMode > GAIN_MODE_AUTOMATIC) {
            throw new IllegalArgumentException("Illegal GainMode value: " + gainMode);
        }
        return callService(
                mService,
                s -> s.setAudioInputGainMode(mAttributionSource, mDevice, mInstanceId, gainMode),
                false);
    }

    /**
     * @return The Audio Input Status as defined in Audio Input Control Service 1.0 - 3.4.
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public @Mute int getMute() {
        return callService(
                mService,
                s -> s.getAudioInputMute(mAttributionSource, mDevice, mInstanceId),
                (int) bluetooth.constants.aics.Mute.DISABLED);
    }

    /**
     * @return The Audio Input Status as defined in Audio Input Control Service 1.0 - 3.4.
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public boolean setMute(@Mute int mute) {
        if (mute < MUTE_NOT_MUTED || mute > MUTE_MUTED) {
            throw new IllegalArgumentException("Illegal mute value: " + mute);
        }
        return callService(
                mService,
                s -> s.setAudioInputMute(mAttributionSource, mDevice, mInstanceId, mute),
                false);
    }
}
