// SPDX-License-Identifier: GPL-2.0-or-later
#include "../gpu/shadps4/video_core/texture_cache/bc_decode_policy.h"
#include <cassert>
int main(){
    using namespace VideoCore;
    assert(SelectBcPath(ParseBcMode("auto"),true)==BcPath::Native);
    assert(SelectBcPath(ParseBcMode("auto"),false)==BcPath::Decode);
    assert(SelectBcPath(ParseBcMode("0"),true)==BcPath::Native);
    assert(SelectBcPath(ParseBcMode("0"),false)==BcPath::Unsupported);
    assert(SelectBcPath(ParseBcMode("1"),true)==BcPath::Decode);
    assert(SelectBcPath(ParseBcMode("1"),false)==BcPath::Decode);
}
