package ro.cobrabm.fexdroid;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Durable launch breadcrumbs exist before JNI, AHB or the FEX child is started. */
final class RunLog {
    static final int EXPORT_REQUEST = 6509;
    static void requestExport(Activity activity) {
        Intent save=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        save.setType("application/zip");save.addCategory(Intent.CATEGORY_OPENABLE);
        save.putExtra(Intent.EXTRA_TITLE,"TLOU-Android-Logs.zip");
        activity.startActivityForResult(save,EXPORT_REQUEST);
    }
    static File launch(Context c) { return new File(c.getFilesDir(),"launch.log"); }
    static synchronized void begin(Context c) {
        File current=launch(c), previous=new File(c.getFilesDir(),"launch-previous.log");
        if(current.exists()) { previous.delete(); current.renameTo(previous); }
        write(c,"JOGAR • "+new Date()+" • Android "+Build.VERSION.RELEASE+" / API "+Build.VERSION.SDK_INT+" • "+Build.MANUFACTURER+" "+Build.MODEL);
    }
    static synchronized void write(Context c,String message) {
        try(FileOutputStream out=new FileOutputStream(launch(c),true)) {
            out.write((new Date()+" ["+Thread.currentThread().getName()+"] "+message+"\n").getBytes(StandardCharsets.UTF_8));
            out.getFD().sync();
        } catch(IOException e) { android.util.Log.e("TLOU","Cannot write launch log",e); }
    }
    static void failure(Context c,String label,Throwable error) {
        StringWriter trace=new StringWriter(); error.printStackTrace(new PrintWriter(trace));
        write(c,label+"\n"+trace);
    }
    static File runtime(Context c) { return new File(c.getFilesDir(),"tlou/runtime.log"); }
    static String tail(File file,int bytes) {
        if(!file.isFile())return "Ainda não criado.";
        try(RandomAccessFile in=new RandomAccessFile(file,"r")) {
            in.seek(Math.max(0,in.length()-bytes)); byte[] data=new byte[(int)(in.length()-in.getFilePointer())];in.readFully(data);return new String(data,StandardCharsets.UTF_8);
        }catch(IOException e){return e.toString();}
    }
    static String summary(Context c) {
        return "INICIALIZAÇÃO\n"+tail(launch(c),10000)+"\n\nRUNTIME\n"+tail(runtime(c),10000)+
            "\n\nINSTALAÇÃO / PREPARAÇÃO\n"+tail(new File(c.getFilesDir(),"installation.log"),5000);
    }
    static void export(Context c,OutputStream output) throws IOException {
        try(ZipOutputStream zip=new ZipOutputStream(output)) {
            for(String path:new String[]{"launch.log","launch-previous.log","tlou/runtime.log","tlou/runtime-previous.log","installation.log","tlou-prepared/device-ready.json","tlou/perf-cs-22e6e19e.bin","tlou/perf-cs-61d67259.bin"}) {
                File file=new File(c.getFilesDir(),path);if(!file.isFile())continue;
                zip.putNextEntry(new ZipEntry(path));
                try(RandomAccessFile in=new RandomAccessFile(file,"r")) {
                    // Snapshot the extent: a running game must not grow an export forever.
                    long end=in.length(),start=Math.max(0,end-4L*1024*1024),remaining=end-start;
                    in.seek(start);byte[] bytes=new byte[65536];int n;
                    while(remaining>0 && (n=in.read(bytes,0,(int)Math.min(remaining,bytes.length)))!=-1){zip.write(bytes,0,n);remaining-=n;}
                }
                zip.closeEntry();
            }
            // Training builds only. Never truncate a binary profile or copy one from a live child.
            int pid=RuntimeProcess.readPid(new File(c.getFilesDir(),"tlou/runtime.pid"));boolean running=false;
            if(pid>0)try{android.system.Os.kill(pid,0);running=true;}catch(android.system.ErrnoException ignored){}
            if(!running){
                File[] profiles=new File(c.getFilesDir(),"tlou/profiles").listFiles((dir,name)->name.endsWith(".profraw"));
                if(profiles!=null){
                    java.util.Arrays.sort(profiles,java.util.Comparator.comparingLong(File::lastModified).reversed());
                    for(int index=0;index<Math.min(8,profiles.length);index++){
                        File file=profiles[index];if(!file.isFile() || file.length()>64L*1024*1024)continue;
                        zip.putNextEntry(new ZipEntry("profiles/"+file.getName()));
                        try(InputStream in=new FileInputStream(file)){byte[] buffer=new byte[65536];int n;while((n=in.read(buffer))!=-1)zip.write(buffer,0,n);}
                        zip.closeEntry();
                    }
                }
            }
            zip.putNextEntry(new ZipEntry("performance-settings.json"));
            org.json.JSONObject settings=new org.json.JSONObject();
            for(java.util.Map.Entry<String,?> entry:PortSettings.prefs(c).getAll().entrySet()) {
                String key=entry.getKey();
                if(key.startsWith("perf_") || key.equals("resolution") || key.equals("custom_scale") ||
                   key.equals("render_scale") || key.equals("fps_limit") || key.equals("driver_name") ||
                   key.equals("vsync") || java.util.Arrays.asList(PortSettings.EFFECTS).contains(key)) {
                    try{settings.put(key,entry.getValue());}catch(org.json.JSONException e){throw new IOException(e);}
                }
            }
            zip.write(settings.toString().getBytes(StandardCharsets.UTF_8));zip.closeEntry();
            zip.putNextEntry(new ZipEntry("android-process-exits.txt"));
            try {
                ActivityManager manager=(ActivityManager)c.getSystemService(Context.ACTIVITY_SERVICE);
                if(manager!=null)for(ApplicationExitInfo exit:manager.getHistoricalProcessExitReasons(c.getPackageName(),0,5)) {
                    String line=new Date(exit.getTimestamp())+" process="+exit.getProcessName()+" reason="+exit.getReason()+" status="+exit.getStatus()+" description="+exit.getDescription()+"\n";
                    zip.write(line.getBytes(StandardCharsets.UTF_8));
                }
            } catch(RuntimeException e) { zip.write(e.toString().getBytes(StandardCharsets.UTF_8)); }
            zip.closeEntry();
        }
    }
}
