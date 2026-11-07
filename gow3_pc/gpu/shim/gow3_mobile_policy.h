// SPDX-License-Identifier: GPL-2.0-or-later
// RAM policy adapted from Uncharted Android/Bloodborne's mobile budget policy.
#pragma once
#include <algorithm>
#include <cstdint>

namespace Gow3MobilePolicy {
using u64 = std::uint64_t;
constexpr u64 MiB = 1ull << 20;
inline u64 Ceiling(u64 driver,u64 used,u64 available) {
    const u64 reserve=1536*MiB;
    const u64 room=available>reserve?available-reserve:0;
    const u64 envelope=room>UINT64_MAX-used?UINT64_MAX:used+room;
    const u64 target=std::max(4096*MiB,used/4*5);
    return std::min(driver,std::max(1024*MiB,std::min(envelope,target)));
}
struct Decision { u64 bytes; bool short_memory; };
inline Decision Step(u64 current, u64 ceiling, u64 used, u64 available,
                     u64 swap_total, u64 swap_free, double psi, int& relaxed_steps) {
    const double swap = swap_total ? 100.0 * double(swap_total-std::min(swap_free,swap_total))/double(swap_total) : 0;
    const bool critical = available < 512*MiB || swap > 80 || psi >= 15;
    const bool tight = critical || available < 1024*MiB || swap > 50 || psi >= 5;
    current = std::min(current,ceiling);
    // Healthy system memory with growing live allocations is not a reason to
    // evict/re-upload the same textures against a stale startup allowance.
    if (!tight && available > 2048*MiB && (psi < 0 || psi < 1))
        current = std::max(current,std::min(ceiling,used/4*5));
    u64 next = current;
    if (tight) {
        relaxed_steps = 0;
        const u64 floor = std::min(current,std::max(1024*MiB,used/4*5));
        const u64 step = (critical ? 512 : 256)*MiB;
        next = current > floor && current-floor > step ? current-step : floor;
    } else if (available > 2048*MiB && swap < 40 && (psi < 0 || psi < 1)) {
        if (++relaxed_steps >= 5 && current < ceiling) next = std::min(ceiling,current+128*MiB);
        relaxed_steps = std::min(relaxed_steps,5);
    } else {
        relaxed_steps = 0;
    }
    return {std::min(next,ceiling),tight || next<ceiling};
}
}
