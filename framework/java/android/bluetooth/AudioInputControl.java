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

import android.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This class provides APIs to control a remote AICS(Audio Input Control Service)
 *
 * @see BluetoothVolumeControl#getAudioInputControlPoints
 * @hide
 */
public final class AudioInputControl {
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
}
