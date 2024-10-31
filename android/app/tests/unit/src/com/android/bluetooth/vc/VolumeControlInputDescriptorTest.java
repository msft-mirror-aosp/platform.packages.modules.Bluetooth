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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.*;

import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class VolumeControlInputDescriptorTest {
    private static final int NUMBER_OF_INPUT = 3;
    private static final int NUMBER_OF_FIELD_IN_STRUCT = 9;
    private static final int VALID_ID = 1;
    private static final int INVALID_ID = NUMBER_OF_INPUT;
    private static final int INVALID_ID2 = -1;

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private VolumeControlInputDescriptor mDescriptor;

    @Before
    public void setUp() {
        mDescriptor = new VolumeControlInputDescriptor(NUMBER_OF_INPUT);
    }

    @Test
    public void dump_noCrash() {
        StringBuilder sb = new StringBuilder();

        mDescriptor.dump(sb);
        int nLines = NUMBER_OF_INPUT * (NUMBER_OF_FIELD_IN_STRUCT + 1); // +1 for the id
        assertThat(sb.toString()).containsMatch("(        .*\n){" + nLines + "}");
    }

    @Test
    public void setFoo_withAllValidId_valuesAreUpdated() {
        for (int i = 0; i < NUMBER_OF_INPUT; i++) {
            assertThat(mDescriptor.getStatus(i)).isEqualTo(0); // AudioInputStatus.INACTIVE);
            mDescriptor.setStatus(i, 1); // AudioInputStatus.ACTIVE);
            assertThat(mDescriptor.getStatus(i)).isEqualTo(1); // AudioInputStatus.ACTIVE);
        }
    }

    @Test
    public void getStatus_whenNeverSet_defaultToInactive() {
        assertThat(mDescriptor.getStatus(VALID_ID)).isEqualTo(0); // AudioInputStatus.INACTIVE);
    }

    @Test
    public void setStatus_withValidId_valueIsUpdated() {
        int newStatus = 1; // AudioInputStatus.ACTIVE;
        mDescriptor.setStatus(VALID_ID, newStatus);

        assertThat(mDescriptor.getStatus(VALID_ID)).isEqualTo(newStatus);
    }

    @Test
    public void setStatus_withInvalidId_valueIsNotUpdated() {
        int newStatus = 1; // AudioInputStatus.ACTIVE;
        mDescriptor.setStatus(INVALID_ID, newStatus);

        assertThat(mDescriptor.getStatus(INVALID_ID)).isNotEqualTo(newStatus);
    }

    @Test
    public void getType_whenNeverSet_defaultToUnspecified() {
        assertThat(mDescriptor.getType(VALID_ID)).isEqualTo(0); // AudioInputType.UNSPECIFIED);
    }

    @Test
    public void setType_withValidId_valueIsUpdated() {
        int newType = 7; // AudioInputType.AMBIENT;
        mDescriptor.setType(VALID_ID, newType);

        assertThat(mDescriptor.getType(VALID_ID)).isEqualTo(newType);
    }

    @Test
    public void setType_withInvalidId_valueIsNotUpdated() {
        int newType = 1; // AudioInputType.BLUETOOTH;
        mDescriptor.setType(INVALID_ID2, newType);

        assertThat(mDescriptor.getType(INVALID_ID2)).isNotEqualTo(newType);
    }

    @Test
    public void setState_withValidIdButIncorrectSettings_valueIsNotUpdated() {
        int newGainValue = 42;
        int newGainMode = 42;
        int mute = 0; // NOT_MUTED
        mDescriptor.setState(VALID_ID, newGainMode, newGainMode, mute);

        assertThat(mDescriptor.getGain(VALID_ID)).isNotEqualTo(newGainValue);
        // assertThat(mDescriptor.getGainMode(VALID_ID)).isNotEqualTo(newGainMode);
        assertThat(mDescriptor.getMute(VALID_ID)).isNotEqualTo(mute);
    }

    @Test
    public void setState_withValidIdAndCorrectSettings_valueIsUpdated() {
        int newMax = 100;
        int newMin = 0;
        int newUnit = 1;
        mDescriptor.setPropSettings(VALID_ID, newUnit, newMin, newMax);

        int newGainValue = 42;
        int newGainMode = 42;
        int mute = 1; // MUTED
        mDescriptor.setState(VALID_ID, newGainMode, newGainMode, mute);

        assertThat(mDescriptor.getGain(VALID_ID)).isEqualTo(newGainValue);
        // assertThat(mDescriptor.getGainMode(VALID_ID)).isNotEqualTo(newGainMode);
        assertThat(mDescriptor.getMute(VALID_ID)).isEqualTo(1); // MUTED
    }

    @Test
    public void setState_withInvalidId_valueIsNotUpdated() {
        int newMax = 100;
        int newMin = 0;
        int newUnit = 1;
        // Should be no-op but we want to copy the working case test, just with an invalid id
        mDescriptor.setPropSettings(INVALID_ID, newUnit, newMin, newMax);

        int newGainValue = 42;
        int newGainMode = 42;
        int mute = 1;
        mDescriptor.setState(INVALID_ID, newGainMode, newGainMode, mute);

        assertThat(mDescriptor.getGain(INVALID_ID)).isNotEqualTo(newGainValue);
        // assertThat(mDescriptor.getGainMode(VALID_ID)).isNotEqualTo(newGainMode);
        assertThat(mDescriptor.getMute(INVALID_ID)).isEqualTo(2); // DISABLED
    }

    @Test
    public void getDescription_whenNeverSet_defaultToEmptyString() {
        assertThat(mDescriptor.getDescription(VALID_ID)).isEmpty();
    }

    @Test
    public void setDescription_withValidId_valueIsUpdated() {
        String newDescription = "what a nice description";
        mDescriptor.setDescription(VALID_ID, newDescription);

        assertThat(mDescriptor.getDescription(VALID_ID)).isEqualTo(newDescription);
    }

    @Test
    public void setDescription_withInvalidId_valueIsNotUpdated() {
        String newDescription = "what a nice description";
        mDescriptor.setDescription(INVALID_ID, newDescription);

        assertThat(mDescriptor.getDescription(INVALID_ID)).isNotEqualTo(newDescription);
    }
}
