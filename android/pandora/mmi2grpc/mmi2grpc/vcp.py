# Copyright (C) 2024 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""VCP proxy module."""
import threading

from mmi2grpc._helpers import assert_description, match_description
from mmi2grpc._proxy import ProfileProxy
from mmi2grpc._rootcanal import Dongle

from pandora_experimental.vcp_grpc import VCP
from pandora_experimental.gatt_grpc import GATT
from pandora.security_grpc import Security, SecurityStorage
from pandora.security_pb2 import LE_LEVEL3, PairingEventAnswer
from pandora.host_grpc import Host
from pandora.host_pb2 import PUBLIC, RANDOM
from pandora.security_grpc import Security
from pandora.security_pb2 import LE_LEVEL3, PairingEventAnswer
from pandora_experimental.le_audio_grpc import LeAudio

from time import sleep


class VCPProxy(ProfileProxy):

    def __init__(self, channel, rootcanal):
        super().__init__(channel)
        self.vcp = VCP(channel)
        self.gatt = GATT(channel)
        self.security_storage = SecurityStorage(channel)
        self.host = Host(channel)
        self.security = Security(channel)
        self.le_audio = LeAudio(channel)
        self.rootcanal = rootcanal
        self.connection = None
        self.pairing_stream = self.security.OnPairing()

    def test_started(self, test: str, description: str, pts_addr: bytes):
        self.rootcanal.select_pts_dongle(Dongle.LAIRD_BL654)

        return "OK"

    @assert_description
    def IUT_INITIATE_CONNECTION(self, pts_addr: bytes, **kwargs):
        """
        Please initiate a GATT connection to the PTS.

        Description: Verify that
        the Implementation Under Test (IUT) can initiate a GATT connect request
        to the PTS.
        """
        self.security_storage.DeleteBond(public=pts_addr)
        self.connection = self.host.ConnectLE(own_address_type=RANDOM, public=pts_addr).connection

        def secure():
            self.security.Secure(connection=self.connection, le=LE_LEVEL3)

        def vcp_connect():
            self.vcp.WaitConnect(connection=self.connection)

        threading.Thread(target=secure).start()
        threading.Thread(target=vcp_connect).start()

        return "OK"

    @match_description
    def _mmi_2004(self, pts_addr: bytes, passkey: str, **kwargs):
        """
        Please confirm that 6 digit number is matched with (?P<passkey>[0-9]*).
        """
        received = []
        for event in self.pairing_stream:
            if event.address == pts_addr and event.numeric_comparison == int(passkey):
                self.pairing_stream.send(PairingEventAnswer(
                    event=event,
                    confirm=True,
                ))
                return "OK"
            received.append(event.numeric_comparison)

        assert False, f"mismatched passcode: expected {passkey}, received {received}"

    @match_description
    def IUT_INITIATE_DISCOVER_CHARACTERISTIC(self, **kwargs):
        """
        Please take action to discover the
        (Volume Control Point|Volume State|Volume Flags|Offset State|Volume Offset Control Point)
        characteristic from the Volume (Offset )?Control. Discover the primary service if needed.
        Description: Verify that the Implementation Under Test \(IUT\) can send
        Discover All Characteristics command.
        """
        # PTS expects us to do discovery after bonding, but in fact Android does it as soon as
        # encryption is completed. Invalidate GATT cache so the discovery takes place again
        self.gatt.ClearCache(connection=self.connection)

        return "OK"

    @match_description
    def IUT_READ_CHARACTERISTIC(self, name: str, handle: str, **kwargs):
        """
        Please send Read Request to read (?P<name>(Volume State|Volume Flags|Offset State)) characteristic with handle
        = (?P<handle>(0x[0-9A-Fa-f]{4})).
        """
        # After discovery Android reads these values by itself, after profile connection.
        # Although, for some tests, this is used as validation, for example for tests with invalid
        # behavior (BI tests). Just send GATT read to sattisfy this conditions, as VCP has no exposed
        # (or even existing, native) interface to trigger read on demand.
        def read():
            nonlocal handle
            self.gatt.ReadCharacteristicFromHandle(\
                    connection=self.connection, handle=int(handle, base=16))

        worker = threading.Thread(target=read)
        worker.start()
        worker.join(timeout=30)

        return "OK"

    @match_description
    def USER_CONFIRM_SUPPORTED_CHARACTERISTIC(self, body: str, **kwargs):
        """
        Please verify that for each supported characteristic, attribute
        handle/UUID pair\(s\) is returned to the (.*)\.(?P<body>.*)
        """

        return "OK"

    @match_description
    def IUT_CONFIG_NOTIFICATION(self, name: str, **kwargs):
        """
        Please write to Client Characteristic Configuration Descriptor of
        (?P<name>(Volume State|Offset State)) characteristic to enable notification.
        """

        # After discovery Android subscribes by itself, after profile connection
        return "OK"

    def IUT_SEND_WRITE_REQUEST(self, description: str, **kwargs):
        r"""
        Please send write request to handle 0xXXXX with following value.
        Characteristic name:
            Op Code: [X (0xXX)] Op code name
            Change Counter: <WildCard: Exists>
            Value: <WildCard: Exists>
        """

        # Wait a couple seconds so the VCP is ready (subscriptions and reads are completed)
        sleep(2)

        if ("Set Absolute Volume" in description):
            self.vcp.SetDeviceVolume(connection=self.connection, volume=42)
        elif ("Unmute" in description):
            # for now, there is no way to trigger this, and tests are skipped
            return "No"
        elif ("Set Volume Offset" in description):
            self.vcp.SetVolumeOffset(connection=self.connection, offset=42)
        elif ("Volume Control Point" in description and
              "Op Code: <WildCard: Exists>" in description):
            # Handles sending *any* OP Code on Volume Control Point
            self.vcp.SetDeviceVolume(connection=self.connection, volume=42)
        elif ("Volume Offset Control Point" in description and
              "Op Code: <WildCard: Exists>" in description):
            self.vcp.SetVolumeOffset(connection=self.connection, offset=42)


        return "OK"

    @assert_description
    def _mmi_20501(self, **kwargs):
        """
        Please start general inquiry. Click 'Yes' If IUT does discovers PTS
        otherwise click 'No'.
        """
        return "OK"
