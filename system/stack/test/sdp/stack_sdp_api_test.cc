/*
 * Copyright 2022 The Android Open Source Project
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

#include <gtest/gtest.h>
#include <stdlib.h>

#include <cstddef>
#include <cstdint>
#include <memory>

#include "btif/include/btif_common.h"
#include "osi/include/allocator.h"
#include "stack/include/sdp_api.h"
#include "stack/sdp/sdpint.h"
#include "test/fake/fake_osi.h"
#include "test/mock/mock_osi_allocator.h"
#include "test/mock/mock_stack_l2cap_api.h"

#ifndef BT_DEFAULT_BUFFER_SIZE
#define BT_DEFAULT_BUFFER_SIZE (4096 + 16)
#endif

namespace {
constexpr uint8_t kSDP_MAX_CONNECTIONS = static_cast<uint8_t>(SDP_MAX_CONNECTIONS);

const RawAddress kRawAddress = RawAddress({0xA1, 0xA2, 0xA3, 0xA4, 0xA5, 0xA6});
int L2CA_ConnectReqWithSecurity_cid = 0x42;
tSDP_DISCOVERY_DB* sdp_db = nullptr;

class StackSdpWithMocksTest : public ::testing::Test {
protected:
  void SetUp() override {
    fake_osi_ = std::make_unique<::test::fake::FakeOsi>();

    test::mock::stack_l2cap_api::L2CA_ConnectReqWithSecurity.body =
            [](uint16_t /* psm */, const RawAddress& /* p_bd_addr */, uint16_t /* sec_level */) {
              return ++L2CA_ConnectReqWithSecurity_cid;
            };
    test::mock::stack_l2cap_api::L2CA_DataWrite.body = [](uint16_t /* cid */,
                                                          BT_HDR* p_data) -> tL2CAP_DW_RESULT {
      osi_free_and_reset(reinterpret_cast<void**>(&p_data));
      return tL2CAP_DW_RESULT::FAILED;
    };
    test::mock::stack_l2cap_api::L2CA_DisconnectReq.body = [](uint16_t /* cid */) { return true; };
    test::mock::stack_l2cap_api::L2CA_RegisterWithSecurity.body =
            [](uint16_t psm, const tL2CAP_APPL_INFO& /* p_cb_info */, bool /* enable_snoop */,
               tL2CAP_ERTM_INFO* /* p_ertm_info */, uint16_t /* my_mtu */,
               uint16_t /* required_remote_mtu */, uint16_t /* sec_level */) { return psm; };
  }

  void TearDown() override {
    test::mock::stack_l2cap_api::L2CA_ConnectReqWithSecurity = {};
    test::mock::stack_l2cap_api::L2CA_RegisterWithSecurity = {};
    test::mock::stack_l2cap_api::L2CA_DataWrite = {};
    test::mock::stack_l2cap_api::L2CA_DisconnectReq = {};

    fake_osi_.reset();
  }

  std::unique_ptr<test::fake::FakeOsi> fake_osi_;
};

class StackSdpApiTest : public StackSdpWithMocksTest {
protected:
  void SetUp() override {
    StackSdpWithMocksTest::SetUp();
    sdp_init();
    sdp_db = static_cast<tSDP_DISCOVERY_DB*>(osi_malloc(BT_DEFAULT_BUFFER_SIZE));
  }

  void TearDown() override {
    osi_free(sdp_db);
    sdp_free();
    StackSdpWithMocksTest::TearDown();
  }
};

}  // namespace

TEST_F(StackSdpApiTest, nop) {}

TEST_F(StackSdpApiTest, SDP_ServiceSearchRequest) {
  tSDP_DISCOVERY_DB db;
  for (uint8_t i = 0; i < kSDP_MAX_CONNECTIONS; i++) {
    RawAddress bd_addr = RawAddress({0x11, 0x22, 0x33, 0x44, 0x55, i});
    ASSERT_NE(nullptr, sdp_conn_originate(bd_addr));
  }
  ASSERT_FALSE(bluetooth::legacy::stack::sdp::get_legacy_stack_sdp_api()
                       ->service.SDP_ServiceSearchRequest(
                               kRawAddress, &db,
                               [](const RawAddress& /* bd_addr */, tSDP_RESULT /* result */) {}));
}

TEST_F(StackSdpApiTest, SDP_ServiceSearchAttributeRequest) {
  tSDP_DISCOVERY_DB db;
  for (uint8_t i = 0; i < kSDP_MAX_CONNECTIONS; i++) {
    RawAddress bd_addr = RawAddress({0x11, 0x22, 0x33, 0x44, 0x55, i});
    ASSERT_NE(nullptr, sdp_conn_originate(bd_addr));
  }
  ASSERT_FALSE(bluetooth::legacy::stack::sdp::get_legacy_stack_sdp_api()
                       ->service.SDP_ServiceSearchAttributeRequest(
                               kRawAddress, &db,
                               [](const RawAddress& /* bd_addr */, tSDP_RESULT /* result */) {}));
}

TEST_F(StackSdpApiTest, SDP_ServiceSearchAttributeRequest2) {
  tSDP_DISCOVERY_DB db;
  for (uint8_t i = 0; i < kSDP_MAX_CONNECTIONS; i++) {
    RawAddress bd_addr = RawAddress({0x11, 0x22, 0x33, 0x44, 0x55, i});
    ASSERT_NE(nullptr, sdp_conn_originate(bd_addr));
  }
  ASSERT_FALSE(bluetooth::legacy::stack::sdp::get_legacy_stack_sdp_api()
                       ->service.SDP_ServiceSearchAttributeRequest2(
                               kRawAddress, &db,
                               base::BindRepeating([](const RawAddress& /* bd_addr */,
                                                      tSDP_RESULT /* result */) {})));
}
