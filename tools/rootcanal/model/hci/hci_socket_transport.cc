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

#include "hci_socket_transport.h"

#include "log.h"

namespace rootcanal {

HciSocketTransport::HciSocketTransport(std::shared_ptr<AsyncDataChannel> socket)
    : socket_(socket) {}

void HciSocketTransport::RegisterCallbacks(PacketCallback packet_callback,
                                           CloseCallback close_callback) {
  // TODO: Avoid the copy here by using new buffer in H4DataChannel
  h4_ = H4DataChannelPacketizer(
      socket_,
      [packet_callback](const std::vector<uint8_t>& raw_command) {
        std::shared_ptr<std::vector<uint8_t>> packet_copy =
            std::make_shared<std::vector<uint8_t>>(raw_command);
        packet_callback(PacketType::COMMAND, packet_copy);
      },
      [](const std::vector<uint8_t>&) {
        FATAL("Unexpected Event in HciSocketTransport!");
      },
      [packet_callback](const std::vector<uint8_t>& raw_acl) {
        std::shared_ptr<std::vector<uint8_t>> packet_copy =
            std::make_shared<std::vector<uint8_t>>(raw_acl);
        packet_callback(PacketType::ACL, packet_copy);
      },
      [packet_callback](const std::vector<uint8_t>& raw_sco) {
        std::shared_ptr<std::vector<uint8_t>> packet_copy =
            std::make_shared<std::vector<uint8_t>>(raw_sco);
        packet_callback(PacketType::SCO, packet_copy);
      },
      [packet_callback](const std::vector<uint8_t>& raw_iso) {
        std::shared_ptr<std::vector<uint8_t>> packet_copy =
            std::make_shared<std::vector<uint8_t>>(raw_iso);
        packet_callback(PacketType::ISO, packet_copy);
      },
      close_callback);
}

void HciSocketTransport::Tick() { h4_.OnDataReady(socket_); }

void HciSocketTransport::Send(PacketType packet_type,
                              const std::vector<uint8_t>& packet) {
  if (!socket_ || !socket_->Connected()) {
    INFO("Closed socket. Dropping packet of type {}", packet_type);
    return;
  }
  uint8_t type = static_cast<uint8_t>(packet_type);
  h4_.Send(type, packet.data(), packet.size());
}

void HciSocketTransport::Close() { socket_->Close(); }

}  // namespace rootcanal
