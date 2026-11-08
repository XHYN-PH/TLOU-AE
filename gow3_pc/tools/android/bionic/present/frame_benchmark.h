/* SPDX-License-Identifier: GPL-2.0-or-later */
#ifndef TLOU_FRAME_BENCHMARK_H
#define TLOU_FRAME_BENCHMARK_H
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#define TLOU_BENCHMARK_SAMPLES 1024
typedef struct {
    uint64_t intervals[TLOU_BENCHMARK_SAMPLES],last;
    unsigned count,next;
    int active;
} TlouFrameBenchmark;
typedef struct {double p50,p95,p99,worst;unsigned count,over_33ms,long_gaps;} TlouFrameSummary;
static inline void tlou_benchmark_active(TlouFrameBenchmark* b,int active){
    if(b->active!=active){
        if(active)memset(b,0,sizeof *b);
        b->last=0;b->active=active;
    }
}
static inline void tlou_benchmark_frame(TlouFrameBenchmark* b,uint64_t now){
    if(!b->active){b->last=0;return;}
    if(b->last && now>b->last){
        b->intervals[b->next]=(now-b->last);b->next=(b->next+1)%TLOU_BENCHMARK_SAMPLES;
        if(b->count<TLOU_BENCHMARK_SAMPLES)++b->count;
    }
    b->last=now;
}
static inline int tlou_benchmark_compare(const void* a,const void* b){
    uint64_t x=*(const uint64_t*)a,y=*(const uint64_t*)b;return (x>y)-(x<y);
}
static inline TlouFrameSummary tlou_benchmark_summary(const TlouFrameBenchmark* b){
    TlouFrameSummary s={0};s.count=b->count;if(!b->count)return s;
    uint64_t sorted[TLOU_BENCHMARK_SAMPLES];memcpy(sorted,b->intervals,b->count*sizeof(uint64_t));
    qsort(sorted,b->count,sizeof(uint64_t),tlou_benchmark_compare);
    s.p50=sorted[(b->count*50+99)/100-1]/1e6;s.p95=sorted[(b->count*95+99)/100-1]/1e6;
    s.p99=sorted[(b->count*99+99)/100-1]/1e6;s.worst=sorted[b->count-1]/1e6;
    for(unsigned i=0;i<b->count;i++){s.over_33ms+=sorted[i]>33333334;s.long_gaps+=sorted[i]>=250000000;}
    return s;
}
#endif
