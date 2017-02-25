package sensor;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import sensor.data.Event;
import sensor.data.Token;
import sensor.data.TokenLostEvent;
import sensor.utility.Logging;
import server.sensor.SensorData;

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
    private boolean waitForTokenLostReturn=false;
    private Object toElaborate;


    SensorOutputThread(SensorHandle thisSensor, Object lockObject) throws IOException {
        this.lock = lockObject;
        this.sensor = thisSensor;
    }


    private boolean connectWithNext() {
        boolean connected = false;
        int maxTry = 5;
        int currentTry;
        do {
            SensorData next = sensor.getNextSensor();
            currentTry=0;
            do {
                try {
                    nextSocket = new Socket(next.getAddress(), next.getPort());//connetto con il successivo

                    out = new DataOutputStream(nextSocket.getOutputStream());//ottengo gli stream
                    in = new DataInputStream(nextSocket.getInputStream());
                    //attendo che il successivo mi invii il messaggio "OK"
                    String msg = in.readUTF();
                    connected = msg.equals("OK");
                } catch (IOException e) {
                    log.info("Connection attempt " + (currentTry + 1) + " to "+ next.getId()+" failed", getClass().getSimpleName());
                    try {
                        currentTry++;
                        connected = false;
                        Thread.sleep(1500);
                    } catch (InterruptedException ee) {
                        ee.printStackTrace();
                    }
                }

            } while (currentTry < maxTry && !connected);
            if (currentTry >= maxTry) {
                //non riesco a connettermi al successivo, quindi lo tolgo dalla lisa e avviso il gateway
                sensorToDelete = sensor.getNextSensor();
                sensor.sensorIsDead(sensorToDelete);
                DeleteSensor();
            }
        }while(!connected);
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
        log.info("Elaborate data",getClass().getSimpleName());
        try {
            sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(data instanceof TokenLostEvent){
            if(elaborateTokenLost((TokenLostEvent) data))
                result = data;
        } else if (data instanceof Event) {
            if (elaborateEvent((Event) data))
                result = data;
        } else if (data instanceof Token) {
            result = elaborateToken((Token) data);
        }

        return result;
    }

    private void DeleteSensor(){
        log.info("Sensor removed",getClass().getSimpleName());
        SensorHandle.removeNetworkSensor(sensorToDelete);
        sensorToDelete=null;
    }

    private boolean elaborateEvent(Event event) {
        boolean toReplicate=false;
        if (!event.isSentBy(SensorHandle.sensorData)) {//non ho inviato io il messaggio
            if (event.isInsertEvent()) {//qualcuno è entrato nella rete
                log.info("A sensor is entering", getClass().getSimpleName());
                SensorHandle.addNetworkSensor(new SensorData(event.target.getId(),event.target.getType(),event.target.getAddress(),event.target.getPort()));
                toReplicate=true;
            } else if (event.isDeleteEvent()) {
                log.info("A Sensor is exiting", getClass().getSimpleName());
                sensorToDelete=event.target;
                toReplicate=true;
            }
        } else if(event.isSentBy(SensorHandle.sensorData)) {//ho inviato io il messaggio
            if (event.isDeleteEvent()) { //è il messaggio di uscita dalla rete
                sensor.canClose();
            }else if(event.isInsertEvent()){
                if(!waitForInsertReturn) {
                    waitForInsertReturn = true;
                    toReplicate=true;
                }
                else {
                    waitForInsertReturn = false;
                }
            }
        }
        return toReplicate;
    }

    private boolean elaborateTokenLost(TokenLostEvent event){
        //prendo l'event e controllo chi l'ha inviato
        if(event.isSentBy(SensorHandle.sensorData)){  //inviato da me
            if(!waitForTokenLostReturn)
                waitForTokenLostReturn=true;
            else{
                //mi faccio dare l'id del "vincitore"
                if(event.getOlderSensorId().equals(SensorHandle.sensorData.getId())) {
                    log.info("I will create new Token",getClass().getSimpleName());
                    SensorHandle.commonBuffer.push(Token.getInstance());//se il vincitore sono io genero un nuovo token
                }
                waitForTokenLostReturn=false;
                return false;
            }
        }
        event.add(SensorHandle.sensorData.getId(),System.currentTimeMillis()-SensorHandle.lifetime);
        return true;
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
            if(data instanceof  TokenLostEvent){
                dataForNext.addProperty("MessageType", "Event");
                dataForNext.add("Body", gson.toJsonTree(data, TokenLostEvent.class));
            } else if (data instanceof Event) {
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
