package server;


import server.data.MeasurementsDB;
import server.sensor.SensorData;
import server.data.SensorsDB;
import server.sensor.ServerResponse;
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
    public ServerResponse addSensor(SensorData sensor) {
        System.out.println("A Sensor want to enter into the network");
        ServerResponse response=new ServerResponse();
        int result=sensorsDB.create(sensor);
        if(result!=-1) {
            response=new ServerResponse(sensor.getPort(),sensor.getId(),sensorsDB.readAll());
            try {
                UsersApi.sendPush(sensor.getId(), sensor.getType(), sensor.getAddress(), String.valueOf(sensor.getPort()), "Enter");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return response;
    }

    @DELETE
    @Path("/{id}")
    public Response deleteSensor(@PathParam("id") String id) {
        System.out.println("A Sensor is exiting from the network");
        SensorData sensor = sensorsDB.read(id);
        Response response=Response.serverError().build();
        if (sensorsDB.delete(id)){
            try {
                UsersApi.sendPush(sensor.getId(), sensor.getType(), sensor.getAddress(), String.valueOf(sensor.getPort()), "Exit");
            } catch (IOException e) {
                e.printStackTrace();
            }
            response=Response.ok().build();
        }
        return response;
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
