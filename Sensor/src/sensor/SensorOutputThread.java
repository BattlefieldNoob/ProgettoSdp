package sensor;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import sensor.data.Event;
import sensor.data.Token;
import sensor.utility.Logging;
import server.data.SensorData;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Created by antonio on 11/05/16.
 */
class SensorOutputThread extends Thread {
    private DataOutputStream out;
    private DataInputStream in;
    private Socket nextSocket;
    private SensorHandle sensor;
    private boolean running = true;
    private final Object lock;
    private Logging log = Logging.getInstance();
    private Gson gson = new Gson();
    private SensorData sensorToDelete=null;
    private boolean waitForInsertReturn=false;
    private Object toElaborate;

    SensorOutputThread(SensorHandle thisSensor, Object lockObject) throws IOException {
        this.lock = lockObject;
        this.sensor = thisSensor;
    }


    private boolean connectWithNext() throws IOException {
        boolean connected = false;
        SensorData next = sensor.getNextSensor();
        nextSocket = new Socket(next.getAddress(), next.getPort());//connetto con il successivo
        out = new DataOutputStream(nextSocket.getOutputStream());//ottengo gli stream
        in = new DataInputStream(nextSocket.getInputStream());
        //attendo che il successivo mi invii il messaggio "OK"
        System.out.println("Wait for OK from next");
        String msg = in.readUTF();
        if (msg.equals("OK"))
            connected = true;
        if (!connected) {
            in = null;
            out = null;
            if (nextSocket != null)
                try {
                    nextSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        return connected;
    }

    void stopMe() {
        running = false;
        synchronized (lock) {
            lock.notify();
        }
    }


    private Object getNextDataToElaborate() {
        Object data = SensorHandle.commonBuffer.pop(); //ottengo il primo oggetto data disponibile
        if (data != null)
            SensorHandle.commonBuffer.remove();
        return data;
    }


    @Override
    public void run() {
        while (running) {//se running e sono disponibili info
            try {
                if(running) {
                    if (!SensorHandle.commonBuffer.isDataAvailable()) {// se non è disponibile qualcosa da inviare, attendo che lo sia
                        log.info("Waiting for data from:"+sensor.getNextSensor(), getClass().getSimpleName());
                        synchronized (lock) {
                            lock.wait(); //interrompo il thread finchè non ci sono dati
                        }
                    }
                    //mi connetto con il successivo
                    Object dataFromPrev = getNextDataToElaborate(); //ottengo i dati da elaborare
                    if (dataFromPrev != null) {
                        toElaborate = elaborateData(dataFromPrev); //elaboro
                        if (toElaborate != null) {
                            if (connectWithNext()) {
                                sendDataToNext(toElaborate); //invio
                            }
                            if(sensorToDelete!=null){
                                DeleteSensor();
                            }
                        }
                    }
                }

            } catch (InterruptedException | IOException e) {
                if(e.toString().contains("java.net.ConnectException")){
                    log.error("Cannot connect with Sensor:"+sensor.getNextSensor(),getClass().getSimpleName());
                    log.error("Removing it from list",getClass().getSimpleName());
                    sensorToDelete = sensor.getNextSensor();
                    DeleteSensor();
                    if(toElaborate !=null)
                        SensorHandle.commonBuffer.push(toElaborate);
                }
            }
        }
        log.info(getClass().getSimpleName() + " Terminated", getClass().getSimpleName());
    }


    private Object elaborateData(Object data) throws IOException {
        Object result = null;
        if (data instanceof Event) {
            //gestisco Event
            if (elaborateEvent((Event) data))
                result = data;
        } else if (data instanceof Token) {
            //gestisco Token
            result = elaborateToken((Token) data);
        }
        try {
            sleep(2000);
        } catch (InterruptedException e) {
        }
        return result;
    }

    private void DeleteSensor(){
        log.info("Sensor removed",getClass().getSimpleName());
        sensor.networkSensors.remove(sensorToDelete);
        sensorToDelete=null;
    }

    private boolean elaborateEvent(Event event) {
        boolean toreplicate=false;
        if (!event.isSentBy(sensor.sensorData)) {//non ho inviato io il messaggio
            log.info("Event Sent by another", getClass().getSimpleName());
            if (event.isInsertEvent()) {//qualcuno è entrato nella rete
                log.info("A sensor Entered", getClass().getSimpleName());
                if (event.isMyPrevious(sensor.sensorData)) {
                    log.info("Is my previous sensor (so he connected to me)", getClass().getSimpleName());
                    int index=sensor.networkSensors.indexOf(sensor.sensorData);
                    //posiziono il sensore prima di me nella lista
                    sensor.networkSensors.add(index,new SensorData(event.target.getId(),event.target.getType(),event.target.getAddress(),event.target.getPort()));

                }else {
                    sensor.networkSensors.add(new SensorData(event.target.getId(),event.target.getType(),event.target.getAddress(),event.target.getPort()));
                }
                toreplicate=true;
            } else if (event.isDeleteEvent()) {
                log.info("a SensorHandle is exiting", getClass().getSimpleName());
                toreplicate=true;
            }
        } else if(event.isSentBy(sensor.sensorData)) {//ho inviato io il messaggio
            log.info("Event message send by me", getClass().getSimpleName());
            if (event.isDeleteEvent()) { //è il messaggio di uscita dalla rete

                log.info("Delete event message returned, can close the server", getClass().getSimpleName());
                sensor.canClose();
            }else if(event.isInsertEvent()){
                if(!waitForInsertReturn) {
                    waitForInsertReturn = true;
                    toreplicate=true;
                }
                else {
                    waitForInsertReturn = false;
                }
            }
        }
        return toreplicate;
    }

    private Token elaborateToken(Token token) {
        try {
            sensor.computeToken(token);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return token;
    }

    void sendDataToNext(Object data) throws IOException {
        boolean connected=true;
        if(nextSocket.isClosed()){
            connected=connectWithNext();
        }
        if(connected) {
            JsonObject dataForNext = new JsonObject();
            if (data instanceof Event) {
                dataForNext.addProperty("MessageType", "Event");
                dataForNext.add("Body", gson.toJsonTree(data, Event.class));
            } else if (data instanceof Token) {
                dataForNext.addProperty("MessageType", "Token");
                dataForNext.add("Body", gson.toJsonTree(data, Token.class));
            }
            log.info("send data to " + nextSocket.getInetAddress().toString() + " port " + nextSocket.getPort(), getClass().getSimpleName());
            try {
                out.writeUTF(dataForNext.toString());
                in.read();//aspetto l'ok
                nextSocket.close(); //chiudo la connessione
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
