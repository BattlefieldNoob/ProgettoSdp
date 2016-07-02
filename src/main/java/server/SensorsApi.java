package server;


import server.data.MeasurementsDB;
import server.data.SensorData;
import server.data.SensorsDB;
import server.simulator.Measurement;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;

/**
 * Created by antonio on 06/05/16.
 */

@Path("/sensor")
public class SensorsApi {

    static SensorsDB sensorsDB = SensorsDB.getInstance();
    static MeasurementsDB db = MeasurementsDB.getInstance();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public SensorData getSensorData(String id) {
        return sensorsDB.read(id);
    }

    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SensorData> getAllSensor() {
        return sensorsDB.readAll();
    }

    @GET
    @Path("/next/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public SensorData getNextSensor(@PathParam("id") String id) {
        SensorData ret = sensorsDB.readNext(id);
        //   System.out.println("Next of "+id+":"+ret);
        return ret;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<SensorData> addSensor(SensorData sensor) {
        System.out.println("Un sensore vuole inserirsi nella rete");
        System.out.println(sensor);
        sensorsDB.create(sensor);
        try {
            UsersApi.sendPush(sensor + " Entered");
        } catch (IOException e) {
            e.printStackTrace();
        }
        //TODO: controllare che non ci sia un altro sensore con lo stesso ID
        return sensorsDB.readAll();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteSensor(@PathParam("id") String id) {
        System.out.println("Un sensore sta uscendo dalla rete");
        sensorsDB.delete(id);
        try {
            UsersApi.sendPush(id + " Exited");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Response.ok().build();
    }

    @POST
    @Path("measurements")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addMeasurements(List<Measurement> measurements) {
        System.out.println("Un sensore mi ha inviato le misurazioni");
        db.addMeasurements(measurements);
        return Response.ok().build();
    }

}
