package utils;

import server.client.ClientRequest;
import server.client.MeasurementsByFilter;
import server.client.UserData;
import server.data.ServerData;
import server.sensor.SensorData;
import simulator.Measurement;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by antonio on 18/02/17.
 */
public class GatewayUtils {

    private static GatewayUtils Instance;
    private WebTarget target;
    int clientPort;

    private GatewayUtils(String serverAddress,int clientPort){
        javax.ws.rs.client.Client client = ClientBuilder.newClient();
        target = client.target("http://" + serverAddress + ":8080").path("appsdp");
        this.clientPort = clientPort;
    }


    public static GatewayUtils getInstance(String serverAddress,int port) {
        if(Instance==null)
            Instance=new GatewayUtils(serverAddress,port);
        return Instance;
    }


    int login(String username){
        return target.path("user").request().post(Entity.json(new UserData(username, "localhost", clientPort))).getStatus();
    }


    public int logout(String username){
        return target.path("user/{username}").resolveTemplate("username", username).request().delete().getStatus();
    }


    List<SensorData> getSensors() {
        Future<List<SensorData>> list = target.path("user/sensors").request().buildGet().submit(new GenericType<List<SensorData>>() {
        });

        try {
            return list.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return new LinkedList<>();
        }
    }

    List<ServerData> getAllTypes(){
        Future<List<ServerData>> list = target.path("user/measurements/types").request().buildGet().submit(new GenericType<List<ServerData>>() {
        });

        try {
            return list.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return new LinkedList<>();
        }
    }

    List<ServerData> getAllIDs(){
        Future<List<ServerData>> list = target.path("user/measurements/ids").request().buildGet().submit(new GenericType<List<ServerData>>() {
        });

        try {
            return list.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return new LinkedList<>();
        }
    }


    MeasurementsByFilter getMeasurements(String id, String type,long t1,long t2, boolean requestById){
        Future<MeasurementsByFilter> result = target.path("user/measurements/" + (requestById ? "BySensor" : "ByType")).request().buildPost(Entity.json(new ClientRequest(id, type, t1, t2))).submit(new GenericType<MeasurementsByFilter>() {
        });

        try {
            return result.get();
        } catch (InterruptedException | ExecutionException e) {
            return new MeasurementsByFilter();
        }
    }

    Measurement getLastMeasurement(String id, String type){
        Future<Measurement> listM = target.path("user/measurements/{key}").resolveTemplate("key", id + "-" + type).request().buildGet().submit(new GenericType<Measurement>() {
        });

        try {
            return listM.get();
        } catch (InterruptedException | ExecutionException e) {
            return new Measurement();
        }
    }
}
