package utils;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by antonio on 18/02/17.
 */
class DataUtils {


    private static String OS = System.getProperty("os.name").toLowerCase();


    static long timeStringToMillis(String timestamp){
        Calendar c = Calendar.getInstance();
        if (timestamp.split(":").length == 2) {
            c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timestamp.split(":")[0])+1);
            c.set(Calendar.MINUTE, Integer.parseInt(timestamp.split(":")[1]));
            c.set(Calendar.YEAR, 1970);
            c.set(Calendar.MONTH, 0);
            c.set(Calendar.WEEK_OF_MONTH, 1);
            c.set(Calendar.DAY_OF_MONTH, 1);
            return c.getTimeInMillis();
        } else {
            System.out.println("Wrong Format, Retry");
            return 0;
        }
    }

    static void startSensor(String type){
        if (isWindows()) {
            try {
                Runtime.getRuntime().exec(new String[]{"cmd","/c","start","cmd","/k","java -jar Sensor.jar", type.toLowerCase() ,"localhost"});
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else if (isUnix()) {
            try {
                Runtime.getRuntime().exec(new String[]{"xterm","-e","java -jar Sensor.jar", type.toLowerCase() ,"localhost"});
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private static boolean isWindows() {
        return OS.contains("win");
    }


    private static boolean isUnix() {
        return OS.contains("nix") || OS.contains("nux") || OS.contains("aix") ;
    }
}
