package ro.cobrabm.fexdroid;

import android.app.Activity;
import android.content.Context;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/** Only disposable caches are eligible; saves and game content are outside these roots. */
final class CacheTools {
    static File root(Context c,boolean jit){return new File(c.getFilesDir(),jit?"tlou/home/.cache/fex-emu":"tlou/cache");}
    static String describe(Context c){
        StringBuilder text=new StringBuilder();
        for(boolean jit:new boolean[]{false,true}){
            long bytes=0;int count=0;boolean bounded=false;Path root=root(c,jit).toPath();
            if(Files.isDirectory(root,LinkOption.NOFOLLOW_LINKS))try(var paths=Files.walk(root)){
                var iterator=paths.iterator();while(iterator.hasNext()){
                    Path p=iterator.next();if(++count>100000){bounded=true;break;}
                    if(Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS))bytes+=Files.size(p);
                }
            }catch(IOException e){text.append("Cache size unavailable: ").append(e.getClass().getSimpleName()).append('\n');}
            text.append(jit?"FEX JIT cache: ":"Renderer cache: ").append(bytes/(1024*1024)).append(" MiB").append(bounded?" (scan bounded)":"").append('\n');
        }
        String log=RunLog.tail(RunLog.runtime(c),32768);java.util.regex.Matcher m=java.util.regex.Pattern.compile("pending-draws=(\\d+)").matcher(log);String pending=null;
        while(m.find())pending=m.group(1);
        text.append("Shader-dependent draws pending: ").append(pending==null?"not reported":pending);
        return text.toString();
    }
    static void clear(Context c,boolean jit) throws IOException {
        int pid=RuntimeProcess.readPid(new File(c.getFilesDir(),"tlou/runtime.pid"));
        if(pid>0)try{android.system.Os.kill(pid,0);throw new IOException("Close the running game before clearing caches.");}
        catch(android.system.ErrnoException error){if(error.errno!=android.system.OsConstants.ESRCH)throw new IOException("Cannot verify that the game has stopped",error);}
        try(SaveArchive.Guard guard=SaveArchive.lock(new File(c.getFilesDir(),"tlou/user"))){
            File file=root(c,jit);Path parent=file.getParentFile().toPath();
            if(Files.isSymbolicLink(parent) || Files.isSymbolicLink(file.toPath()))throw new IOException("Cache path is not a regular private directory");
            Path privateRoot=c.getFilesDir().getCanonicalFile().toPath();
            if(!file.getCanonicalFile().toPath().equals(privateRoot.resolve(jit?"tlou/home/.cache/fex-emu":"tlou/cache")))throw new IOException("Cache path is outside app storage");
            InstallTools.delete(file);
        }catch(IOException e){throw new IOException("Close the game before clearing caches. "+e.getMessage(),e);}
    }
    static void controls(Activity a,LinearLayout root){
        PerformanceControls.text(a,root,"Shader and renderer caches");
        TextView info=new TextView(a);info.setTextColor(0xffe7e9df);root.addView(info);
        Runnable refresh=()->new Thread(()->{String result=describe(a);a.runOnUiThread(()->info.setText(result));},"tlou-cache-status").start();
        info.setText("Inspect caches to see size and the latest shader warmup status.");
        Button inspect=new Button(a);inspect.setText("Inspect caches and warmup");inspect.setOnClickListener(v->refresh.run());root.addView(inspect);
        PerformanceControls.text(a,root,"Warmup follows scenes you play. Valid caches are reused; this port cannot precompile shaders it has never encountered. Clearing caches can increase first-run stutter. No cache action changes saves or game files.");
        for(boolean jit:new boolean[]{false,true}){
            Button button=new Button(a);button.setText(jit?"Clear FEX JIT cache":"Clear renderer cache");
            button.setOnClickListener(v->new android.app.AlertDialog.Builder(a).setTitle("Clear disposable cache?")
                .setMessage("Close the game first. Shaders or translated code may need to compile again.")
                .setNegativeButton("Cancel",null).setPositiveButton("Clear",(dialog,which)->new Thread(()->{
                    try{clear(a,jit);a.runOnUiThread(refresh);}catch(IOException error){a.runOnUiThread(()->new android.app.AlertDialog.Builder(a).setTitle("Cache not cleared").setMessage(error.getMessage()).setPositiveButton("OK",null).show());}
                },"tlou-cache-clear").start()).show());root.addView(button);
        }
    }
}
