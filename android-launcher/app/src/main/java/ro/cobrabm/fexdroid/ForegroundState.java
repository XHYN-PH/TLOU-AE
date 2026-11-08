package ro.cobrabm.fexdroid;

import android.content.Context;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/** A single lifecycle event reaches the child process without per-frame Java IPC. */
final class ForegroundState {
    private static Boolean last;
    static synchronized void publish(Context context,boolean active) {
        if(last!=null && last.booleanValue()==active)return;
        try {
            File target=new File(context.getFilesDir(),"tlou/foreground.state");
            Files.createDirectories(target.getParentFile().toPath());
            File temp=new File(target.getPath()+".new");long stamp=System.nanoTime();
            Files.write(temp.toPath(),((active?"1 ":"0 ")+stamp+"\n").getBytes(StandardCharsets.US_ASCII));
            Files.move(temp.toPath(),target.toPath(),StandardCopyOption.REPLACE_EXISTING);
            last=active;
            RunLog.write(context,"Foreground state: active="+active+" mono-ns="+stamp);
        }catch(Exception error){RunLog.write(context,"Foreground state unavailable: "+error.getClass().getSimpleName());}
    }
}
