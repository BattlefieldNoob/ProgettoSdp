package server;


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
import java.util.List;

/**
 * Created by antonio on 12/05/16.
 */

@Path("/user")
public class UsersApi {

    static UsersDB usersDB = UsersDB.getInstance();
    static MeasurementsDB measurementsDB = MeasurementsDB.getInstance();
    static SensorsDB sensorsDB = SensorsDB.getInstance();

    public static void sendPush(String message) throws IOException {
        synchronized (usersDB) {
            DatagramSocket ds = new DatagramSocket();
            for (UserData user : usersDB.readAll()) {
                DatagramPacket dp = new DatagramPacket(message.getBytes(), message.getBytes().length, InetAddress.getByName(user.address), user.port);
                System.out.println("Sending push to " + dp.toString());
                ds.send(dp);
            }
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addSensor(UserData user) {
        System.out.println("Un Utente ha effettuato il login");
        if (usersDB.create(user))
            return Response.ok().build();
        else
            return Response.status(401).build();
    }

    @DELETE
    @Path("/{username}")
    public Response deleteSensor(@PathParam("username") String user) {
        System.out.println("Un Utente ha effettuato il logout");
        usersDB.delete(user);
        return Response.ok().build();
    }

    @GET
    @Path("/measurements/{key}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Measurement> getMeasurements(@PathParam("key") String key) {
        System.out.println("Qualcuno ha chiesto le misurazioni con key " + key);
        return measurementsDB.readById(key);
    }

    @GET
    @Path("/sensors")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SensorData> getAllSensor() {
        return sensorsDB.readAll();
    }

}
