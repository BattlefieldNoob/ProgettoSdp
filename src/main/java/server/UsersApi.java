package server;


import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import server.client.ClientRequest;
import server.client.MeasurementsByFilter;
import server.client.UserData;
import server.data.MeasurementsDB;
import server.data.SensorData;
import server.data.SensorsDB;
import server.data.UsersDB;
import server.simulator.Measurement;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by antonio on 12/05/16.
 */

@Path("/user")
public class UsersApi {

    private final static UsersDB usersDB = UsersDB.getInstance();
    private final static MeasurementsDB measurementsDB = MeasurementsDB.getInstance();
    private final static SensorsDB sensorsDB = SensorsDB.getInstance();

    static void sendPush(String id, String type, String address,String port,String event) throws IOException {
        synchronized (usersDB) {
            JsonObject obj=new JsonObject();
            obj.add("id", new JsonPrimitive(id));
            obj.add("type", new JsonPrimitive(type));
            obj.add("address", new JsonPrimitive(address));
            obj.add("port", new JsonPrimitive(port));
            obj.add("event",new JsonPrimitive(event));
            DatagramSocket ds = new DatagramSocket();
            String message=obj.toString();
            for (UserData user : usersDB.readAll()) {
                DatagramPacket dp = new DatagramPacket(message.getBytes(), message.getBytes().length, InetAddress.getByName(user.address), user.port);
                System.out.println("Sending push to " + dp.toString());
                ds.send(dp);
            }
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response userLogin(UserData user) {
        System.out.println("User Login");
        if (usersDB.create(user))
            return Response.ok().build();
        else
            return Response.status(401).build();
    }

    @DELETE
    @Path("/{username}")
    public Response userLogout(@PathParam("username") String user) {
        System.out.println("User Logout");
        usersDB.delete(user);
        return Response.ok().build();
    }

    @GET
    @Path("/measurements/{key}")
    @Produces(MediaType.APPLICATION_JSON)
    public Measurement getLastMeasurement(@PathParam("key") String key) {
        System.out.println("A user request measurements with this key " + key);
        return measurementsDB.readLastById(key);
    }

    @POST
    @Path("/measurements/BySensor")
    public MeasurementsByFilter getMeasurementBySensor(final ClientRequest request) {
        //ottenere il min, mid, max da un sensore secondo l'id
        List<Measurement> t1t2Filter = measurementsDB.readById(request.id).stream().filter(m -> m.getTimestamp() > request.t1 && m.getTimestamp() < request.t2).collect(Collectors.toList());
        t1t2Filter.sort(new Comparator<Measurement>() {
            @Override
            public int compare(Measurement measurement, Measurement t1) {
                if (Double.valueOf(measurement.getValue()) > Double.valueOf(t1.getValue())) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });
        String min = t1t2Filter.get(0).getValue();
        String max = t1t2Filter.get(t1t2Filter.size() - 1).getValue();
        double acc = 0;
        for (Measurement m : t1t2Filter) {
            acc += Double.valueOf(m.getValue());
        }
        acc = acc / t1t2Filter.size();
        return new MeasurementsByFilter(request.id, request.t1, request.t2, Double.valueOf(min), acc, Double.valueOf(max));
    }

    @POST
    @Path("/measurements/ByType")
    public MeasurementsByFilter getMeasurementByType(final ClientRequest request) {
        //ottenere il min, mid, max da un sensore secondo l'id
        List<Measurement> t1t2Filter = filterInT1T2(measurementsDB.readById(request.id), request.t1, request.t2);
        t1t2Filter.sort(new Comparator<Measurement>() {
            @Override
            public int compare(Measurement measurement, Measurement t1) {
                if (Double.valueOf(measurement.getValue()) > Double.valueOf(t1.getValue())) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });
        String min = t1t2Filter.get(0).getValue();
        String max = t1t2Filter.get(t1t2Filter.size() - 1).getValue();
        double acc = 0;
        for (Measurement m : t1t2Filter) {
            acc += Double.valueOf(m.getValue());
        }
        acc = acc / t1t2Filter.size();
        return new MeasurementsByFilter(request.id, request.t1, request.t2, Double.valueOf(min), acc, Double.valueOf(max));
    }


    private List<Measurement> filterInT1T2(List<Measurement> list, double t1, double t2) {
        return list.stream().filter(m -> m.getTimestamp() > t1 && m.getTimestamp() < t2).collect(Collectors.toList());
    }

    @GET()
    @Path("/sensors")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SensorData> getAllSensor() {
        return sensorsDB.readAll();
    }


}
