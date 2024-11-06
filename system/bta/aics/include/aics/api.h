// Copyright (C) 2024 The Android Open Source Project
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at

// http://www.apache.org/licenses/LICENSE-2.0

// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

#pragma once

#ifndef TARGET_FLOSS
#include <bluetooth/constants/aics/Mute.h>
#endif

#include <cstdint>

namespace bluetooth::aics {

#ifndef TARGET_FLOSS
using Mute = bluetooth::constants::aics::Mute;
#else
// TODO: b/376941621 Support the aidl generation in FLOSS
enum class Mute : int8_t { NOT_MUTED = 0, MUTED = 1, DISABLED = 2 };
#endif

/** Check if the data is a correct Mute value */
bool isValidAudioInputMuteValue(uint8_t data);

/** Convert valid data into a Mute value. Abort if data is not valid */
Mute parseMuteField(uint8_t data);
}  // namespace bluetooth::aics
