package sensor;

import sensor.Factories.AbstractSensorFactory;
import sensor.data.CommonBuffer;
import sensor.data.Event;
import sensor.data.Token;
import sensor.simulator.Measurement;
import sensor.simulator.SensorSimulator;
import sensor.utility.Logging;
import server.data.SensorData;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by antonio on 14/05/16.
 */
public class SensorHandle {

    //Inizializzazione del buffer comune del sensore
    final static CommonBuffer commonBuffer=new CommonBuffer();
    public final static Object outputlock = new Object();
    private final Object exitlock = new Object();

    SensorSimulator sensor;
    public boolean toEliminate=false;
    SensorData sensorData;
    private SensorOutputThread outputThread;
    private SensorInputThread inputThread;
    public  List<SensorData> networkSensors;
    private SensorData nextSensor;
    private Logging log;

    WebTarget target;



    private SensorHandle(String id, String type, int port, String gatewayAddress) throws IOException, ExecutionException, InterruptedException {
        //prendo l'elemento in posizione
        log = Logging.getInstance(id, type);
        log.info("I am " + id + " " + type + " " + port, getClass().getSimpleName());

        sensor = AbstractSensorFactory.getFactory(type).createSensorSimulator(id);
        sensorData = new SensorData(id, type, "localhost", port); //instanza sensore

        networkSensors = new LinkedList<>(); //sensori in rete

        javax.ws.rs.client.Client client = ClientBuilder.newClient();
        target = client.target("http://" + gatewayAddress + ":8080");
        WebTarget sensorREST = target.path("appsdp/sensor");
        Future<List<SensorData>> res = sensorREST.request(MediaType.APPLICATION_JSON).buildPost(Entity.json(sensorData)).submit(new GenericType<List<SensorData>>() {
        });
        if (res.get() != null) { //registrato sulla rete
            log.info("Server Response:" + res.get(), getClass().getSimpleName());
            networkSensors = res.get();
        }

        nextSensor = getNextSensor(); //ottengo il sensore successivo

        outputThread = new SensorOutputThread(this, outputlock);//inizializzo il thread per la comunicazione con il successivo
        inputThread = new SensorInputThread(port);//inizializzo il thread per la gestione delle connessioni con il precedente
        inputThread.start();//avvio il thread
        if (sensorData.equals(nextSensor)) {//se sono l'unico sensore creo il token
            log.info("Token Generated", getClass().getSimpleName());
            commonBuffer.push(Token.getInstance());//creo il token e lo inserisco nel buffer
        }
        outputThread.start();//avvio il thread
        announceSensorEnter();//annuncio il nuovo sensore al resto della rete

        //Inizializzazione completata
        Scanner scanner = new Scanner(System.in);
        boolean exit = false;
        while (!exit) {
            if (scanner.hasNext()) {
                String data = scanner.next();
                if (data.equals("exit")) {
                    closeSensor();
                    exit = true;
                }
            }
        }
        System.exit(0);
    }

    public static void main(String[] argv) throws IOException, ExecutionException, InterruptedException {
        if (argv != null && argv.length > 0) {
            if (argv.length > 3)
                new SensorHandle(argv[1], argv[0], Integer.parseInt(argv[2]), argv[3]);
            else
                System.out.println("Wrong parameters, please retry [type(temperature, light), ID, port, gateway Address]");
        } else {
            System.out.println("Wrong parameters, please retry [type(temperature, light), ID, port, gateway Address]");
        }
    }

    void computeToken(Token token) throws InterruptedException {
        log.info("computing Token", getClass().getSimpleName());
        synchronized (token) {
            synchronized (sensor) {
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


    public SensorData getNextSensor(){
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

    private void closeSensor() throws InterruptedException {
        synchronized (exitlock) {
            System.out.println("Closing...");
            sensor.stopMeGently();
            announceSensorExit();
            if (nextSensor != null) {
                exitlock.wait();//aspetto l'ok a morire
            }
            String.valueOf(target.path("/{id}").resolveTemplate("id", sensorData.getId()).request().delete().getStatus());
            System.out.println("exitLock passed");
            inputThread.stopMe();
            outputThread.stopMe();
            outputThread.interrupt();
            outputThread.join();
        }
    }
}
