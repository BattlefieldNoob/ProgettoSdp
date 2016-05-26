package sensor;

import com.google.gson.Gson;
import sensor.data.Token;
import sensor.data.Ussaro;
import sensor.simulator.LightSimulator;
import sensor.simulator.Measurement;
import sensor.simulator.Simulator;
import sensor.simulator.TemperatureSimulator;
import sensor.utility.Logging;
import server.data.SensorBuffer;
import server.data.SensorData;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by antonio on 14/05/16.
 */
public class TestSensor {

    Simulator sensor;
    Thread sensorThread;
    int port;

    Token token;
    SensorData thisSensor;
    SensorOutputThread outputThread;
    SensorComputeThread computeThread;
    SensorUssaroThread ussaroThread;
    SensorBuffer buffer;
    SensorInputThread inputThread;
    String id;
    List<SensorData> networkSensors;
    int nextPort;
    SensorData nextSensor;

    WebTarget target;

    Logging log;

    boolean sendToMyself=false;

    final Object outputlock = new Object();
    final Object inputlock = new Object();
    final Object exitlock = new Object();

    public static void main(String[] argv) throws IOException, ExecutionException, InterruptedException {
        System.out.println("[DEBUG] prova grep console");
        if(argv!=null && argv.length>0) {
            System.out.println(argv.length);
            if(argv.length>3)
                new TestSensor(argv[1],argv[0],Integer.parseInt(argv[2]),Integer.parseInt(argv[3]));
            else
                new TestSensor(argv[1],argv[0],Integer.parseInt(argv[2]),-1);
        }else{
            System.out.println("Paramentri non corretti");
            System.in.read();
        }
    }

    TestSensor(String id, String type, int port,int nextPort) throws IOException, ExecutionException, InterruptedException {
        //prendo l'elemento in posizione 0
        this.port=port;
        this.id=id;
        this.nextPort=nextPort;
        log=Logging.getInstance(id,type);
        log.info("Io sono:"+id+" "+type+" "+port);
        buffer=new SensorBuffer();
        switch (type) {
            case "light":
                sensor = new LightSimulator(id, buffer);
                break;
            case "temperature":
                sensor = new TemperatureSimulator(id, buffer);
                break;
            default:
                log.info("Errore!!!!!");
                return;
        }
        thisSensor=new SensorData(id, type, "localhost", port);
        sensorThread = new Thread(sensor);
        sensorThread.start();
        networkSensors=new LinkedList<>();
        javax.ws.rs.client.Client client = ClientBuilder.newClient();
        target = client.target("http://localhost:8080").path("appsdp/sensor");

        Future<List<SensorData>> res = target.request(MediaType.APPLICATION_JSON).buildPost(Entity.json(thisSensor)).submit(new GenericType<List<SensorData>>(){});
        if (res.get()!=null) { //registrato sulla rete
            log.info("Server Response:"+res.get());
            networkSensors=res.get();
        }
        outputThread = new SensorOutputThread(this,outputlock);//mi connetto con il successivo
        nextSensor=findNextSensor();
        // computeThread=new SensorComputeThread(this);
        inputThread =new SensorInputThread(this,inputlock,port);//mi rendo disponibile a connessioni
        ussaroThread=new SensorUssaroThread(this,port);
        if(networkSensors.size()<=1){
            //sono il primo, genero il token
            log.info("Generato token");
            setToken(Token.getInstance());
        }
        ussaroThread.start();
        inputThread.start();
        outputThread.start();
        announceSensorEnter();

        // computeThread.start();
        Scanner scanner = new Scanner(System.in);
        boolean exit=false;
        while(!exit) {
            if (scanner.hasNext()){
                String data=scanner.next();
                log.info("hai scritto "+data);
                if(data.equals("exit")){
                    synchronized (exitlock) {
                        log.info("Closing...");
                        sensor.stopMeGently();
                        outputThread.stopMe();
                        outputThread.join();
                        announceSensorExit();
                        shutdownAll();
                        if(nextSensor!=null)
                            exitlock.wait();//aspetto l'ok a morire
                        ussaroThread.stopMe();
                        ussaroThread.join();
                        exit=true;
                    }
                }
            }
        }
        System.exit(0);
    }

    public synchronized boolean isDataAvaiable(){
        return token!=null;
    }

    public Token handleToken(Token token){
        synchronized (token) {
            List<Measurement> measurements = buffer.readAllAndClean();
            if (!measurements.isEmpty()) {
                for (Measurement m : measurements) {
                    if (token.isFull()) {
                        System.err.println("Token Pieno!!!!!");
                        //invio tutto al server e svuoto il token
                        sendMeasurementsToGateway();
                    }
                    token.addMeasurement(m);
                }
            }
        }
        return token;
    }

    public void sendMeasurementsToGateway(){
        javax.ws.rs.client.Client client = ClientBuilder.newClient();
        WebTarget target = client.target("http://localhost:8080").path("appsdp/sensor/measurements");
        Gson prova=new Gson();
        log.info("il token è grande:"+prova.toJson(token).length());
        int res = target.request(MediaType.APPLICATION_JSON).post(Entity.json(token.readAllAndClean())).getStatus();
        if(res==200){
            log.info("Invio Token Effettuato");
        }
    }
    public void shutdownAll() throws InterruptedException {
        inputThread.stopMe();
        inputThread.join();
        log.info(String.valueOf(target.path("/{id}").resolveTemplate("id", id).request().delete().getStatus()));
        log.info("Done");
    }

    public synchronized boolean isSendToMyself(){
        return sendToMyself;
    }

    public synchronized Token getComputedToken() throws InterruptedException {
        log.info("GetComputedToken");
        Token token1;
        synchronized (token) {
            Thread.sleep(2000);
            token1 = handleToken(token);
        }
        return token1;
    }

    public synchronized void setToken(Token newToken){
        synchronized (outputlock) {
            token=newToken;
            log.info("data computata");
            outputlock.notify();
        }
    }

    public synchronized void elaborateUssaro(Ussaro ussaro){
        log.info(token!=null?"Ho il token":"Non ho il token");
        if (!ussaro.target.getId().equals(thisSensor.getId())) {//non ho inviato io il messaggio
            if (ussaro.event.equals("Insert")) {
                networkSensors.add(ussaro.target);
                if(ussaro.targetNext.equals(thisSensor)){//io sono il suo next, quindi riapro il server
                    log.info("Riapro il server");
                    inputThread.resetServer();
                }
                log.info("new Sensor");
                try {
                    nextSensor=findNextSensor();
                    outputThread.configureConnectionWithNext(nextSensor);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                outputThread.sendUdpMessage(ussaro, nextSensor!=null?nextSensor:ussaro.target);

            } else if (ussaro.event.equals("Delete")) {
                networkSensors.remove(ussaro.target);
                log.info("a Sensor is exiting");
                if(ussaro.targetNext.equals(thisSensor)){//io sono il suo next, quindi riapro il server
                    log.info("Riapro il server");
                    inputThread.resetServer();
                }
                outputThread.sendUdpMessage(ussaro, nextSensor!=null?nextSensor:ussaro.target);
                try {
                    nextSensor=findNextSensor();
                    outputThread.configureConnectionWithNext(nextSensor);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        } else {//ho inviato io il messaggio
            log.info("Messaggio inviato da me");
            if (ussaro.event.equals("Insert")) {
                try {
                    log.info("mi connetto al successivo");
                    nextSensor=findNextSensor();
                    outputThread.configureConnectionWithNext(nextSensor);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //ora posso tentare la connessione
            }else if (ussaro.event.equals("Delete")) {
                synchronized (exitlock){
                    exitlock.notify();//permetto la morte
                }
                log.info("posso morire");
            }
        }
    }

    public SensorData findNextSensor() throws IOException {
        log.info("Calcolo il nuovo successivo");
        //la funzione deve preoccuparsi di "scegliere" il sensore successivo dalla lista
        if(networkSensors.size()==1){
            //sono il primo sensore della rete
            log.info("Sono il primo sensore, oppure sono rimasto da solo");
            sendToMyself=true;
            return null;
        }else{
            //non sono da solo, mi trovo nella lista e prendo il successivo
            log.info("Sensors:"+networkSensors);
            int index=networkSensors.indexOf(thisSensor)+1;
            if(index>=networkSensors.size()){
                index=0;
            }
            log.info("Nuovo successivo:"+networkSensors.get(index));
            sendToMyself=false;
            return networkSensors.get(index);
        }
    }

    public void announceSensorEnter(){
        if(networkSensors.size()>1) {//invio il messaggio solo se ho un successivo
            log.info("Send Udp Message");
            outputThread.sendUdpMessage(new Ussaro("Insert", thisSensor, nextSensor),nextSensor); //invio un messaggio Udp al successivo
        }
    }

    public void announceSensorExit(){
        if(networkSensors.size()>1) {//invio il messaggio solo se ho un successivo
            log.info("Send Udp Message");
            outputThread.sendUdpMessage(new Ussaro("Delete", thisSensor, nextSensor),nextSensor); //invio un messaggio Udp al successivo
        }
    }
}
