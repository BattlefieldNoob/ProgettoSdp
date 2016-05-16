package sensor;

import sensor.simulator.Measurement;
import server.data.SensorData;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by antonio on 11/05/16.
 */
public class ProvaMisurazioni {



    public static void main(String[] args) throws IOException {

        javax.ws.rs.client.Client client = ClientBuilder.newClient();
        WebTarget target = client.target("http://localhost:8080").path("appsdp/measurements");
        Measurement b=new Measurement("ui","uii","bjbjbj",12345L);
        List<Measurement> ms=new LinkedList<>();
        ms.add(b);
        ms.add(b);
        ms.add(b);
        ms.add(b);
        ms.add(b);
        ms.add(b);
        ms.add(b);
        ms.add(b);
        ms.add(b);
        ms.add(b);
        ms.add(b);
        ms.add(b);
        ms.add(b);
        Response res = target.request(MediaType.APPLICATION_JSON).post(Entity.json(ms));

        System.out.println(res);

        System.in.read();
    }
}
