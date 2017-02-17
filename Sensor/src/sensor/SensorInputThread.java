package sensor;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import sensor.data.Event;
import sensor.data.Token;
import sensor.utility.Logging;
import server.data.SensorData;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

/**
 * Created by antonio on 27/05/16.
 * la classe tiene traccia dei SensorInputThread esistenti e agisce se si verificano 2 connessioni
 */
class SensorServerThread extends Thread {

    LinkedList<SensorInputThread> inputThreads = new LinkedList<>();
    private boolean running = true;
    private ServerSocket server;
 //   private SensorHandle sensor;
 public boolean shouldExit=false;
    private Logging log = Logging.getInstance();
    private static Gson gson=new Gson();
    private DataInputStream in;
    private DataOutputStream out;


    SensorServerThread(int port) {
 //       this.sensor = sensor;
        try {
            server = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void stopMe() {
        running = false;
        inputThreads.get(0).closeThread();
        inputThreads.get(0).interrupt();
        System.out.println("Wait for input thread to close");
        try {
            inputThreads.get(0).join();
        } catch (InterruptedException e) {
            System.out.println("Thread Closed");
        }
        inputThreads.clear();
        try {
            System.out.println("Closing Server");
            server.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (running) {
            //non è connesso, quindi mi metto in ascolto
            log.info("Waiting for connections...", getClass().getSimpleName());
            try {
                Socket prevSocket = server.accept();
                in=new DataInputStream(prevSocket.getInputStream());
                out=new DataOutputStream(prevSocket.getOutputStream());
                out.writeUTF("OK");
                log.info("Waiting for messages...", getClass().getSimpleName());
                Object data = readAndParseInput();
                out.write(255);
                prevSocket.close();
                if (data != null) {
                    SensorHandle.commonBuffer.push(data);//aggiungo l'input letto al buffer comune
                }
            } catch (IOException e) {
                log.info("Exception OK", getClass().getSimpleName());
            }
        }
        log.info("Terminated", getClass().getSimpleName());
    }

    public void stopThreads(){
        for (SensorInputThread in : inputThreads){
            in.shouldExit=true;
        }
    }

    private Object readAndParseInput() {

        Object objectRead = null;
        if(!shouldExit) { //se il thread non deve chiudersi
            try {
                String message = in.readUTF(); //leggo l'input
                log.info("Received message : " + message, getClass().getSimpleName());
                //     out.write(255);//invio ACK
                JsonObject data = new JsonParser().parse(message).getAsJsonObject();

                JsonPrimitive messageType=data.getAsJsonPrimitive("MessageType");
                JsonObject body=data.getAsJsonObject("Body");

                if (messageType.getAsString().equals("Event")) {
                    //ho letto un object "Event"
                    objectRead = gson.fromJson(body, Event.class);//creo la classe Event
                    log.info("Read a Event", getClass().getSimpleName());
                } else if (messageType.getAsString().equals("Token")) {
                    //ho letto un object "Token"
                    log.info("Read a Token", getClass().getSimpleName());
                    objectRead = gson.fromJson(body, Token.class);//creo la classe Token
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return objectRead;
    }
}
