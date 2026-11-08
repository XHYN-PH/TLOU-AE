// SPDX-License-Identifier: GPL-2.0-or-later
#include "gow3_mobile.h"
#include "gow3_mobile_policy.h"
#include "video_core/renderer_vulkan/tlou_adpf.h"
#include <algorithm>
#include <array>
#include <atomic>
#include <chrono>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <mutex>
#ifdef __ANDROID__
#include <dlfcn.h>
#include <pthread.h>
#include <time.h>
#include <unistd.h>
#endif

namespace Gow3Mobile {
namespace {
using namespace Gow3MobilePolicy;
bool Enabled(const char* name) { const char* v=std::getenv(name);return v && v[0]=='1'; }
std::atomic<bool> short_memory{false};
}
bool MemoryShort() { return short_memory.load(std::memory_order_relaxed); }
std::uint64_t MemoryBudget(std::uint64_t driver_budget,std::uint64_t used) {
    if (!driver_budget || !Enabled("TLOU_MOBILE_MEMORY_POLICY")) return driver_budget;
    static std::mutex mutex;std::scoped_lock lock{mutex};
    static u64 current=0,ceiling=0;static int relaxed=0;
    static auto next=std::chrono::steady_clock::time_point{};
    const auto now=std::chrono::steady_clock::now();
    if (now<next) return current ? std::min(current,driver_budget) : driver_budget;
    next=now+std::chrono::seconds(2);
    u64 available=0,swap_total=0,swap_free=0;unsigned found=0;
    if (FILE* file=std::fopen("/proc/meminfo","r")) {
        char line[160];unsigned long long value;
        while(std::fgets(line,sizeof(line),file)) {
            if(std::sscanf(line,"MemAvailable: %llu kB",&value)==1){available=value*1024;++found;}
            else if(std::sscanf(line,"SwapTotal: %llu kB",&value)==1){swap_total=value*1024;++found;}
            else if(std::sscanf(line,"SwapFree: %llu kB",&value)==1){swap_free=value*1024;++found;}
        }
        std::fclose(file);
    }
    if(found<3)return driver_budget;
    double psi=-1;
    if(FILE* file=std::fopen("/proc/pressure/memory","r")) {
        char line[160];double value;
        while(std::fgets(line,sizeof(line),file))if(std::sscanf(line,"full avg10=%lf",&value)==1)psi=value;
        std::fclose(file);
    }
    // The launch-time budget can be small while assets/driver caches initialize.
    // Re-evaluate the ceiling instead of permanently latching that first value.
    const u64 previous=current;
    // MemAvailable is additional headroom, not the entire process/GPU budget.
    // Include current allocations and leave the system reserve available.
    ceiling=Ceiling(driver_budget,used,available);
    if(!current)current=ceiling;
    current=std::min(current,ceiling);
    const auto decision=Step(current,ceiling,used,available,swap_total,swap_free,psi,relaxed);
    if(decision.bytes!=previous)std::printf("TLOU RAM budget: %llu -> %llu MiB; available=%llu used=%llu driver=%llu MiB psi=%.2f\n",(unsigned long long)(previous/MiB),(unsigned long long)(decision.bytes/MiB),(unsigned long long)(available/MiB),(unsigned long long)(used/MiB),(unsigned long long)(driver_budget/MiB),psi);
    current=decision.bytes;short_memory.store(decision.short_memory,std::memory_order_relaxed);
    return current;
}

#ifdef __ANDROID__
namespace {
struct NativeThreads {std::mutex mutex;int32_t command=0,draw=0,recorder=0;};
NativeThreads& Threads(){static NativeThreads ids;return ids;}
}
void MainThread(){}
void ThreadNamed(const char* name){
    if(!name || !Enabled("TLOU_ADPF"))return;
    auto& ids=Threads();std::scoped_lock lock{ids.mutex};
    if(std::strcmp(name,"shadPS4:GpuCommandProcessor")==0)ids.command=gettid();
    else if(std::strcmp(name,"gow3:DrawRec")==0)ids.draw=gettid();
    else if(std::strcmp(name,"gow3:VkRecorder")==0)ids.recorder=gettid();
}
void FrameSubmitted(){
    if(!Enabled("TLOU_ADPF"))return;
    auto& ids=Threads();int32_t command,draw,recorder;
    {std::scoped_lock lock{ids.mutex};command=ids.command;draw=ids.draw;recorder=ids.recorder;}
    Vulkan::TlouAdpf::OnFrame(command,draw,recorder);
}
#else
void MainThread(){}
void ThreadNamed(const char*){}
void FrameSubmitted(){}
#endif
}
