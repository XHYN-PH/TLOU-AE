// SPDX-License-Identifier: GPL-2.0-or-later
#include "../gpu/shadps4/video_core/buffer_cache/fault_limits.h"
#include <array>
#include <cassert>
#include <cstdio>
#include <limits>
int main(){
    std::array<std::uint64_t,1024> words{};
    for(auto count:{0ull,1ull,1023ull,1024ull,65535ull,0x100000000ull,std::numeric_limits<unsigned long long>::max()}){
        words[0]=count;auto consumed=VideoCore::Detail::StoredFaultCount(words[0],words.size());
        assert(consumed<=1023);
        assert(consumed==std::min<std::uint64_t>(count,1023));
        std::uint64_t sum=0;for(std::uint32_t i=1;i<=consumed;i++)sum+=words.at(i);
        assert(sum==0);
    }
    assert(VideoCore::Detail::StoredFaultCount(100,0)==0);
    assert(VideoCore::Detail::StoredFaultCount(100,1)==0);
    std::puts("Fault list bounds: normal, overflow, corrupt 64-bit counter and empty area PASS");
}
