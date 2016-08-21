package sensor;

import sensor.Factories.AbstractSensorFactory;
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
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by antonio on 14/05/16.
 */
public class Sensor {

    private final Object outputlock = new Object();
    private final Object exitlock = new Object();
    Token token;
    Queue<Event> eventBuffer = new PriorityQueue<>();
    SensorSimulator sensor;
    WebTarget target;
    public SensorData thisSensor;
    private SensorOutputThread outputThread;
    private SensorServerThread inputThread;
    private List<SensorData> networkSensors;
    private SensorData nextSensor;
    private Logging log;
    private boolean sendToMyself = false;
    private boolean waitingForEventReturn = false, waitingForEventDeleteReturn = false;

    private Sensor(String id, String type, int port, String gatewayAddress) throws IOException, ExecutionException, InterruptedException {
        //prendo l'elemento in posizione
        log = Logging.getInstance(id, type);
        log.info("I am" + id + " " + type + " " + port, getClass().getSimpleName());

        AbstractSensorFactory factory=AbstractSensorFactory.getFactory(type);
        sensor = factory.createSensorSimulator(id);

        thisSensor = new SensorData(id, type, "localhost", port);
        networkSensors = new LinkedList<>();
        javax.ws.rs.client.Client client = ClientBuilder.newClient();
        target = client.target("http://" + gatewayAddress + ":8080");
        WebTarget sensorREST = target.path("appsdp/sensor");
        Future<List<SensorData>> res = sensorREST.request(MediaType.APPLICATION_JSON).buildPost(Entity.json(thisSensor)).submit(new GenericType<List<SensorData>>() {
        });
        if (res.get() != null) { //registrato sulla rete
            log.info("Server Response:" + res.get(), getClass().getSimpleName());
            networkSensors = res.get();
        }
        outputThread = new SensorOutputThread(this, outputlock);//mi connetto con il successivo
      //  nextSensor = findNextSensor();
        nextSensor = getNextSensor();
        if (nextSensor != null)
            outputThread.configureConnectionWithNext(nextSensor);
        inputThread = new SensorServerThread(this, port);
        if (networkSensors.size() <= 1) {
            //sono il primo, genero il token
            log.info("Token Generated", getClass().getSimpleName());
            setToken(Token.getInstance());
        }
        inputThread.start();
        outputThread.start();
        announceSensorEnter();
        Scanner scanner = new Scanner(System.in);
        boolean exit = false;
        while (!exit) {
            if (scanner.hasNext()) {
                String data = scanner.next();
                if (data.equals("exit")) {
                    synchronized (exitlock) {
                        System.out.println("Closing...");
                        outputThread.expectedException = true;
                        if (!inputThread.inputThreads.isEmpty())
                            inputThread.inputThreads.getFirst().expectedException = true;
                        sensor.stopMeGently();
                        announceSensorExit();
                        String.valueOf(target.path("/{id}").resolveTemplate("id", id).request().delete().getStatus());
                        if (nextSensor != null)
                            exitlock.wait();//aspetto l'ok a morire
                        System.out.println("exitLock passed");
                        outputThread.stopMe();
                        outputThread.interrupt();
                        outputThread.join();
                        exit = true;
                    }
                }
            }
        }
        System.exit(0);
    }

    public static void main(String[] argv) throws IOException, ExecutionException, InterruptedException {
        if (argv != null && argv.length > 0) {
            if (argv.length > 3)
                new Sensor(argv[1], argv[0], Integer.parseInt(argv[2]), argv[3]);
            else
                System.out.println("Wrong parameters, please retry [type(temperature, light), ID, port, gateway Address]");
        } else {
            System.out.println("Wrong parameters, please retry [type(temperature, light), ID, port, gateway Address]");
        }
    }

    synchronized boolean isDataAvailable() {
        return token != null;
    }

    private void sendMeasurementsToGateway() {
        WebTarget measurements = target.path("appsdp/sensor/measurements");
        int res = measurements.request(MediaType.APPLICATION_JSON).post(Entity.json(token.readAllAndClean())).getStatus();
        if (res == 200) {
            log.info("Token sent to gateway ", getClass().getSimpleName());
        }
    }

    synchronized boolean isSendToMyself() {
        return sendToMyself;
    }

    Token computeToken() throws InterruptedException {
        log.info("computing Token", getClass().getSimpleName());
        Thread.sleep(2000);
        if (token != null) {
            synchronized (token) {
                synchronized (sensor) {
                    List<Measurement> measurements = sensor.getMeasurementList();
                if (!measurements.isEmpty()) {
                    for (Measurement m : measurements) {
                        if (token.isFull()) {
                            log.info("Token Full", getClass().getSimpleName());
                            //invio tutto al server e svuoto il token
                            sendMeasurementsToGateway();
                        }
                        token.addMeasurement(m);
                        }
                    }
                }
            }
        }
        return token;
    }

    synchronized void setToken(Token newToken) {
        if (newToken == null) {
            log.error("Something WRONG!!!!!!!!", getClass().getSimpleName());
        } else {
            synchronized (outputlock) {
                token = newToken;
                outputlock.notify();
            }
        }
    }

    synchronized void setEvent(Event event) {
        if (event == null) {
            log.error("Something WRONG!!!!!!!!", getClass().getSimpleName());
        } else {
            synchronized (outputlock) {
                eventBuffer.add(event);
                elaborateEvent(event);
                outputlock.notify();
            }
        }
    }

    private synchronized void elaborateEvent(Event event) {
        if (!event.target.getId().equals(thisSensor.getId())) {//non ho inviato io il messaggio
            if (event.event.equals("Insert")) {
                if (!networkSensors.contains(event.target)) {
                    networkSensors.add(event.target);
                    log.info("new Sensor", getClass().getSimpleName());
                }
            } else if (event.event.equals("Delete")) {
                networkSensors.remove(event.target);
                log.info("a Sensor is exiting", getClass().getSimpleName());
            }
            try {
               // nextSensor = findNextSensor();
                nextSensor = getNextSensor();
                outputThread.configureConnectionWithNext(nextSensor);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }

        } else {//ho inviato io il messaggio
            log.info("Event message send by me", getClass().getSimpleName());
            if (event.event.equals("Insert")) {
                if (!waitingForEventReturn) {
                    try {
                        nextSensor = findNextSensor();
                        outputThread.configureConnectionWithNext(nextSensor);
                        waitingForEventReturn = true;
                    } catch (InterruptedException | IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    log.info("Insert event message returned, Removing it", getClass().getSimpleName());
                    eventBuffer.remove();
                    waitingForEventReturn = false;
                }
                //ora posso tentare la connessione
            } else if (event.event.equals("Delete")) {
                if (waitingForEventDeleteReturn) {
                    log.info("Delete event message returned, can close the server", getClass().getSimpleName());
                    eventBuffer.remove();
                    inputThread.stopMe();
                    try {
                        inputThread.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    waitingForEventDeleteReturn = false;
                    synchronized (exitlock) {
                        exitlock.notify();//permetto la fine dei processi
                        log.info("Sensor can terminate", getClass().getSimpleName());
                    }
                }
            }
        }

    }

    private SensorData findNextSensor() throws IOException {
        //la funzione deve preoccuparsi di "scegliere" il sensore successivo dalla lista
        if (networkSensors.size() == 1) {
            //sono il primo sensore della rete
            log.info("I'm the only sensor in network", getClass().getSimpleName());
            sendToMyself = true;
            return null;
        } else {
            //non sono da solo, mi trovo nella lista e prendo il successivo
            log.info("Sensors:" + networkSensors, getClass().getSimpleName());
            int index = networkSensors.indexOf(thisSensor) + 1;
            if (index >= networkSensors.size()) {
                index = 0;
            }
            log.info("New Next:" + networkSensors.get(index), getClass().getSimpleName());
            sendToMyself = false;
            return networkSensors.get(index);
        }
    }

    private SensorData getNextSensor(){
        int index = networkSensors.indexOf(thisSensor) + 1;
        if (index >= networkSensors.size()) {
            index = 0;
        }
        sendToMyself = false;
        return networkSensors.get(index);
    }

    private void announceSensorEnter() {
        if (networkSensors.size() > 1) {//invio il messaggio solo se ho un successivo
            log.info("Announce Sensor Enter", getClass().getSimpleName());
            eventBuffer.add(new Event("Insert", thisSensor, nextSensor)); //invio un messaggio Udp al successivo
        }
    }

    private void announceSensorExit() {
        if (networkSensors.size() > 1) {//invio il messaggio solo se ho un successivo
            log.info("Announce Sensor Exiting", getClass().getSimpleName());
            eventBuffer.add(new Event("Delete", thisSensor, nextSensor)); //invio un messaggio Udp al successivo
            waitingForEventDeleteReturn = true;
        }
    }

}
