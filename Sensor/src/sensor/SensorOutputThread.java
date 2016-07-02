package sensor;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import sensor.utility.Logging;
import server.data.SensorData;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Created by antonio on 11/05/16.
 */
class SensorOutputThread extends Thread {
    boolean expectedException = false;
    private DataOutputStream out;
    private DataInputStream in;
    private Socket nextSocket;
    private Sensor sensor;
    private boolean running = true;
    private Gson gson = new Gson();
    private Object lock;
    private Logging log = Logging.getInstance();
    private boolean lastSendToCurrentNext = false;
    private SensorData nextSensor;


    SensorOutputThread(Sensor thisSensor, Object lockObject) throws IOException {
        this.lock = lockObject;
        this.sensor = thisSensor;
    }

    private void resetSocket() throws IOException {
        if (nextSocket != null) {
            synchronized (nextSocket) {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
                if (!nextSocket.isClosed()) {
                    nextSocket.close();
                }
            }
        }
        nextSocket = null;
        out = null;
        in = null;
    }

    void configureConnectionWithNext(SensorData next) throws InterruptedException, IOException {
        nextSensor = next;
        if (next == null) {
            lastSendToCurrentNext = true;
        } else {
            log.info("Next Sensor:" + next.getAddress() + ":" + next.getPort(), getClass().getSimpleName());
            if (nextSocket != null && !nextSocket.isClosed() && nextSocket.getInetAddress().equals(InetAddress.getByName(next.getAddress())) && nextSocket.getPort() == next.getPort()) {
                log.info("Already connected to next", getClass().getSimpleName());
            } else {
                lastSendToCurrentNext = true;
            }
        }
    }

    void stopMe() {
        running = false;
        expectedException = true;
        synchronized (lock) {
            lock.notify();
        }
    }


    @Override
    public void run() {
        while (running || sensor.isDataAvailable() && !sensor.isSendToMyself()) {//se running e sono disponibili info
            if (!running) {
                log.info("Still Sending Token", getClass().getSimpleName());
            }
            try {
                if (sensor.isSendToMyself()) {
                    log.info("Send Token to myself", getClass().getSimpleName());
                    sensor.computeToken();
                }
                synchronized (lock) {
                    if (nextSocket != null) {
                        if (out != null && !nextSocket.isClosed()) {
                            if (sensor.eventBuffer.isEmpty() && !sensor.isDataAvailable()) {// se non è disponibile qualcosa da inviare, attendo che lo sia
                                log.info("Waiting for data", getClass().getSimpleName());
                                lock.wait();
                            }
                            synchronized (nextSocket) {
                                log.info("send data to " + nextSocket.getInetAddress().toString() + " port " + nextSocket.getPort(), getClass().getSimpleName());
                                JsonObject json = new JsonObject();
                                synchronized (sensor.eventBuffer) {
                                    if (!sensor.eventBuffer.isEmpty()) {
                                        log.info("Sending Event to next", getClass().getSimpleName());
                                        json.addProperty("MessageType", "Event");
                                        json.add("Body", gson.toJsonTree(sensor.eventBuffer.element()));
                                        sensor.eventBuffer.remove();
                                        out.writeUTF(json.toString());
                                        in.read();
                                    }
                                }
                                if (sensor.isDataAvailable() && !lastSendToCurrentNext) {
                                    synchronized (sensor.token) {
                                        //invia il token
                                        log.info("Sending Token to next", getClass().getSimpleName());
                                        json.addProperty("MessageType", "Token");
                                        sensor.computeToken();
                                        json.add("Body", gson.toJsonTree(sensor.token));
                                        sensor.token = null;
                                        out.writeUTF(json.toString());
                                        in.read();//aspetto l'ok
                                    }
                                }
                                if (lastSendToCurrentNext) {
                                    log.info("Next is exiting, Don't send to him the token", getClass().getSimpleName());
                                }

                            }
                        }
                    }
                    if (lastSendToCurrentNext) { // mi assicuro che non ci siano event da inviare

                        if (nextSensor == null) {
                            resetSocket();
                            lastSendToCurrentNext = false;
                        } else {
                            log.info("Trying connection with next", getClass().getSimpleName());
                            boolean connected = false;
                            while (!connected) {
                                try {
                                    nextSocket = new Socket(nextSensor.getAddress(), nextSensor.getPort());
                                    out = new DataOutputStream(nextSocket.getOutputStream());
                                    in = new DataInputStream(nextSocket.getInputStream());
                                    log.info("Connected to nextSocket", getClass().getSimpleName());
                                    connected = true;
                                } catch (IOException e) {
                                    log.info("Not connected, retry", getClass().getSimpleName());
                                    sleep(1000);
                                }
                            }
                            lastSendToCurrentNext = false;
                        }
                    }
                }
            } catch (InterruptedException | IOException e) {
                if (expectedException) {
                    log.info("Expected Exception", getClass().getSimpleName());
                    expectedException = false;
                } else {
                    log.error("Something WRONG!!!!", getClass().getSimpleName());
                    e.printStackTrace();
                }
            }
        }
        log.info(getClass().getSimpleName() + " Terminated", getClass().getSimpleName());
    }
}
