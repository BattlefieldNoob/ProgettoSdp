package sensor;

import com.google.gson.Gson;
import sensor.data.Token;
import sensor.data.Ussaro;
import sensor.simulator.Measurement;
import sensor.utility.Logging;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.List;

/**
 * Created by antonio on 13/05/16.
 * é il outputThread che coordina tutto
 */
public class SensorComputeThread extends Thread {

    Sensor sensor;
    Gson gson=new Gson();
    boolean running=true;
    private Logging log=Logging.getInstance();

    public SensorComputeThread(Sensor sensor){
        this.sensor=sensor;
        try {
            nextSensor();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopMe(){
        running=false;
    }


    @Override
    public void run(){
        while (running) {
            synchronized (sensor.message){
                if(!sensor.message.isEmpty()) {
                    log.info("Can compute");
                    handleData(sensor.message);
                    //invio i dati al successivo
                }
            }
            if(sensor.isSendToMyself){
                handleToken(sensor.token);
            }
        }
    }

    public void nextSensor() throws IOException {
        log.info("Calcolo il nuovo successivo");
        //la funzione deve preoccuparsi di "scegliere" il sensore successivo dalla lista
        if(sensor.networkSensors.size()==1){
            //sono il primo sensore della rete
            log.info("Sono il primo sensore, oppure sono rimasto da solo");
            sensor.thread.sendToMyself=true;//invio le cose a me stesso
            sensor.isSendToMyself=true;
            if(sensor.token==null)//genero il token
                sensor.token=Token.getInstance();
        }else{
            //non sono da solo, mi trovo nella lista e prendo il successivo
            log.info("Sensors:"+sensor.networkSensors);
            int index=sensor.networkSensors.indexOf(sensor.thisSensor)+1;
            if(index>=sensor.networkSensors.size()){
                index=0;
            }
            log.info("Nuovo successivo:"+sensor.networkSensors.get(index));
         //   sensor.thread.changeNext(sensor.networkSensors.get(index));
            sensor.thread.sendToMyself=false;//invio le cose a me stesso
            sensor.isSendToMyself=false;
        }
    }

    public void handleData(String message){
        if (message.contains("Token")) {
            log.info("Ricevuto token");
            handleToken(gson.fromJson(message.split("-")[1], Token.class));
        } else if (message.contains("Ussaro")) {
            sensor.ussaro = gson.fromJson(message.split("-")[1], Ussaro.class);//vedere cosa fare
            handleUssaro(sensor.ussaro);
        }
        sensor.message="";
    }

    public void handleToken(Token newToken){
        sensor.token=newToken;
        synchronized (sensor.token) {
            List<Measurement> measurements = sensor.buffer.readAllAndClean();
            if (!measurements.isEmpty()) {
                for (Measurement m : measurements) {
                    if (sensor.token.isFull()) {
                        System.err.println("Token Pieno!!!!!");
                        //invio tutto al server e svuoto il token
                        sendMeasurementsToGateway();
                    }
                    sensor.token.addMeasurement(m);
                }
            }
        }
    }

    public void handleUssaro(Ussaro ussaro) {
        sensor.ussaro=ussaro;
        if(sensor.ussaro!=null) {
            synchronized (sensor.ussaro) {
                log.info("Ricevuto Ussaro");
                if (!ussaro.target.getId().equals(sensor.id)) {//non ho inviato io il messaggio
                    if (ussaro.event.equals("Insert")) {
                        sensor.networkSensors.add(ussaro.target);
                        //io sono target nextSocket?
                        log.info(ussaro.targetNext.getAddress()+" "+sensor.thisSensor.getAddress()+" "+ussaro.targetNext.getPort()+" "+sensor.thisSensor.getPort());
                        //se io sono il nextTarget devo disattivare il server
                        if(sensor.thread.nextdata!=null) {
                            if (sensor.thisSensor.getAddress().equals(ussaro.targetNext.getAddress()) && sensor.thisSensor.getPort()==ussaro.targetNext.getPort()) {
                                log.info("ah, sono io il nextTarget, allora ripristino il server");
                                //si, allora scollego il server e lo riapro
                                try {
                                    sensor.nextThread.in.close();
                                    sensor.nextThread.server.close();
                                    sensor.nextThread.connected = false;
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        log.info("new Sensor");

                    } else if (ussaro.event.equals("Delete")){
                        sensor.networkSensors.remove(ussaro.target);
                        log.info("a Sensor is exiting");
                        //ero connesso a lui come server?
                        if(ussaro.targetNext.getAddress().equals(sensor.thisSensor.getAddress()) && ussaro.targetNext.getPort()==sensor.thisSensor.getPort())
                        {
                            try {
                                sensor.nextThread.in.close();
                                sensor.nextThread.server.close();
                                sensor.nextThread.connected = false;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                    }
                    try {
                        nextSensor();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {//ho inviato io il messaggio
                    if (sensor.imDying && ussaro.event.equals("Delete")) {
                        //posso morire
                        try {
                            sensor.shutdownAll();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        sensor.ussaro = null;
                    } else {
                        log.info("cose");
                        sensor.ussaro = null;
                    }
                }
            }
        }
    }

    public void sendMeasurementsToGateway(){
        javax.ws.rs.client.Client client = ClientBuilder.newClient();
        WebTarget target = client.target("http://localhost:8080").path("appsdp/sensor/measurements");
        Gson prova=new Gson();
        log.info("il token è grande:"+prova.toJson(sensor.token).length());
        int res = target.request(MediaType.APPLICATION_JSON).post(Entity.json(sensor.token.readAllAndClean())).getStatus();
        if(res==200){
            log.info("Invio Token Effettuato");
        }
    }
}
