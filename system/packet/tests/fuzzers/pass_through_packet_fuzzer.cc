/*
 * Copyright 2020 The Android Open Source Project
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

// Adapted from pass_through_packet_fuzzer.cc

#include <gtest/gtest.h>

#include "avrcp_test_packets.h"
#include "packet_test_helper.h"
#include "pass_through_packet.h"

namespace bluetooth {
namespace avrcp {

using TestPassThroughPacket = TestPacketType<PassThroughPacket>;

extern "C" int LLVMFuzzerTestOneInput(const char* data, size_t size) {
  std::vector<uint8_t> pass_through_command_play_pushed;

  // Expected packet size by the library is ~5
  if (size >= 10) {
    for (size_t x = 0; x < size; x++) {
      pass_through_command_play_pushed.push_back(data[x]);
    }

    auto test_packet = TestPassThroughPacket::Make(pass_through_command_play_pushed);
    test_packet->GetKeyState();
    test_packet->GetOperationId();

    test_packet = TestPassThroughPacket::Make(pass_through_command_play_released);
    test_packet->GetKeyState();
    test_packet->GetOperationId();
    test_packet->GetData();
    test_packet->IsValid();
    test_packet->ToString();
  }

  return 0;
}

}  // namespace avrcp
}  // namespace bluetooth
