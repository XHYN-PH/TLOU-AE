package ro.cobrabm.fexdroid;

import android.app.Activity;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.zip.ZipInputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=30, application=android.app.Application.class)
public class PerformanceSettingsTest {
    @Test public void scaleHasValidDimensionsAndPreservesAspectRatio(){
        for(int percent=10;percent<=100;percent++){
            assertEquals(RenderScale.width(percent)*9,RenderScale.height(percent)*16);
            assertTrue(RenderScale.height(percent)>0);
        }
        assertEquals("1920x1080",RenderScale.size(100));
        assertEquals("960x540",RenderScale.size(50));
        assertThrows(IllegalArgumentException.class,()->RenderScale.size(0));
    }
    @Test public void launchUsesCustomScaleAndKeepsUnsafeModesOff() throws Exception {
        try(var controller=Robolectric.buildActivity(Activity.class).setup()){
            Activity a=controller.get();var p=PortSettings.prefs(a);
            p.edit().putBoolean("custom_scale",true).putInt("render_scale",50)
                .putString("resolution","720").putString("render_path","proxy")
                .putString("perf_overlap","1").putString("perf_drains","nested").commit();
            PortSettings.retireTestPreferences(a);
            assertEquals("1",p.getString("perf_overlap","0"));
            var env=new HashMap<String,String>();env.put("GOW3_FEWER_DRAINS","1");env.put("GOW3_SUBMIT_NO_WAIT","1");
            PortSettings.environment(a,env);
            assertEquals("960x540",env.get("TLOU_EXPECT_SCENE_SIZE"));
            assertEquals("960x540",env.get("GOW3_MOVIE_SIZE"));
            assertEquals("1",env.get("TLOU_UNIFIED_GUEST_UI"));
            assertNull(env.get("TLOU_SPATIAL_SCENE"));
            assertEquals("0",env.get("GOW3_FEWER_DRAINS"));assertEquals("0",env.get("GOW3_SUBMIT_NO_WAIT"));
            assertEquals("1",env.get("GOW3_FRAMES_IN_FLIGHT"));assertEquals("1",env.get("GOW3_SKIP_IB_DRAIN"));
            var options=new org.json.JSONObject(new String(Files.readAllBytes(PortSettings.patchOptions(a).toPath()),java.nio.charset.StandardCharsets.UTF_8));
            assertEquals(50,options.getInt("resolution_scale"));assertEquals("original",options.getString("resolution"));
            p.edit().putInt("render_scale",100).putString("perf_overlap","999").commit();
            PortSettings.environment(a,env);assertNull(env.get("TLOU_UNIFIED_GUEST_UI"));assertNull(env.get("GOW3_MOVIE_SIZE"));
            assertEquals("0",env.get("GOW3_FRAMES_IN_FLIGHT"));
        }
    }
    @Test public void diagnosticsResetAndExportAreConsistent() throws Exception {
        try(var controller=Robolectric.buildActivity(Activity.class).setup()){
            Activity a=controller.get();var p=PortSettings.prefs(a);
            p.edit().putString("perf_diagnostics","gpu").putString("game_path","private-player-path").putString("driver_name","Test driver").commit();
            var env=new HashMap<String,String>();PortSettings.environment(a,env);assertFalse(env.containsKey("GOW3_GPU_PROFILE"));assertEquals("1",env.get("TLOU_CPU_PHASE_SAMPLES"));
            p.edit().putString("perf_diagnostics","normal").commit();PortSettings.environment(a,env);assertFalse(env.containsKey("GOW3_GPU_PROFILE"));
            RunLog.write(a,"test session");
            byte[] runtime=new byte[6*1024*1024];runtime[runtime.length-1]=123;
            RunLog.runtime(a).getParentFile().mkdirs();Files.write(RunLog.runtime(a).toPath(),runtime);
            java.io.File profiles=new java.io.File(a.getFilesDir(),"tlou/profiles");profiles.mkdirs();
            byte[] raw=new byte[5*1024*1024];raw[raw.length-1]=42;Files.write(new java.io.File(profiles,"test.profraw").toPath(),raw);
            var output=new ByteArrayOutputStream();RunLog.export(a,output);
            boolean settings=false,profile=false,tail=false;
            try(var zip=new ZipInputStream(new java.io.ByteArrayInputStream(output.toByteArray()))){
                java.util.zip.ZipEntry entry;
                while((entry=zip.getNextEntry())!=null){
                    byte[] data=zip.readAllBytes();
                    if(entry.getName().endsWith(".log"))assertTrue(data.length<=4*1024*1024);
                    if(entry.getName().equals("tlou/runtime.log")){assertEquals(4*1024*1024,data.length);assertEquals(123,data[data.length-1]);tail=true;}
                    if(entry.getName().equals("profiles/test.profraw")){assertArrayEquals(raw,data);profile=true;}
                    if(entry.getName().equals("performance-settings.json")){
                        String text=new String(data,java.nio.charset.StandardCharsets.UTF_8);assertFalse(text.contains("private-player-path"));assertTrue(text.contains("Test driver"));settings=true;
                    }
                }
            }
            assertTrue(settings && profile && tail);PerformanceControls.reset(p);assertFalse(p.contains("perf_diagnostics"));assertEquals("private-player-path",p.getString("game_path",""));
        }
    }
}
