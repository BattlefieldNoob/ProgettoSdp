package sensor;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import sensor.data.Event;
import sensor.data.Token;
import sensor.utility.Logging;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Created by antonio on 11/05/16.
 * Thread che accetta le richieste dal nodo precedente
 */
public class SensorInputThread extends Thread {

    TestSensor sensor;
    DataInputStream in;
    DataOutputStream out;
    Socket socket;
    boolean expectedException = false;
    private boolean running = true;
    private String TAG = getClass().getSimpleName();
    private Logging log = Logging.getInstance();

    public SensorInputThread(TestSensor sensor, Socket socket) throws IOException {
        this.sensor = sensor;
        this.socket = socket;
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        start();
    }

    public void closeThread() {
        log.info("Closing All things", getClass().getSimpleName());
        expectedException = true;
        if (isAlive()) {
            try {
                running = false;

                socket.close();
                in.close();
                System.out.println("Socket Close");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
        Gson gson = new Gson();
        while (running) {
            synchronized (socket) {
                String message = "";
                log.info(TAG + " In attesa di cose...", getClass().getSimpleName());
                try {
                    message = in.readUTF();
                    out.write(255);//invio ACK
                    log.info(" letto :" + message, getClass().getSimpleName());
                    //ho letto qualcosa, la rendo disponibile e "avviso" outputThread
                    JsonObject json = new JsonParser().parse(message).getAsJsonObject();
                    if (json.getAsJsonPrimitive("MessageType").getAsString().contentEquals("Event")) {
                        //readed a ussaro
                        sensor.setEvent(gson.fromJson(json.getAsJsonObject("Body"), Event.class));
                        log.info("Letto un Event", getClass().getSimpleName());
                    } else if (json.getAsJsonPrimitive("MessageType").getAsString().contentEquals("Token")) {
                        //readed a token
                        log.info(" Setto il messaggio come 'da computare'", getClass().getSimpleName());
                        sensor.setToken(gson.fromJson(json.getAsJsonObject("Body"), Token.class));
                        log.info("Letto un Token", getClass().getSimpleName());
                    }
                } catch (IOException e) {
                    System.out.println(expectedException);
                    if (expectedException) {
                        log.info("Tutto ok", getClass().getSimpleName());
                        expectedException = false;
                        break;
                    } else {
                        log.error("Something WRONG!!!!", getClass().getSimpleName());
                        e.printStackTrace();
                        break;
                    }
                }
            }
        }
        log.info(" Terminated", getClass().getSimpleName());
    }
}
