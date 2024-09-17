# Copyright 2024 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import asyncio
import avatar

from avatar import PandoraDevices, BumblePandoraDevice
from mobly import base_test, signals
from mobly.asserts import assert_in  # type: ignore

from pandora.host_pb2 import RANDOM
from pandora_experimental.gatt_grpc import GATT

from bumble.att import UUID
from bumble.gatt import GATT_AUDIO_INPUT_CONTROL_SERVICE
from bumble.profiles.aics import AICSService


class AicsTest(base_test.BaseTestClass):

    def setup_class(self) -> None:
        self.devices = PandoraDevices(self)
        self.dut, self.ref, *_ = self.devices

        if not isinstance(self.ref, BumblePandoraDevice):
            raise signals.TestAbortClass('Test require Bumble as reference device.')

    def teardown_class(self) -> None:
        if self.devices:
            self.devices.stop_all()

    @avatar.asynchronous
    async def setup_test(self) -> None:
        await asyncio.gather(self.dut.reset(), self.ref.reset())

        self.ref.device.add_service(AICSService())  # type: ignore

    def test_gatt_discover_aics_service(self) -> None:
        advertise = self.ref.host.Advertise(legacy=True, connectable=True)
        dut_ref_connection = self.dut.host.ConnectLE(public=self.ref.address, own_address_type=RANDOM).connection
        assert dut_ref_connection
        advertise.cancel()  # type: ignore

        dut_gatt = GATT(self.dut.channel)  # type: ignore
        services = dut_gatt.DiscoverServices(dut_ref_connection).services
        uuids = [UUID(service.uuid) for service in services]

        assert_in(GATT_AUDIO_INPUT_CONTROL_SERVICE, uuids)
