/*
 * Copyright 2019 The Android Open Source Project
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

package com.android.bluetooth.avrcpcontroller;

import static com.google.common.truth.Truth.assertThat;

import android.annotation.SuppressLint;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/** A test suite for the BipImageDescriptor class */
@RunWith(AndroidJUnit4.class)
public class BipImageDescriptorTest {

    private static final String sXmlDocDecl =
            "<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>\r\n";

    @Test
    public void testBuildImageDescriptor_encodingConstants() {
        String expected =
                sXmlDocDecl
                        + "<image-descriptor version=\"1.0\">\r\n"
                        + "  <image encoding=\"JPEG\" pixel=\"1280*960\" size=\"500000\" />\r\n"
                        + "</image-descriptor>";

        BipImageDescriptor.Builder builder = new BipImageDescriptor.Builder();
        builder.setEncoding(BipEncoding.JPEG);
        builder.setFixedDimensions(1280, 960);
        builder.setFileSize(500000);

        BipImageDescriptor descriptor = builder.build();
        assertThat(descriptor.toString()).isEqualTo(expected);
    }

    @Test
    public void testBuildImageDescriptor_encodingObject() {
        String expected =
                sXmlDocDecl
                        + "<image-descriptor version=\"1.0\">\r\n"
                        + "  <image encoding=\"JPEG\" pixel=\"1280*960\" size=\"500000\" />\r\n"
                        + "</image-descriptor>";

        BipEncoding encoding = new BipEncoding(BipEncoding.JPEG, null);
        BipImageDescriptor.Builder builder = new BipImageDescriptor.Builder();
        builder.setEncoding(encoding);
        builder.setFixedDimensions(1280, 960);
        builder.setFileSize(500000);

        BipImageDescriptor descriptor = builder.build();
        assertThat(descriptor.toString()).isEqualTo(expected);
    }

    @Test
    public void testBuildImageDescriptor_proprietaryEncoding() {
        String expected =
                sXmlDocDecl
                        + "<image-descriptor version=\"1.0\">\r\n"
                        + "  <image encoding=\"USR-NOKIA-1\" pixel=\"1280*960\" size=\"500000\""
                        + " />\r\n"
                        + "</image-descriptor>";

        BipImageDescriptor.Builder builder = new BipImageDescriptor.Builder();
        builder.setProprietaryEncoding("NOKIA-1");
        builder.setFixedDimensions(1280, 960);
        builder.setFileSize(500000);

        BipImageDescriptor descriptor = builder.build();
        assertThat(descriptor.toString()).isEqualTo(expected);
    }

    @Test
    public void testBuildImageDescriptor_transformationConstantStretch() {
        String expected =
                sXmlDocDecl
                        + "<image-descriptor version=\"1.0\">\r\n"
                        + "  <image encoding=\"USR-NOKIA-1\" pixel=\"1280*960\" "
                        + "transformation=\"stretch\" />\r\n"
                        + "</image-descriptor>";

        BipImageDescriptor.Builder builder = new BipImageDescriptor.Builder();
        builder.setProprietaryEncoding("NOKIA-1");
        builder.setFixedDimensions(1280, 960);
        builder.setTransformation(BipTransformation.STRETCH);

        BipImageDescriptor descriptor = builder.build();
        assertThat(descriptor.toString()).isEqualTo(expected);
    }

    @Test
    public void testBuildImageDescriptor_transformationConstantCrop() {
        String expected =
                sXmlDocDecl
                        + "<image-descriptor version=\"1.0\">\r\n"
                        + "  <image encoding=\"USR-NOKIA-1\" pixel=\"1280*960\" "
                        + "transformation=\"crop\" />\r\n"
                        + "</image-descriptor>";

        BipImageDescriptor.Builder builder = new BipImageDescriptor.Builder();
        builder.setProprietaryEncoding("NOKIA-1");
        builder.setFixedDimensions(1280, 960);
        builder.setTransformation(BipTransformation.CROP);

        BipImageDescriptor descriptor = builder.build();
        assertThat(descriptor.toString()).isEqualTo(expected);
    }

    @Test
    public void testBuildImageDescriptor_transformationConstantFill() {
        String expected =
                sXmlDocDecl
                        + "<image-descriptor version=\"1.0\">\r\n"
                        + "  <image encoding=\"USR-NOKIA-1\" pixel=\"1280*960\" "
                        + "transformation=\"fill\" />\r\n"
                        + "</image-descriptor>";

        BipImageDescriptor.Builder builder = new BipImageDescriptor.Builder();
        builder.setProprietaryEncoding("NOKIA-1");
        builder.setFixedDimensions(1280, 960);
        builder.setTransformation(BipTransformation.FILL);

        BipImageDescriptor descriptor = builder.build();
        assertThat(descriptor.toString()).isEqualTo(expected);
    }

    @Test
    public void testBuildImageDescriptor_transformationConstantCropThenFill() {
        String expected =
                sXmlDocDecl
                        + "<image-descriptor version=\"1.0\">\r\n"
                        + "  <image encoding=\"USR-NOKIA-1\" pixel=\"1280*960\" "
                        + "transformation=\"fill\" />\r\n"
                        + "</image-descriptor>";

        BipImageDescriptor.Builder builder = new BipImageDescriptor.Builder();
        builder.setProprietaryEncoding("NOKIA-1");
        builder.setFixedDimensions(1280, 960);
        builder.setTransformation(BipTransformation.CROP);
        builder.setTransformation(BipTransformation.FILL);

        BipImageDescriptor descriptor = builder.build();
        assertThat(descriptor.toString()).isEqualTo(expected);
    }

    @Test
    public void testBuildImageDescriptor_noSize() {
        String expected =
                sXmlDocDecl
                        + "<image-descriptor version=\"1.0\">\r\n"
                        + "  <image encoding=\"JPEG\" pixel=\"1280*960\" />\r\n"
                        + "</image-descriptor>";

        BipImageDescriptor.Builder builder = new BipImageDescriptor.Builder();
        builder.setEncoding(BipEncoding.JPEG);
        builder.setFixedDimensions(1280, 960);

        BipImageDescriptor descriptor = builder.build();
        assertThat(descriptor.toString()).isEqualTo(expected);
    }

    @Test
    public void testBuildImageDescriptor_useMaxSize() {
        String expected =
                sXmlDocDecl
                        + "<image-descriptor version=\"1.0\">\r\n"
                        + "  <image encoding=\"JPEG\" pixel=\"1280*960\" maxsize=\"500000\" />\r\n"
                        + "</image-descriptor>";

        BipImageDescriptor.Builder builder = new BipImageDescriptor.Builder();
        builder.setEncoding(BipEncoding.JPEG);
        builder.setFixedDimensions(1280, 960);
        builder.setMaxFileSize(500000);

        BipImageDescriptor descriptor = builder.build();
        assertThat(descriptor.toString()).isEqualTo(expected);
    }

    @Test
    public void testBuildImageDescriptor_allButSize() {
        String expected =
                sXmlDocDecl
                        + "<image-descriptor version=\"1.0\">\r\n"
                        + "  <image encoding=\"JPEG\" pixel=\"1280*960\" maxsize=\"500000\" "
                        + "transformation=\"fill\" />\r\n"
                        + "</image-descriptor>";

        BipImageDescriptor.Builder builder = new BipImageDescriptor.Builder();
        builder.setEncoding(BipEncoding.JPEG);
        builder.setFixedDimensions(1280, 960);
        builder.setMaxFileSize(500000);
        builder.setTransformation(BipTransformation.FILL);

        BipImageDescriptor descriptor = builder.build();
        assertThat(descriptor.toString()).isEqualTo(expected);
    }

    @Test
    public void testBuildImageDescriptor_noEncoding() {
        BipImageDescriptor.Builder builder = new BipImageDescriptor.Builder();
        builder.setFixedDimensions(1280, 960);
        builder.setFileSize(500000);

        BipImageDescriptor descriptor = builder.build();
        assertThat(descriptor.toString()).isNull();
    }

    @Test
    public void testBuildImageDescriptor_noPixel() {
        BipImageDescriptor.Builder builder = new BipImageDescriptor.Builder();
        builder.setEncoding(BipEncoding.JPEG);
        builder.setFileSize(500000);

        BipImageDescriptor descriptor = builder.build();
        assertThat(descriptor.toString()).isNull();
    }

    @Test
    public void testBuildImageDescriptor_noEncodingNoPixel() {
        BipImageDescriptor.Builder builder = new BipImageDescriptor.Builder();
        builder.setFileSize(500000);

        BipImageDescriptor descriptor = builder.build();
        assertThat(descriptor.toString()).isNull();
    }

    @Test
    public void testEquals_sameInstance() {
        BipImageDescriptor.Builder builder = new BipImageDescriptor.Builder();

        BipImageDescriptor descriptor = builder.build();

        assertThat(descriptor).isEqualTo(descriptor);
    }

    @Test
    @SuppressLint("TruthIncompatibleType") // That the point of this test
    public void testEquals_differentClass() {
        BipImageDescriptor.Builder builder = new BipImageDescriptor.Builder();

        BipImageDescriptor descriptor = builder.build();
        String notDescriptor = "notDescriptor";

        assertThat(descriptor).isNotEqualTo(notDescriptor);
    }

    @Test
    public void testEquals_sameInfo() {
        BipImageDescriptor.Builder builder = new BipImageDescriptor.Builder();
        BipImageDescriptor.Builder builderEqual = new BipImageDescriptor.Builder();

        BipImageDescriptor descriptor = builder.build();
        BipImageDescriptor descriptorEqual = builderEqual.build();

        assertThat(descriptor).isEqualTo(descriptorEqual);
    }
}
