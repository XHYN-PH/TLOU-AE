package ro.cobrabm.fexdroid;

import android.app.Activity;
import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=34)
public final class PerformanceUpdateTest {
    @Test public void bcModesAndExperimentsUseOneSnapshotWithExplicitOverridePrecedence(){
        try(var c=Robolectric.buildActivity(Activity.class).setup()){
            Activity a=c.get();var p=PortSettings.prefs(a);p.edit().clear().commit();
            for(String mode:new String[]{"auto","off","force"}){
                p.edit().putString("perf_bc_decode",mode).commit();var env=new HashMap<String,String>();PortSettings.environment(a,env);
                assertEquals(mode.equals("off")?"0":mode.equals("force")?"1":"auto",env.get("TLOU_BC_DECODE"));
                assertEquals("0",env.get("TLOU_SRT_RETRY"));assertEquals("immediate",env.get("GOW3_RAYCAST_FLUSH"));
                assertFalse(env.containsKey("GOW3_ASYNC_READBACK"));assertEquals("1",env.get("GOW3_EOP_READBACK"));
            }
            p.edit().putString("launch_overrides","TLOU_BC_DECODE=0\nGOW3_RAYCAST_FLUSH=lazy\nTLOU_SRT_RETRY=1").commit();
            var frozen=new LaunchSnapshot(a);p.edit().remove("launch_overrides").putString("perf_bc_decode","auto").commit();
            var env=new HashMap<String,String>();PortSettings.environment(frozen,env);
            assertEquals("0",env.get("TLOU_BC_DECODE"));assertEquals("lazy",env.get("GOW3_RAYCAST_FLUSH"));assertEquals("1",env.get("TLOU_SRT_RETRY"));
        }
    }
    @Test public void invalidModesAndManagedForegroundChannelAreRejected(){
        for(String text:new String[]{"TLOU_BC_DECODE=wrong","TLOU_SRT_RETRY=3","GOW3_RAYCAST_FLUSH=skip","TLOU_FOCUS_STATE=/tmp/other"})
            assertThrows(IllegalArgumentException.class,()->LaunchOverrides.parse(text));
    }
    @Test public void reducedOutputPreservesPhysicalAspectAndLeavesRenderChoiceAlone(){
        assertArrayEquals(new int[]{2293,1080},PresentationSize.dimensions(2293,1080,396,false));
        int[] small=PresentationSize.dimensions(2293,1080,396,true);
        assertEquals(396,small[1]);assertEquals(2293.0/1080,1.0*small[0]/small[1],.005);
        int[] minimum=PresentationSize.dimensions(2293,1080,108,true);assertTrue(minimum[0]>=320&&minimum[1]>=180);
        try(var c=Robolectric.buildActivity(Activity.class).setup()){
            Activity a=c.get();PortSettings.prefs(a).edit().clear().putString("perf_output_size","scene").commit();
            var env=new HashMap<String,String>();env.put("GOW3_SCREEN_W","2293");env.put("GOW3_SCREEN_H","1080");env.put("TLOU_EXPECT_SCENE_SIZE","704x396");
            PresentationSize.apply(a,env);assertEquals("704x396",env.get("TLOU_EXPECT_SCENE_SIZE"));assertEquals("396",env.get("GOW3_SCREEN_H"));
        }
    }
    @Test public void refreshSelectionIsIndependentAndKeepsAndroid11FifoSafeguard(){
        float[] rates={60,90,120};
        assertEquals(90,DisplayPacing.selectedRefresh(rates,30,true,34,"90"),.01);
        assertEquals(120,DisplayPacing.selectedRefresh(rates,60,true,34,"120"),.01);
        assertEquals(60,DisplayPacing.selectedRefresh(rates,30,true,34,"144"),.01);
        assertEquals(120,DisplayPacing.selectedRefresh(rates,60,true,30,"60"),.01);
        assertEquals(0,DisplayPacing.selectedRefresh(rates,60,true,34,"system"),.01);
    }
    @Test public void foregroundMessagesHaveOneMonotonicTimestampAndAtomicContent() throws Exception {
        try(var c=Robolectric.buildActivity(Activity.class).setup()){
            Activity a=c.get();ForegroundState.publish(a,false);ForegroundState.publish(a,true);
            String[] state=new String(Files.readAllBytes(new File(a.getFilesDir(),"tlou/foreground.state").toPath()),java.nio.charset.StandardCharsets.US_ASCII).trim().split(" ");
            assertEquals(2,state.length);assertEquals("1",state[0]);assertTrue(Long.parseLong(state[1])>0);
            assertFalse(new File(a.getFilesDir(),"tlou/foreground.state.new").exists());
        }
    }
}
