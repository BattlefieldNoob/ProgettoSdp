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
class SensorInputThread extends Thread {

    boolean expectedException = false;
    private Sensor sensor;
    private DataInputStream in;
    private DataOutputStream out;
    private Socket socket;
    private boolean running = true;
    private Logging log = Logging.getInstance();

    public SensorInputThread(Sensor sensor, Socket socket) throws IOException {
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
                //socket.shutdownInput();
                //socket.shutdownOutput();
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
                String message;
                log.info("Waiting for messages...", getClass().getSimpleName());
                try {
                    message = in.readUTF();
                    out.write(255);//invio ACK
                    JsonObject json = new JsonParser().parse(message).getAsJsonObject();
                    if (json.getAsJsonPrimitive("MessageType").getAsString().contentEquals("Event")) {
                        //read a Event
                        sensor.setEvent(gson.fromJson(json.getAsJsonObject("Body"), Event.class));
                        log.info("Read a Event", getClass().getSimpleName());
                    } else if (json.getAsJsonPrimitive("MessageType").getAsString().contentEquals("Token")) {
                        //read a token
                        log.info("Read a Token", getClass().getSimpleName());
                        sensor.setToken(gson.fromJson(json.getAsJsonObject("Body"), Token.class));

                    }
                } catch (IOException e) {
                    System.out.println(expectedException);
                    if (expectedException) {
                        log.info("Expected Exception", getClass().getSimpleName());
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
