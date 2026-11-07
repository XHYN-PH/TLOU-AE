// SPDX-License-Identifier: GPL-2.0-or-later
#pragma once
#include <algorithm>
#include <cstdint>
#include <cstddef>
namespace VideoCore::Detail {
inline std::uint32_t StoredFaultCount(std::uint64_t reported,std::size_t words) {
    // Word zero is the counter. Clamp before narrowing, including corrupt counters.
    return words ? std::uint32_t(std::min<std::uint64_t>(reported,words-1)) : 0;
}
}
