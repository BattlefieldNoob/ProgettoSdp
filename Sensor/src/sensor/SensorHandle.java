package sensor;

import sensor.Factories.AbstractSensorFactory;
import sensor.data.CommonBuffer;
import sensor.data.Event;
import sensor.data.ServerResponse;
import sensor.data.Token;
import sensor.simulator.Measurement;
import sensor.simulator.SensorSimulator;
import sensor.utility.Logging;
import server.sensor.SensorData;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by antonio on 14/05/16.
 */
public class SensorHandle {

    final static CommonBuffer commonBuffer = new CommonBuffer();
    public final static Object outputlock = new Object();
    private final Object exitlock = new Object();

    private SensorSimulator sensor;
    static SensorData sensorData;
    private SensorOutputThread outputThread;
    private SensorInputThread inputThread;
    private SensorUdpThread udpThread;
    private static List<SensorData> networkSensors;
    private SensorData nextSensor;
    private Logging log;

    private WebTarget target;
    private boolean exit = false;

    static long lifetime=System.currentTimeMillis();

    private SensorHandle(String type, String gatewayAddress) throws IOException, ExecutionException, InterruptedException {
        sensorData = new SensorData(type, gatewayAddress); //instanza sensore

        javax.ws.rs.client.Client client = ClientBuilder.newClient();
        target = client.target("http://" + gatewayAddress + ":8080");
        WebTarget sensorREST = target.path("appsdp/sensor");
        Future<ServerResponse> res = sensorREST.request(MediaType.APPLICATION_JSON).buildPost(Entity.json(sensorData)).submit(new GenericType<ServerResponse>() {
        });
        if (res!=null && res.get() != null && res.get().getPort()!=-1 && !res.get().getID().equalsIgnoreCase("ERROR")) {

            ServerResponse response=res.get();

            int port=response.getPort();
            String id=response.getID();

            log = Logging.getInstance(id, type);

            sensorData.setId(id);
            sensorData.setPort(port);

            log.info("I am " + id + " " + type + " " + port, getClass().getSimpleName());

            sensor = AbstractSensorFactory.getFactory(type).createSensorSimulator(id);

            networkSensors = new LinkedList<>(response.getSensorList());

            nextSensor = getNextSensor(); //ottengo il sensore successivo

            outputThread = new SensorOutputThread(this, outputlock);//inizializzo il thread per la comunicazione con il successivo
            inputThread = new SensorInputThread(port);//inizializzo il thread per la gestione delle connessioni con il precedente
            udpThread = new SensorUdpThread(this, port);

            if (sensorData.equals(nextSensor)) {//se sono l'unico sensore creo il token
                log.info("Token Generated", getClass().getSimpleName());
                commonBuffer.push(Token.getInstance());//creo il token e lo inserisco nel buffer
            }

            inputThread.start();//avvio il thread
            outputThread.start();//avvio il thread
            announceSensorEnter();//annuncio il nuovo sensore al resto della rete
        }else {
            log.info("Unable to connect to Gateway, exiting", getClass().getSimpleName());
            System.in.read();
            System.exit(1);
        }
    }


    private void mainLoop() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            while (!exit) {
                while (!in.ready() && !exit) {
                    Thread.sleep(200);
                }
                if (!exit) {
                    String data = in.readLine();
                    if (data.equals("exit")) {
                        exit = true;
                    }
                }
            }
            closeSensor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] argv) throws IOException, ExecutionException, InterruptedException {
        if (argv != null && argv.length > 1 && argv.length<3) {
            SensorHandle sensorHandle=new SensorHandle(argv[0],argv[1]);
            sensorHandle.mainLoop();
            System.exit(0);

        } else {
            System.out.println("Wrong parameters, please retry [type(temperature, light), gateway Address]");
            System.exit(1);
        }
    }

    void computeToken(Token token) throws InterruptedException {
        log.info("computing Token", getClass().getSimpleName());
        List<Measurement> measurements = sensor.getMeasurementList();
        if (!measurements.isEmpty()) {
            for (Measurement m : measurements) {
                if (token.isFull()) {
                    log.info("Token Full", getClass().getSimpleName());
                    //invio tutto al server e svuoto il token
                    sendMeasurementsToGateway(token);
                }
                token.addMeasurement(m);
            }
        }
    }

    private void sendMeasurementsToGateway(Token token) {
        WebTarget measurements = target.path("appsdp/sensor/measurements");
        int res = measurements.request(MediaType.APPLICATION_JSON).post(Entity.json(token.readAllAndClean())).getStatus();
        if (res == 200) {
            log.info("Token sent to gateway ", getClass().getSimpleName());
        }
    }

    void canClose(){
        synchronized (exitlock){
            exitlock.notify();
        }
    }


    SensorData getNextSensor(){
        int index = (networkSensors.indexOf(sensorData) + 1) % networkSensors.size();
        return networkSensors.get(index);
    }

    private void announceSensorEnter() {
        log.info("Announce SensorHandle Enter", getClass().getSimpleName());
        commonBuffer.push(new Event("Insert", sensorData, nextSensor));
    }

    private void announceSensorExit() {
        log.info("Announce SensorHandle Exiting", getClass().getSimpleName());
        try {
            outputThread.sendDataToNext(new Event("Delete", sensorData, nextSensor));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    void sensorIsDead(SensorData sensor){
        target.path("appsdp/sensor/{id}").resolveTemplate("id", sensor.getId()).request().delete().getStatus();
    }

    private void closeSensor() throws InterruptedException {
        synchronized (exitlock) {
            sensor.stopMeGently();
            announceSensorExit();

            if (nextSensor != null) {
                exitlock.wait();//aspetto l'ok a morire
            }

            log.info("Closing Sensor...",getClass().getSimpleName());

            outputThread.stopMe();
            outputThread.join();

            inputThread.stopMe();
            inputThread.join();

            udpThread.stopMe();
            udpThread.join();

            target.path("appsdp/sensor/{id}").resolveTemplate("id", sensorData.getId()).request().delete().getStatus();
            exit = true;
        }
    }


    void closeSensorFromUdp(){
        exit=true;
    }


    static int getNetworkSensorsSize(){
        int size=1;
        synchronized (networkSensors){
            size=networkSensors.size();
        }
        return size;
    }


    static void addNetworkSensor(SensorData data){
        synchronized (networkSensors){
            networkSensors.add(data);
        }
    }

    static void removeNetworkSensor(SensorData data){
        synchronized (networkSensors){
            networkSensors.remove(data);
        }
    }
}
