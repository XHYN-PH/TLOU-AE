// SPDX-License-Identifier: GPL-2.0-or-later
#include "gow3_mobile.h"
#include "gow3_mobile_policy.h"
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
    if(!current) {
        const u64 reserve=1536*MiB;
        ceiling=std::min(driver_budget,std::max(std::clamp(available>reserve?available-reserve:0,u64(1024*MiB),u64(4096*MiB)),used/4*5));
        current=ceiling;
    }
    ceiling=std::min(ceiling,driver_budget);
    current=std::min(current,ceiling);
    const auto decision=Step(current,ceiling,used,available,swap_total,swap_free,psi,relaxed);
    if(decision.bytes!=current)std::printf("TLOU RAM budget: %llu -> %llu MiB; available=%llu MiB\n",(unsigned long long)(current/MiB),(unsigned long long)(decision.bytes/MiB),(unsigned long long)(available/MiB));
    current=decision.bytes;short_memory.store(decision.short_memory,std::memory_order_relaxed);
    return current;
}

#ifdef __ANDROID__
namespace {
struct HintState {
    std::mutex mutex;
    void* library=nullptr;void* manager=nullptr;void* session=nullptr;
    void* (*get_manager)()=nullptr;
    void* (*create)(void*,const int32_t*,size_t,int64_t)=nullptr;
    int (*report)(void*,int64_t)=nullptr;
    void (*close)(void*)=nullptr;
    void* thermal=nullptr;
    void* (*thermal_acquire)()=nullptr;
    int (*thermal_status)(void*)=nullptr;
    void (*thermal_release)(void*)=nullptr;
    std::chrono::steady_clock::time_point thermal_next{};
    std::array<int32_t,8> tids{};size_t count=0;bool dirty=false,attempted=false,failed=false;
    clockid_t main_clock=-1,gpu_clock=-1;std::uint64_t main_previous=0,gpu_previous=0;
    ~HintState(){if(session && close)close(session);if(thermal && thermal_release)thermal_release(thermal);if(library)dlclose(library);}
};
HintState& State(){static HintState state;return state;}
void Register(bool main,bool gpu) {
    if(!Enabled("TLOU_ADPF"))return;
    auto& s=State();std::scoped_lock lock{s.mutex};const int32_t tid=gettid();
    if(std::find(s.tids.begin(),s.tids.begin()+s.count,tid)==s.tids.begin()+s.count && s.count<s.tids.size()){
        s.tids[s.count++]=tid;s.dirty=true;
    }
    clockid_t clock;
    if(pthread_getcpuclockid(pthread_self(),&clock)==0){if(main)s.main_clock=clock;if(gpu)s.gpu_clock=clock;}
}
std::uint64_t CpuTime(clockid_t clock){timespec t{};return clock!=-1 && clock_gettime(clock,&t)==0?std::uint64_t(t.tv_sec)*1000000000+t.tv_nsec:0;}
}
void MainThread(){Register(true,false);}
void ThreadNamed(const char* name){
    if(!name)return;
    const bool gpu=std::strcmp(name,"shadPS4:GpuCommandProcessor")==0;
    if(gpu || std::strcmp(name,"gow3:DrawRec")==0 || std::strcmp(name,"gow3:VkRecorder")==0)Register(false,gpu);
}
void FrameSubmitted(){
    if(!Enabled("TLOU_ADPF"))return;
    auto& s=State();std::scoped_lock lock{s.mutex};if(s.failed || !s.count)return;
    if(!s.attempted){
        s.attempted=true;s.library=dlopen("libandroid.so",RTLD_NOW|RTLD_LOCAL);
        if(s.library){
            s.get_manager=reinterpret_cast<decltype(s.get_manager)>(dlsym(s.library,"APerformanceHint_getManager"));
            s.create=reinterpret_cast<decltype(s.create)>(dlsym(s.library,"APerformanceHint_createSession"));
            s.report=reinterpret_cast<decltype(s.report)>(dlsym(s.library,"APerformanceHint_reportActualWorkDuration"));
            s.close=reinterpret_cast<decltype(s.close)>(dlsym(s.library,"APerformanceHint_closeSession"));
            s.thermal_acquire=reinterpret_cast<decltype(s.thermal_acquire)>(dlsym(s.library,"AThermal_acquireManager"));
            s.thermal_status=reinterpret_cast<decltype(s.thermal_status)>(dlsym(s.library,"AThermal_getCurrentThermalStatus"));
            s.thermal_release=reinterpret_cast<decltype(s.thermal_release)>(dlsym(s.library,"AThermal_releaseManager"));
            if(s.thermal_acquire && s.thermal_status && s.thermal_release)s.thermal=s.thermal_acquire();
        }
        if(!s.get_manager || !s.create || !s.report || !s.close || !(s.manager=s.get_manager())){s.failed=true;std::puts("TLOU ADPF: unavailable; using system scheduling");return;}
    }
    const char* cap=std::getenv("GOW3_FPS_LIMIT");const int fps=std::clamp(cap?std::atoi(cap):60,1,120);
    const auto now=std::chrono::steady_clock::now();
    if(s.thermal && now>=s.thermal_next){
        std::printf("TLOU thermal status: %d (0=none, 1=light, 2=moderate, 3=severe, 4+=critical)\n",s.thermal_status(s.thermal));
        s.thermal_next=now+std::chrono::seconds(5);
    }
    if(!s.session || s.dirty){
        if(s.session)s.close(s.session);
        s.session=s.create(s.manager,s.tids.data(),s.count,1000000000LL/fps);s.dirty=false;
        if(!s.session){s.failed=true;std::puts("TLOU ADPF: session refused; using system scheduling");return;}
        s.main_previous=CpuTime(s.main_clock);s.gpu_previous=CpuTime(s.gpu_clock);
        std::printf("TLOU ADPF: active for %zu native threads, target=%d FPS\n",s.count,fps);return;
    }
    const auto main=CpuTime(s.main_clock),gpu=CpuTime(s.gpu_clock);
    const auto delta=[](std::uint64_t now,std::uint64_t before){return now && before && now>=before?now-before:0;};
    // Native CPU time excludes vblank, limiter sleep and semaphore waits.
    const auto work=std::max(delta(main,s.main_previous),delta(gpu,s.gpu_previous));s.main_previous=main;s.gpu_previous=gpu;
    if(work && s.report(s.session,int64_t(work))!=0){s.close(s.session);s.session=nullptr;s.failed=true;std::puts("TLOU ADPF: reporting failed; session disabled");}
}
#else
void MainThread(){}
void ThreadNamed(const char*){}
void FrameSubmitted(){}
#endif
}
