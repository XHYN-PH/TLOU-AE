package ro.cobrabm.fexdroid;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.PowerManager;
import android.view.Display;
import android.view.WindowManager;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

/** Best-effort, bounded device evidence. Missing readings stay unknown. */
final class DiagnosticState {
    static String read(String path,int limit){
        try(FileInputStream in=new FileInputStream(path)){
            byte[] bytes=new byte[limit];int count=in.read(bytes);return count<0?"":new String(bytes,0,count,StandardCharsets.UTF_8).trim();
        }catch(Exception error){return "unavailable";}
    }
    static String presenter(Context c){
        if(RuntimeProcess.readPid(new File(c.getFilesDir(),"tlou/runtime.pid"))<1)return "not started";
        try{return Gow3Present.stats();}catch(LinkageError | RuntimeException error){return "unavailable";}
    }
    static String snapshot(Context c){
        StringBuilder result=new StringBuilder("TLOU-AE · XHYN_PH\n");
        result.append("Device: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append(" Android ").append(Build.VERSION.RELEASE).append(" API ").append(Build.VERSION.SDK_INT).append('\n');
        try{result.append("App: ").append(c.getPackageName()).append(' ').append(c.getPackageManager().getPackageInfo(c.getPackageName(),0).versionName).append('\n');}catch(Exception ignored){}
        try{
            ActivityManager.MemoryInfo memory=new ActivityManager.MemoryInfo();((ActivityManager)c.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryInfo(memory);
            result.append("RAM available/total MiB: ").append(memory.availMem>>20).append('/').append(memory.totalMem>>20).append(" low-memory=").append(memory.lowMemory).append('\n');
        }catch(RuntimeException error){result.append("RAM: unavailable\n");}
        try{PowerManager p=(PowerManager)c.getSystemService(Context.POWER_SERVICE);result.append("Thermal status: ").append(p.getCurrentThermalStatus()).append(" power-save=").append(p.isPowerSaveMode()).append(" sustained-supported=").append(p.isSustainedPerformanceModeSupported()).append('\n');}catch(RuntimeException error){result.append("Thermal status: unavailable\n");}
        try{Intent b=c.registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));if(b!=null)result.append("Battery temperature C: ").append((b.hasExtra(BatteryManager.EXTRA_TEMPERATURE)?String.valueOf(b.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0)/10.0):"unavailable")).append('\n');}catch(RuntimeException ignored){}
        try{Display d=((WindowManager)c.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();result.append("Display physical/app-visible Hz: ").append(d.getMode().getRefreshRate()).append('/').append(d.getRefreshRate()).append('\n');}catch(RuntimeException error){result.append("Display: unavailable\n");}
        result.append("GPU frequency Hz: ").append(read("/sys/class/kgsl/kgsl-3d0/devfreq/cur_freq",128)).append('\n');
        for(int cpu=0;cpu<Math.min(16,Runtime.getRuntime().availableProcessors());cpu++)result.append("CPU ").append(cpu).append(" kHz: ").append(read("/sys/devices/system/cpu/cpu"+cpu+"/cpufreq/scaling_cur_freq",64)).append('\n');
        result.append("Memory pressure: ").append(read("/proc/pressure/memory",2048)).append('\n');
        result.append("Presenter: ").append(presenter(c)).append('\n');
        return result.toString();
    }
}
