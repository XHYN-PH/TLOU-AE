#include "../tools/android/bionic/present/frame_benchmark.h"
#include <assert.h>
#include <stdio.h>
int main(void){
    TlouFrameBenchmark b={0};tlou_benchmark_active(&b,1);uint64_t now=1;
    tlou_benchmark_frame(&b,now);
    for(int i=0;i<100;i++){now+=i==99?500000000:33333333;tlou_benchmark_frame(&b,now);}
    TlouFrameSummary s=tlou_benchmark_summary(&b);
    assert(s.count==100 && s.p50>33.3 && s.p95<33.4 && s.p99<33.4 && s.worst==500);
    assert(s.over_33ms==1 && s.long_gaps==1); // long gaps stay included, not hidden as loading
    tlou_benchmark_active(&b,0);tlou_benchmark_frame(&b,now+10000000000ull);assert(b.count==100);
    tlou_benchmark_active(&b,1);tlou_benchmark_frame(&b,now+20000000000ull);assert(b.count==0);
    for(int i=0;i<3000;i++)tlou_benchmark_frame(&b,now+20000000001ull+(uint64_t)i*16666667);
    assert(b.count==TLOU_BENCHMARK_SAMPLES && tlou_benchmark_summary(&b).worst<17);
    puts("Frame benchmark: percentile tails, long gaps, background exclusion and bounded history PASS");
}
