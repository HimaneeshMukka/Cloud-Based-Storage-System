import core.FileSystem;

import java.util.Timer;
import java.util.TimerTask;

public class Main {
    public static void main(String[] args) {
        FileSystem fs = new FileSystem("/Users/skreweverything/client1/");
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("New files: " + fs.getAddedFiles());
                System.out.println("Deleted files: " + fs.getDeletedFiles());
                System.out.println("Modified files: " + fs.getModifiedFiles());
                fs.reloadCachedFiles();
            }
        }, 0, 10000);
    }
}
