package sensor.utility;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by antonio on 26/05/16.
 */
public class Logging {

    private static Logging instance;
    private String id, type;

    private Logging() {
    }

    private Logging(String id, String type) {
        this.id = id;
        this.type = type;
    }

    public static Logging getInstance() {
        if (instance == null) {
            System.err.println("Logging non è stata inizializzata, Impossibile utilizzarla");
            instance = new Logging();
        }
        return instance;
    }

    public static Logging getInstance(String ID, String type) {
        if (instance == null) {
            instance = new Logging(ID, type);
        } else {
            System.err.println("Logging è stata già inizializzata, nelle prossime richieste considera l'utilizzo di getInstance()");
        }
        return instance;
    }

    public void info(String message, String classname) {
        if (id == null || id.isEmpty() || type == null || type.isEmpty())
            System.out.println("[" + new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()) + "," + classname + "] -" + message);
        else
            System.out.println("[" + id + ", " + type + ", " + new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()) + "," + classname + "] - " + message);
    }

    public void error(String message, String classname) {
        if (id == null || id.isEmpty() || type == null || type.isEmpty())
            System.err.println("[" + new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()) + "," + classname + "] -" + message);
        else
            System.err.println("[" + id + ", " + type + ", " + new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()) + "," + classname + "]- " + message);
    }

}
