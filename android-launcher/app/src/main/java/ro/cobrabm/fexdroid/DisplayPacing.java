package ro.cobrabm.fexdroid;

import android.app.Activity;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

/** PT4Droid-style bounded recovery; requests do not override thermal/display policy. */
final class DisplayPacing implements SurfaceHolder.Callback,DisplayManager.DisplayListener {
    private final Activity activity;
    private final SurfaceView view;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final DisplayManager displays;
    private int fps,attempts;
    private boolean fifo;
    private boolean resumed,registered;
    private long lastRecovery;
    private float lastPhysical=-1,lastVisible=-1,lastSurfaceRate=-1;
    private Surface lastSurface;
    private Boolean sustainedChoice;
    private final Runnable check=new Runnable(){public void run(){if(!resumed)return;apply("display-check",true);handler.postDelayed(this,2000);}};
    DisplayPacing(Activity activity,SurfaceView view,int fps){
        this.activity=activity;this.view=view;this.fps=fps;fifo=PortSettings.prefs(activity).getBoolean("vsync",true);
        displays=(DisplayManager)activity.getSystemService(Activity.DISPLAY_SERVICE);
        view.getHolder().addCallback(this);
    }
    static float compatibleRefresh(float[] rates,int fps){
        if(fps<1)return 0;float best=0;
        for(float hz:rates){
            int multiple=Math.round(hz/fps);
            if(Float.isFinite(hz) && hz>=fps-0.5f && multiple>=1 && Math.abs(hz-multiple*fps)<0.5f && (best==0 || hz<best))best=hz;
        }
        return best;
    }
    static float preferredRefresh(float[] rates,int fps,boolean fifo,int sdk){
        // API 30 has no transaction-onCommit callback: FIFO retirement can take
        // two refreshes. Do not introduce a 30 Hz -> 15 FPS regression there.
        if(fifo && sdk<=30){
            float[] faster=new float[rates.length];int count=0;
            for(float rate:rates)if(rate>=fps*2-0.5f)faster[count++]=rate;
            float selected=compatibleRefresh(java.util.Arrays.copyOf(faster,count),fps);
            if(selected>0)return selected;
        }
        return compatibleRefresh(rates,fps);
    }
    static float selectedRefresh(float[] rates,int fps,boolean fifo,int sdk,String policy){
        if("system".equals(policy))return 0;
        if(!"match".equals(policy))try{
            float requested=Float.parseFloat(policy);
            for(float rate:rates)if(Math.abs(rate-requested)<0.5f && rate>=fps-0.5f &&
                    !(fifo && sdk<=30 && rate<fps*2-0.5f))return rate;
        }catch(NumberFormatException ignored){}
        return preferredRefresh(rates,fps,fifo,sdk);
    }
    void setTarget(int target,boolean fifo){if(target!=fps || fifo!=this.fifo){fps=target;this.fifo=fifo;attempts=0;lastSurfaceRate=-1;apply("launch-target",false);}}

    void resume(){
        if(resumed)return;resumed=true;
        if(displays!=null&&!registered){displays.registerDisplayListener(this,handler);registered=true;}
        apply("resume",false);handler.postDelayed(check,2000);
    }
    void pause(){
        resumed=false;handler.removeCallbacks(check);
        if(displays!=null&&registered){displays.unregisterDisplayListener(this);registered=false;}
        if(Boolean.TRUE.equals(sustainedChoice)){activity.getWindow().setSustainedPerformanceMode(false);sustainedChoice=false;}
        Gow3Present.active(false);
        ForegroundState.publish(activity,false);
    }
    void focus(boolean focused){
        Gow3Present.active(resumed&&focused);
        ForegroundState.publish(activity,resumed&&focused);
        if(focused){attempts=0;lastSurfaceRate=-1;apply("focus-returned",false);}
    }
    void apply(String reason,boolean periodic){
        if(activity.isFinishing() || activity.isDestroyed())return;
        String policy=PortSettings.prefs(activity).getString("perf_display_rate","match");
        boolean match=!"system".equals(policy);
        boolean recovery=false,changed=false;
        try{
            Display display=view.getDisplay();float rate=0;int modeId=0;
            if(display!=null){
                float physical=display.getMode().getRefreshRate(),visible=display.getRefreshRate();
                changed=Math.abs(physical-lastPhysical)>0.5f || Math.abs(visible-lastVisible)>0.5f;
                if(changed){attempts=0;lastPhysical=physical;lastVisible=visible;}
                if(match){
                    Display.Mode current=display.getMode();java.util.ArrayList<Float> rates=new java.util.ArrayList<>();
                    for(Display.Mode mode:display.getSupportedModes())if(mode.getPhysicalWidth()==current.getPhysicalWidth() && mode.getPhysicalHeight()==current.getPhysicalHeight())rates.add(mode.getRefreshRate());
                    float[] values=new float[rates.size()];for(int i=0;i<values.length;i++)values[i]=rates.get(i);
                    rate=selectedRefresh(values,fps,fifo,Build.VERSION.SDK_INT,policy);
                    if(rate>0)for(Display.Mode mode:display.getSupportedModes())if(mode.getPhysicalWidth()==current.getPhysicalWidth() && mode.getPhysicalHeight()==current.getPhysicalHeight() && Math.abs(mode.getRefreshRate()-rate)<0.1f){modeId=mode.getModeId();break;}
                }
                boolean mismatch=match&&rate>0&&(Math.abs(physical-rate)>0.5f || visible<Math.min(fps,rate)-0.5f);
                if(!mismatch)attempts=0;
                long now=SystemClock.uptimeMillis();
                recovery=mismatch&&resumed&&activity.hasWindowFocus()&&attempts<3&&now-lastRecovery>=2000;
                if(recovery){attempts++;lastRecovery=now;lastSurfaceRate=-1;}
            }
            WindowManager.LayoutParams attrs=activity.getWindow().getAttributes();
            if(recovery || attrs.preferredDisplayModeId!=modeId || Math.abs(attrs.preferredRefreshRate-rate)>0.1f){attrs.preferredDisplayModeId=modeId;attrs.preferredRefreshRate=rate;activity.getWindow().setAttributes(attrs);}
            Surface surface=view.getHolder().getSurface();float contentRate=match?fps:0;
            if(surface!=null && surface.isValid() && (surface!=lastSurface || contentRate!=lastSurfaceRate)){
                if(Build.VERSION.SDK_INT>=31)surface.setFrameRate(contentRate,Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE,Surface.CHANGE_FRAME_RATE_ONLY_IF_SEAMLESS);
                else surface.setFrameRate(contentRate,Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE);
                lastSurface=surface;lastSurfaceRate=contentRate;
                Gow3Present.frameRate(contentRate);
            }
            Gow3Present.active(resumed&&activity.hasWindowFocus());
            ForegroundState.publish(activity,resumed&&activity.hasWindowFocus());
            PowerManager power=(PowerManager)activity.getSystemService(Activity.POWER_SERVICE);
            boolean sustained=resumed&&PortSettings.prefs(activity).getBoolean("perf_sustained",false)&&power!=null&&power.isSustainedPerformanceModeSupported();
            if(sustainedChoice==null || sustainedChoice.booleanValue()!=sustained){activity.getWindow().setSustainedPerformanceMode(sustained);sustainedChoice=sustained;}
        }catch(RuntimeException | LinkageError error){if(!periodic)RunLog.write(activity,"Display pacing unavailable: "+error.getClass().getSimpleName());}
        if(!periodic || recovery || changed)RunLog.write(activity,"Display event "+reason+": "+state()+" recovery-attempt="+attempts);
    }
    String state(){
        Display display=view.getDisplay();
        return "mono-ns="+System.nanoTime()+" target="+fps+" policy="+PortSettings.prefs(activity).getString("perf_display_rate","match")+" physical-Hz="+(display==null?"unknown":display.getMode().getRefreshRate())+
            " app-Hz="+(display==null?"unknown":display.getRefreshRate())+" focused="+activity.hasWindowFocus()+
            " multiwindow="+activity.isInMultiWindowMode()+" view="+view.getWidth()+"x"+view.getHeight();
    }
    public void surfaceCreated(SurfaceHolder holder){lastSurfaceRate=-1;apply("surface-created",false);}
    public void surfaceChanged(SurfaceHolder holder,int format,int width,int height){apply("surface-changed",false);}
    public void surfaceDestroyed(SurfaceHolder holder){lastSurface=null;Gow3Present.active(false);ForegroundState.publish(activity,false);RunLog.write(activity,"Display event surface-destroyed");}
    public void onDisplayAdded(int id){}
    public void onDisplayRemoved(int id){}
    public void onDisplayChanged(int id){Display d=view.getDisplay();if(d!=null&&d.getDisplayId()==id)apply("display-changed",false);}
    void close(){pause();view.getHolder().removeCallback(this);}
}
