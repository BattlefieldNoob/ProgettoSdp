package sensor;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import sensor.data.Event;
import sensor.data.Token;
import sensor.data.TokenLostEvent;
import sensor.utility.Logging;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by antonio on 27/05/16.
 * la classe tiene traccia dei SensorInputThread esistenti e agisce se si verificano 2 connessioni
 */
class SensorInputThread extends Thread {

    private boolean running = true;
    private ServerSocket server;
    private boolean shouldExit=false;
    private Logging log = Logging.getInstance();
    private static Gson gson=new Gson();
    private DataInputStream in;

    private Timer timer;

    SensorInputThread(int port) {
        try {
            server = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void stopMe() {
        running = false;
        shouldExit=true;
        try {
            server.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        DataOutputStream out;
        while (running) {
            try {
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        log.info("Input Timeout, sending TokenLostEvent",getClass().getSimpleName());
                        TokenLostEvent event=new TokenLostEvent(SensorHandle.sensorData);
                        SensorHandle.commonBuffer.push(event);
                    }
                },(4000 * SensorHandle.getNetworkSensorsSize()) + 3000);
                Socket prevSocket = server.accept();
                timer.cancel();
                timer.purge();
                in = new DataInputStream(prevSocket.getInputStream());
                out = new DataOutputStream(prevSocket.getOutputStream());
                out.writeUTF("OK");
                Object data = readAndParseInput();
                out.write(255);
                prevSocket.close();

                if (data != null) {
                    SensorHandle.commonBuffer.push(data);//aggiungo l'input letto al buffer comune
                }
            } catch (IOException e) {
                if (!shouldExit)
                    e.printStackTrace();
            }
        }
        log.info("Terminated", getClass().getSimpleName());
    }

    private Object readAndParseInput() {

        Object objectRead = null;
        if(!shouldExit) { //se il thread non deve chiudersi
            try {
                String message = in.readUTF(); //leggo l'input
                JsonObject data = new JsonParser().parse(message).getAsJsonObject();

                JsonPrimitive messageType=data.getAsJsonPrimitive("MessageType");
                JsonObject body=data.getAsJsonObject("Body");

                if (messageType.getAsString().equals("Event") && body.getAsJsonPrimitive("event").getAsString().equalsIgnoreCase("TOKENLOST")) {
                    //ho letto un object "TokenLost"
                    objectRead = gson.fromJson(body, TokenLostEvent.class);//creo la classe Event
                } else if (messageType.getAsString().equals("Event")) {
                    //ho letto un object "Event"
                    objectRead = gson.fromJson(body, Event.class);//creo la classe Event
                } else if (messageType.getAsString().equals("Token")) {
                    //ho letto un object "Token"
                    objectRead = gson.fromJson(body, Token.class);//creo la classe Token
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return objectRead;
    }
}
