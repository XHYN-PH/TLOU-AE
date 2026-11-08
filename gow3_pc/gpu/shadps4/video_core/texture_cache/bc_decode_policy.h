// SPDX-License-Identifier: GPL-2.0-or-later
#pragma once
#include <string_view>
namespace VideoCore {
enum class BcMode { Auto, Off, Force };
enum class BcPath { Native, Decode, Unsupported };
constexpr BcMode ParseBcMode(std::string_view value) {
    return value=="0"?BcMode::Off:value=="1"?BcMode::Force:BcMode::Auto;
}
constexpr BcPath SelectBcPath(BcMode mode,bool sampled) {
    return mode==BcMode::Force?BcPath::Decode:sampled?BcPath::Native:
        mode==BcMode::Off?BcPath::Unsupported:BcPath::Decode;
}
}
