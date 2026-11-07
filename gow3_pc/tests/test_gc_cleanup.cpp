// SPDX-License-Identifier: GPL-2.0-or-later
#include <cassert>
#include <cstdio>
#include <vector>
#include "common/lru_cache.h"
#include "../gpu/shadps4/video_core/texture_cache/gc_policy.h"
int main(){
    using VideoCore::Detail::GcCleanupBudget;
    Common::LeastRecentlyUsedCache<unsigned,u64> lru;
    std::vector<size_t> ids;
    for(unsigned i=0;i<40;++i)ids.push_back(lru.Insert(i,0));
    GcCleanupBudget scan;scan.Reset(3);unsigned freed=0,skipped=0;
    lru.ForEachItemBelow(10,[&](unsigned item){
        if(!scan.Visit())return true;
        if(!GcCleanupBudget::Eligible(item<20,true,true)){
            ++skipped;lru.Touch(ids[item],20);return false;
        }
        lru.Free(ids[item]);scan.Freed();++freed;return false;
    });
    assert(skipped==20 && freed==3 && scan.remaining==0);
    assert(!GcCleanupBudget::Eligible(false,true,false));
    assert(GcCleanupBudget::Eligible(true,false,false));
    assert(GcCleanupBudget::Age(true,true)<GcCleanupBudget::Age(false,true));
    scan.Reset(20);unsigned visits=0;while(scan.Visit())++visits;assert(visits==256);
    std::puts("Texture GC: skipped entries preserve deletion slots, LRU touch/free order, bounded scan and critical age PASS");
}
