// SPDX-License-Identifier: GPL-2.0-or-later
#pragma once
#include <cstdint>
namespace Gow3Mobile {
std::uint64_t MemoryBudget(std::uint64_t driver_budget, std::uint64_t used);
bool MemoryShort();
void MainThread();
void ThreadNamed(const char* name);
void FrameSubmitted();
}
