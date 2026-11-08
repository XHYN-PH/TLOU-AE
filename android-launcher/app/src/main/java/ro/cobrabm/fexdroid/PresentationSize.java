package ro.cobrabm.fexdroid;

import android.content.Context;
import java.util.Map;

/** Output buffers are independent of guest render patches and screen input coordinates. */
final class PresentationSize {
    static int[] dimensions(int width,int height,int sceneHeight,boolean reduced) {
        if(width<1 || height<1)throw new IllegalArgumentException("Invalid physical display size");
        if(!reduced)return new int[]{width,height};
        double factor=Math.min(1.0,Math.max(Math.max(180.0/height,320.0/width),(double)sceneHeight/height));
        return new int[]{Math.min(width,Math.max(2,(int)Math.round(width*factor/2)*2)),
            Math.min(height,Math.max(2,(int)Math.round(height*factor/2)*2))};
    }
    static void apply(Context context,Map<String,String> env) {
        int width=Integer.parseInt(env.get("GOW3_SCREEN_W")),height=Integer.parseInt(env.get("GOW3_SCREEN_H"));
        String[] size=env.getOrDefault("TLOU_EXPECT_SCENE_SIZE","1920x1080").split("x");
        int[] output=dimensions(width,height,Integer.parseInt(size[1]),
            "scene".equals(PortSettings.prefs(context).getString("perf_output_size","native")));
        env.put("GOW3_SCREEN_W",String.valueOf(output[0]));env.put("GOW3_SCREEN_H",String.valueOf(output[1]));
    }
}
