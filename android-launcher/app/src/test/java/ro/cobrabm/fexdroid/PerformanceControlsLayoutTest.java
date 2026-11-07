package ro.cobrabm.fexdroid;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import java.io.File;
import java.io.FileOutputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=30, application=android.app.Application.class, qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class PerformanceControlsLayoutTest {
    private View find(View view,String label) {
        if(view instanceof android.widget.TextView && ((android.widget.TextView)view).getText().toString().equals(label))return view;
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){View found=find(group.getChildAt(i),label);if(found!=null)return found;}}
        return null;
    }
    @Test public void settingsFitAndPersistUserActions() throws Exception {
        try(var controller=Robolectric.buildActivity(Activity.class).setup()){
            Activity a=controller.get();LinearLayout root=new LinearLayout(a);root.setOrientation(LinearLayout.VERTICAL);
            root.setPadding(16,16,16,16);root.setBackgroundColor(0xff10161a);
            PerformanceControls.resolution(a,root,()->{});PerformanceControls.advanced(a,root,()->{});
            Switch enabled=null;SeekBar scale=null;
            for(int i=0;i<root.getChildCount();i++){
                View child=root.getChildAt(i);
                if(child instanceof Switch && enabled==null)enabled=(Switch)child;
                if(child instanceof SeekBar)scale=(SeekBar)child;
            }
            assertNotNull(enabled);assertNotNull(scale);assertEquals(90,scale.getMax());
            enabled.performClick();assertTrue(PortSettings.prefs(a).getBoolean("custom_scale",false));
            View warm=find(root,"Keep recently used textures warm");assertNotNull(warm);warm.performClick();
            assertTrue(PortSettings.prefs(a).getBoolean("perf_warm_textures",false));
            find(root,"Reset advanced performance settings").performClick();
            assertFalse(((Switch)find(root,"Keep recently used textures warm")).isChecked());
            assertFalse(PortSettings.prefs(a).contains("perf_warm_textures"));
            assertTrue(PortSettings.prefs(a).getBoolean("custom_scale",false));
            root.measure(View.MeasureSpec.makeMeasureSpec(480,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
            root.layout(0,0,480,root.getMeasuredHeight());
            for(int i=0;i<root.getChildCount();i++)assertTrue(root.getChildAt(i).getRight()<=480);
            Bitmap bitmap=Bitmap.createBitmap(480,root.getMeasuredHeight(),Bitmap.Config.ARGB_8888);root.draw(new Canvas(bitmap));
            File file=new File("build/reports/performance-controls.png");file.getParentFile().mkdirs();
            try(FileOutputStream out=new FileOutputStream(file)){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}
        }
    }
}
