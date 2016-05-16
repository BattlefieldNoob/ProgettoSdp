package sensor;

import com.google.gson.Gson;
import sensor.data.Ussaro;
import sensor.simulator.*;
import server.data.SensorBuffer;
import server.data.SensorData;
import sensor.data.Token;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

import java.lang.annotation.Target;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


/**
 * Created by antonio on 06/05/16.
 */
public class Sensor {
    Simulator sensor;
    Thread sensorThread;
    int port;
    public Token token;
    public Ussaro ussaro;
    SensorData thisSensor;
    SensorOutputThread thread;
    SensorComputeThread computeThread;
    SensorUssaroThread ussaroThread;
    SensorBuffer buffer;
    SensorInputThread nextThread;
    String id;
    List<SensorData> networkSensors;
    public boolean imDying=false;
    boolean isSendToMyself=false;
    String message="";

    WebTarget target;

    public static void main(String[] argv) throws IOException, ExecutionException, InterruptedException {
        if(argv!=null && argv.length>0) {
            new Sensor(argv[1],argv[0],Integer.parseInt(argv[2]));
        }else{
            System.out.println("Paramentri non corretti");
            System.in.read();
        }
    }

    Sensor(String id,String type,int port) throws IOException, ExecutionException, InterruptedException {
        //prendo l'elemento in posizione 0
        this.port=port;
        this.id=id;
        System.out.println("Io sono:"+id+" "+type+" "+port);
        buffer=new SensorBuffer();
        switch (type) {
            case "light":
                sensor = new LightSimulator(id, buffer);
                break;
            case "temperature":
                sensor = new TemperatureSimulator(id, buffer);
                break;
            default:
                System.out.println("Errore!!!!!");
                return;
        }
        thisSensor=new SensorData(id, type, "localhost", port);
        sensorThread = new Thread(sensor);
        sensorThread.start();
        //tento la connessione con il gateway
        javax.ws.rs.client.Client client = ClientBuilder.newClient();
        target = client.target("http://localhost:8080").path("appsdp/sensor");
/*
        Future<List<SensorData>> res = target.request(MediaType.APPLICATION_JSON).buildPost(Entity.json(thisSensor)).submit(new GenericType<List<SensorData>>(){});
        //System.out.println("First:"+res.getStatusInfo());
        if (res.get()!=null) { //registrato sulla rete
            System.out.println("Server Response:"+res.get());
            networkSensors=res.get();
            outputThread=new SensorOutputThread(this,buffer);//mi connetto con il successivo
            computeThread=new SensorComputeThread(this);
            inputThread=new SensorInputThread(this,computeThread);//mi rendo disponibile a connessioni
            ussaroThread=new SensorUssaroThread(port,this);
            ussaroThread.start();
            inputThread.start();
            outputThread.start();
            computeThread.start();
        }
        Scanner scanner = new Scanner(System.in);
        while(true) {
            if (scanner.hasNext() && scanner.nextSocket().equals("exit")){
                System.out.println("Closing...");
                sensor.stopMeGently();
                imDying=true;

            }
        }*/
    }

    public void shutdownAll() throws InterruptedException {
        nextThread.stopMe();
        thread.stopMe();
        computeThread.stopMe();
        ussaroThread.stopMe();
        thread.join();
        System.out.println(target.path("/{id}").resolveTemplate("id", id).request().delete().getStatus());
        System.out.println("Done");
        System.exit(0);
    }


}
