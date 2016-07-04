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
        return ret;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<SensorData> addSensor(SensorData sensor) {
        System.out.println("A Sensor want to enter into the network");
        System.out.println(sensor);
        sensorsDB.create(sensor);
        try {
            UsersApi.sendPush(sensor.getId(),sensor.getType(),sensor.getAddress(),String.valueOf(sensor.getPort()),"Enter");
        } catch (IOException e) {
            e.printStackTrace();
        }
        //TODO: controllare che non ci sia un altro sensore con lo stesso ID
        return sensorsDB.readAll();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteSensor(@PathParam("id") String id) {
        System.out.println("A Sensor is exiting from the network");
        SensorData sensor=sensorsDB.read(id);
        sensorsDB.delete(id);
        try {
            UsersApi.sendPush(sensor.getId(),sensor.getType(),sensor.getAddress(),String.valueOf(sensor.getPort()),"Exit");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Response.ok().build();
    }

    @POST
    @Path("measurements")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addMeasurements(List<Measurement> measurements) {
        System.out.println("Measurements from Sensor");
        db.addMeasurements(measurements);
        return Response.ok().build();
    }

}
