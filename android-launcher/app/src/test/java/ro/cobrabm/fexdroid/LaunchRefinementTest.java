package ro.cobrabm.fexdroid;

import android.app.Activity;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.zip.ZipInputStream;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=30,application=android.app.Application.class)
public class LaunchRefinementTest {
    @Test public void validatesOverridesAndRoutesThemToTheirActualProcess() throws Exception {
        assertThrows(IllegalArgumentException.class,()->LaunchOverrides.parse("bad line"));
        assertThrows(IllegalArgumentException.class,()->LaunchOverrides.parse("A=1\nA=2"));
        assertThrows(IllegalArgumentException.class,()->LaunchOverrides.parse("LD_LIBRARY_PATH=x"));
        assertThrows(IllegalArgumentException.class,()->LaunchOverrides.parse("GOW3_FEWER_DRAINS=1"));
        assertThrows(IllegalArgumentException.class,()->LaunchOverrides.parse("GOW3_READBACKS=2"));
        assertEquals("$(not-a-command)",LaunchOverrides.parse("CUSTOM=$(not-a-command)").get("CUSTOM"));
        try(var controller=Robolectric.buildActivity(Activity.class).setup()){
            Activity a=controller.get();var p=PortSettings.prefs(a);
            p.edit().putString("perf_overlap","2").putString("launch_overrides","# comments work\nSNAFU_RAW_ERROR_MESSAGES=1\nGOW3_FRAMES_IN_FLIGHT=0\nTLOU_FIXES=none\nPRIVATE_TOKEN=do-not-export").commit();
            var runtime=new HashMap<String,String>();PortSettings.environment(a,runtime);
            assertEquals("0",runtime.get("GOW3_FRAMES_IN_FLIGHT"));assertFalse(runtime.containsKey("SNAFU_RAW_ERROR_MESSAGES"));
            var tools=new HashMap<String,String>();LaunchOverrides.apply(a,tools,true);
            assertEquals("1",tools.get("SNAFU_RAW_ERROR_MESSAGES"));assertEquals("none",tools.get("TLOU_FIXES"));assertFalse(tools.containsKey("GOW3_FRAMES_IN_FLIGHT"));
            JSONObject options=new JSONObject(new String(Files.readAllBytes(PortSettings.patchOptions(a).toPath()),java.nio.charset.StandardCharsets.UTF_8));
            assertEquals("none",options.getString("fixes"));assertEquals("[redacted]",LaunchOverrides.report(a).get("PRIVATE_TOKEN"));
        }
    }
    @Test public void patchAndRuntimeSettingsRemainOneSnapshot() throws Exception {
        try(var controller=Robolectric.buildActivity(Activity.class).setup()){
            Activity a=controller.get();PortSettings.setScale(a,36);
            PortSettings.prefs(a).edit().putString("fps_limit","30").commit();
            LaunchSnapshot frozen=new LaunchSnapshot(a);PortSettings.setScale(a,67);
            PortSettings.prefs(a).edit().putString("fps_limit","60").commit();
            var env=new HashMap<String,String>();PortSettings.environment(frozen,env);
            assertEquals("704x396",env.get("TLOU_EXPECT_SCENE_SIZE"));assertEquals("30",env.get("GOW3_FPS_LIMIT"));
            JSONObject options=new JSONObject(new String(Files.readAllBytes(PortSettings.patchOptions(frozen).toPath()),java.nio.charset.StandardCharsets.UTF_8));
            assertEquals(36,options.getInt("resolution_scale"));assertEquals(67,PortSettings.scale(a));
            RunLog.captureActive(frozen,new File(a.getFilesDir(),"prepared"),env);
            ByteArrayOutputStream out=new ByteArrayOutputStream();RunLog.export(a,out);
            String active=null,desired=null;boolean device=false,readme=false;
            try(ZipInputStream zip=new ZipInputStream(new java.io.ByteArrayInputStream(out.toByteArray()))){
                java.util.zip.ZipEntry entry;while((entry=zip.getNextEntry())!=null){
                    String text=new String(zip.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
                    if(entry.getName().equals("tlou/active-settings.json"))active=text;
                    if(entry.getName().equals("performance-settings.json"))desired=text;
                    if(entry.getName().equals("device-state.txt"))device=text.contains("TLOU-AE");
                    if(entry.getName().equals("READ-ME.txt"))readme=text.contains("next launch");
                }
            }
            assertNotNull(active);assertNotNull(desired);assertTrue(device&&readme);
            assertEquals(36,new JSONObject(active).getInt("render_scale"));assertEquals(67,new JSONObject(desired).getInt("render_scale"));
        }
    }
    @Test public void legacyControlsMigrateWithoutLosingSelectedScale(){
        try(var controller=Robolectric.buildActivity(Activity.class).setup()){
            Activity a=controller.get();var p=PortSettings.prefs(a);
            p.edit().putString("resolution","540").putBoolean("custom_scale",true).putInt("render_scale",36).commit();
            PortSettings.migrateResolution(a);assertEquals(36,PortSettings.scale(a));assertEquals("original",p.getString("resolution",""));
            PortSettings.setScale(a,50);PortSettings.migrateResolution(a);assertEquals(50,PortSettings.scale(a));
        }
    }
    @Test public void cacheClearingPreservesSavesAndRefusesRunningOrRedirectedPaths() throws Exception {
        try(var controller=Robolectric.buildActivity(Activity.class).setup()){
            Activity a=controller.get();File user=new File(a.getFilesDir(),"tlou/user");File save=new File(user,"savedata/slot");save.getParentFile().mkdirs();Files.write(save.toPath(),new byte[]{1,2,3});
            File cache=CacheTools.root(a,false);cache.mkdirs();Files.write(new File(cache,"pipeline").toPath(),new byte[]{4});
            try(SaveArchive.Guard guard=SaveArchive.lock(user)){assertThrows(java.io.IOException.class,()->CacheTools.clear(a,false));}
            CacheTools.clear(a,false);assertFalse(cache.exists());assertArrayEquals(new byte[]{1,2,3},Files.readAllBytes(save.toPath()));
            Files.createSymbolicLink(cache.toPath(),user.toPath());assertThrows(java.io.IOException.class,()->CacheTools.clear(a,false));
            assertTrue(save.isFile());Files.delete(cache.toPath());
        }
    }
    @Test public void refreshRequestsUseCompatibleRatesAndEnglishRemainsSelected(){
        assertEquals(60,DisplayPacing.preferredRefresh(new float[]{30,60,120},30,true,30),.01);
        assertEquals(120,DisplayPacing.preferredRefresh(new float[]{60,120},60,true,30),.01);
        assertEquals(30,DisplayPacing.preferredRefresh(new float[]{30,60,120},30,true,34),.01);
        assertEquals(30,DisplayPacing.preferredRefresh(new float[]{30,60,120},30,false,30),.01);
        assertEquals(60,DisplayPacing.compatibleRefresh(new float[]{144,120,60},30),.01);
        assertEquals(30,DisplayPacing.compatibleRefresh(new float[]{120,30,60},30),.01);
        assertEquals(59.94,DisplayPacing.compatibleRefresh(new float[]{59.94f,120},60),.01);
        assertEquals(0,DisplayPacing.compatibleRefresh(new float[]{Float.NaN,49},60),.01);
        try(var controller=Robolectric.buildActivity(Activity.class).setup()){
            Activity a=controller.get();PortSettings.prefs(a).edit().putString("ui_language","es").commit();assertEquals(1,TlouLanguage.index(a));
        }
    }
}
