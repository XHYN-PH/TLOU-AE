// SPDX-License-Identifier: GPL-2.0-or-later
#pragma once
#include <cstddef>
#include <cstdint>
namespace VideoCore::Detail {
struct GcCleanupBudget {
    std::size_t remaining=0, visited=0;
    void Reset(std::size_t deletions) { remaining=deletions;visited=0; }
    bool Visit() { if(!remaining || visited>=256)return false;++visited;return true; }
    static bool Eligible(bool tiled,bool download,bool pressure) {
        return !(download && (tiled || !pressure));
    }
    void Freed() { if(remaining)--remaining; }
    static constexpr std::uint64_t Age(bool critical,bool pressure) {
        return critical?40:pressure?80:16;
    }
};
}
