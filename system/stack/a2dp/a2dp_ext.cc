/**
 * Copyright 2023 The Android Open Source Project
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

#define LOG_TAG "bluetooth-a2dp"

#include "a2dp_ext.h"

#include <bluetooth/log.h>

#include <cstddef>
#include <cstdint>
#include <cstring>
#include <vector>

#include "a2dp_codec_api.h"
#include "a2dp_constants.h"
#include "audio_hal_interface/a2dp_encoding.h"
#include "hardware/bt_av.h"

using namespace bluetooth;

static a2dp::CodecId codec_id(btav_a2dp_codec_index_t codec_index) {
  bluetooth::a2dp::CodecId id;
  auto result = ::bluetooth::audio::a2dp::provider::codec_info(codec_index, &id, nullptr, nullptr);
  log::assert_that(result, "provider::codec_info unexpectdly failed");
  return id;
}

A2dpCodecConfigExt::A2dpCodecConfigExt(btav_a2dp_codec_index_t codec_index, bool is_source)
    : A2dpCodecConfig(codec_index, codec_id(codec_index),
                      bluetooth::audio::a2dp::provider::codec_index_str(codec_index).value(),
                      BTAV_A2DP_CODEC_PRIORITY_DEFAULT),
      is_source_(is_source) {
  // Load the local capabilities from the provider info.
  auto result = ::bluetooth::audio::a2dp::provider::codec_info(
          codec_index, nullptr, ota_codec_config_, &codec_local_capability_);
  log::assert_that(result, "provider::codec_info unexpectdly failed");
}

tA2DP_STATUS A2dpCodecConfigExt::setCodecConfig(const uint8_t* p_peer_codec_info,
                                                bool /* is_capability */,
                                                uint8_t* p_result_codec_config) {
  if (p_peer_codec_info == nullptr || p_result_codec_config == nullptr) {
    return A2DP_FAIL;
  }

  // Call get_a2dp_config to recompute best capabilities.
  // This method need to update codec_config_, and ota_codec_config_
  // using the local codec_user_config_, and input peer_codec_info.
  using namespace bluetooth::audio::a2dp;
  provider::a2dp_remote_capabilities capabilities = {
          .seid = 0,  // the SEID does not matter here.
          .capabilities = p_peer_codec_info,
  };

  auto result = provider::get_a2dp_configuration(
          RawAddress::kEmpty, std::vector<provider::a2dp_remote_capabilities>{capabilities},
          codec_user_config_);
  if (!result.has_value()) {
    log::error("Failed to set a configuration for {}", name_);
    return AVDTP_UNSUPPORTED_CONFIGURATION;
  }

  // Use the local capabilities for the selectable capabilities:
  // the provider AIDL HAL does not provide an interface to parse the
  // peer capabilities and the selectable capabilities cannot be
  // computed.
  codec_selectable_capability_ = codec_local_capability_;
  codec_config_ = result->codec_parameters;
  vendor_specific_parameters_ = result->vendor_specific_parameters;
  memcpy(ota_codec_config_, result->codec_config, sizeof(ota_codec_config_));
  memcpy(p_result_codec_config, result->codec_config, sizeof(ota_codec_config_));
  return A2DP_SUCCESS;
}

bool A2dpCodecConfigExt::setPeerCodecCapabilities(const uint8_t* p_peer_codec_capabilities) {
  // Use the local capabilities for the selectable capabilities:
  // the provider AIDL HAL does not provide an interface to parse the
  // peer capabilities and the selectable capabilities cannot be
  // computed.
  codec_selectable_capability_ = codec_local_capability_;
  memcpy(ota_codec_peer_capability_, p_peer_codec_capabilities, sizeof(ota_codec_peer_capability_));
  return true;
}

void A2dpCodecConfigExt::setCodecConfig(btav_a2dp_codec_config_t codec_parameters,
                                        uint8_t const codec_config[AVDT_CODEC_SIZE],
                                        std::vector<uint8_t> const& vendor_specific_parameters) {
  // Use the local capabilities for the selectable capabilities:
  // the provider AIDL HAL does not provide an interface to parse the
  // peer capabilities and the selectable capabilities cannot be
  // computed.
  codec_selectable_capability_ = codec_local_capability_;
  codec_config_ = codec_parameters;
  memcpy(ota_codec_config_, codec_config, sizeof(ota_codec_config_));
  vendor_specific_parameters_ = vendor_specific_parameters;
}

tA2DP_ENCODER_INTERFACE const a2dp_encoder_interface_ext = {
        .encoder_init = [](const tA2DP_ENCODER_INIT_PEER_PARAMS*, A2dpCodecConfig*,
                           a2dp_source_read_callback_t, a2dp_source_enqueue_callback_t) {},
        .encoder_cleanup = []() {},
        .feeding_reset = []() {},
        .feeding_flush = []() {},
        .get_encoder_interval_ms = []() { return (uint64_t)20; },
        .get_effective_frame_size = []() { return 0; },
        .send_frames = [](uint64_t) {},
        .set_transmit_queue_length = [](size_t) {},
};

const tA2DP_ENCODER_INTERFACE* A2DP_GetEncoderInterfaceExt(const uint8_t*) {
  return &a2dp_encoder_interface_ext;
}
