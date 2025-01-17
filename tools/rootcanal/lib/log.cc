/*
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

#include "log.h"

#include <array>
#include <chrono>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <ctime>
#include <format>
#include <iostream>
#include <iterator>
#include <optional>

namespace rootcanal::log {

// Enable flag for log styling.
static bool enable_log_color = true;

enum class color : uint32_t {
  aquamarine = 0x7FFFD4,         // rgb(127,255,212)
  black = 0x000000,              // rgb(0,0,0)
  blue_violet = 0x8A2BE2,        // rgb(138,43,226)
  cadet_blue = 0x5F9EA0,         // rgb(95,158,160)
  chartreuse = 0x7FFF00,         // rgb(127,255,0)
  coral = 0xFF7F50,              // rgb(255,127,80)
  dark_orange = 0xFF8C00,        // rgb(255,140,0)
  deep_pink = 0xFF1493,          // rgb(255,20,147)
  dim_gray = 0x696969,           // rgb(105,105,105)
  floral_white = 0xFFFAF0,       // rgb(255,250,240)
  golden_rod = 0xDAA520,         // rgb(218,165,32)
  green_yellow = 0xADFF2F,       // rgb(173,255,47)
  indian_red = 0xCD5C5C,         // rgb(205,92,92)
  lemon_chiffon = 0xFFFACD,      // rgb(255,250,205)
  medium_orchid = 0xBA55D3,      // rgb(186,85,211)
  medium_sea_green = 0x3CB371,   // rgb(60,179,113)
  medium_slate_blue = 0x7B68EE,  // rgb(123,104,238)
  orange_red = 0xFF4500,         // rgb(255,69,0)
  red = 0xFF0000,                // rgb(255,0,0)
  turquoise = 0x40E0D0,          // rgb(64,224,208)
  wheat = 0xF5DEB3,              // rgb(245,222,179)
  yellow = 0xFFFF00,             // rgb(255,255,0)
};

void SetLogColorEnable(bool enable) { enable_log_color = enable; }

static std::array<char, 5> verbosity_tag = {'D', 'I', 'W', 'E', 'F'};

static std::array<char const*, 5> text_style = {
        "\033[38;5;254m", "\033[38;5;15m", "\033[38;5;226m", "\033[38;5;160m", "\033[38;5;9m",
};

static std::array<color, 16> text_color = {
        color::cadet_blue,   color::aquamarine,       color::indian_red, color::blue_violet,
        color::chartreuse,   color::medium_sea_green, color::deep_pink,  color::medium_orchid,
        color::green_yellow, color::dark_orange,      color::golden_rod, color::medium_slate_blue,
        color::coral,        color::lemon_chiffon,    color::wheat,      color::turquoise,
};

void VLog(Verbosity verb, char const* file, int line, std::optional<int> instance,
          char const* format, std::format_args args) {
  // Generate the time label.
  auto now = std::chrono::system_clock::now();
  auto now_ms = std::chrono::time_point_cast<std::chrono::milliseconds>(now);
  auto now_t = std::chrono::system_clock::to_time_t(now);
  char time_str[19];  // "mm-dd_HH:MM:SS.mmm\0" is 19 byte long
  auto n = std::strftime(time_str, sizeof(time_str), "%m-%d %H:%M:%S", std::localtime(&now_t));
  snprintf(time_str + n, sizeof(time_str) - n, ".%03u",
           static_cast<unsigned int>(now_ms.time_since_epoch().count() % 1000));

  // Generate the file label.
  char delimiter = '/';
  char const* file_name = ::strrchr(file, delimiter);
  file_name = file_name == nullptr ? file : file_name + 1;
  char file_str[40];  // file:line limited to 40 characters
  snprintf(file_str, sizeof(file_str), "%.35s:%d", file_name, line);

  std::ostream_iterator<char> out(std::cout);
  std::format_to(out, "root-canal {} {} {:<40} ", verbosity_tag[verb], time_str,
                 reinterpret_cast<char*>(file_str));

  if (instance.has_value() && enable_log_color) {
    color instance_color = text_color[*instance % text_color.size()];
    std::format_to(out, "\033[38;5;0;48;2;{};{};{}m {:>2} \033[0m ",
                   ((unsigned)instance_color >> 16) & 0xFF, ((unsigned)instance_color >> 8) & 0xFF,
                   ((unsigned)instance_color >> 0) & 0xFF, *instance);
  } else if (instance.has_value()) {
    std::format_to(out, " {:>2}  ", *instance);
  } else {
    std::format_to(out, "     ");
  }

  if (enable_log_color) {
    std::format_to(out, "{}", text_style[verb]);
    std::vformat_to(out, format, args);
    std::format_to(out, "\033[0m");
  } else {
    std::vformat_to(out, format, args);
  }

  std::format_to(out, "\n");

  if (verb == Verbosity::kFatal) {
    std::abort();
  }
}

}  // namespace rootcanal::log
