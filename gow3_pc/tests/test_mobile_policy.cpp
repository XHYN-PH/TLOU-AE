// SPDX-License-Identifier: GPL-2.0-or-later
#include "../gpu/shim/gow3_mobile_policy.h"
#include <cassert>
#include <cstdio>
int main(){
    using namespace Gow3MobilePolicy;
    int calm=0;
    assert(Ceiling(8192*MiB,2048*MiB,4096*MiB)==4096*MiB);
    assert(Ceiling(8192*MiB,4096*MiB,2048*MiB)==4608*MiB);
    assert(Ceiling(8192*MiB,4096*MiB,400*MiB)==4096*MiB);
    assert(Ceiling(2048*MiB,4096*MiB,8192*MiB)==2048*MiB);
    auto growing=Step(2048*MiB,6144*MiB,4096*MiB,6144*MiB,0,0,0,calm);
    assert(growing.bytes==5120*MiB);
    growing=Step(2048*MiB,3072*MiB,4096*MiB,6144*MiB,0,0,0,calm);
    assert(growing.bytes==3072*MiB); // driver ceiling is authoritative
    growing=Step(2048*MiB,6144*MiB,4096*MiB,400*MiB,0,0,0,calm);
    assert(growing.bytes==2048*MiB); // never grow under real system pressure
    auto result=Step(4096*MiB,4096*MiB,0,800*MiB,0,0,-1,calm);
    assert(result.bytes==3840*MiB && result.short_memory);
    result=Step(4096*MiB,4096*MiB,0,400*MiB,0,0,-1,calm);assert(result.bytes==3584*MiB);
    result=Step(4096*MiB,4096*MiB,3500*MiB,400*MiB,0,0,-1,calm);assert(result.bytes==4096*MiB);
    result=Step(4096*MiB,4096*MiB,0,4096*MiB,4096*MiB,200*MiB,-1,calm);assert(result.bytes==3584*MiB);
    result=Step(4096*MiB,4096*MiB,0,4096*MiB,0,0,16,calm);assert(result.bytes==3584*MiB);
    calm=0;
    for(int i=0;i<4;i++){result=Step(2048*MiB,4096*MiB,0,4096*MiB,0,0,0,calm);assert(result.bytes==2048*MiB);}
    result=Step(2048*MiB,4096*MiB,0,4096*MiB,0,0,0,calm);assert(result.bytes==2176*MiB);
    for(unsigned available=128;available<=4096;available+=128){
        result=Step(2048*MiB,4096*MiB,0,available*MiB,4096*MiB,2048*MiB,0,calm);
        assert(result.bytes>=1024*MiB && result.bytes<=4096*MiB);
    }
    std::puts("Mobile RAM policy: pressure, active usage floor, swap/PSI and bounded recovery PASS");
}
