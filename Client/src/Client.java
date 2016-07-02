import server.data.SensorData;
import server.simulator.Measurement;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by antonio on 12/05/16.
 */
public class Client {

    public static void main(String[] argv) throws ExecutionException, InterruptedException, IOException {
        String serverAddress = argv[0];
        System.out.println("Hello!!");
        System.out.println("It's me, Mario!");
        javax.ws.rs.client.Client client = ClientBuilder.newClient();
        WebTarget target = client.target("http://" + serverAddress + ":8080").path("appsdp");
        boolean login = false;
        Scanner scanner = new Scanner(System.in);
        int port = 9100;
        String username;
        do {
            username = scanner.next();
            int res = target.path("user").request().post(Entity.json(new UserData(username, "localhost", port))).getStatus();
            if (res == 401) {
                System.out.println("Username già utilizzato, inserirne un altro");
            } else if (res == 200) {
                System.out.println("Accesso Eseguito");
                login = true;
            }
        } while (!login);

        PushThread thread = new PushThread(port);
        thread.start();
        while (true) {
            System.out.println("I sensori sono:");
            Future<List<SensorData>> list = target.path("user/sensors").request().buildGet().submit(new GenericType<List<SensorData>>() {
            });
            for (SensorData data : list.get()) {
                System.out.println(data);
            }
            System.out.println("Per quale sensore chiedi le rilevazioni?");
            int index = scanner.nextInt();
            if (index > 0 && index <= list.get().size()) {
                Future<List<Measurement>> listM = target.path("user/measurements/{key}").resolveTemplate("key", list.get().get(index - 1).getId() + "-" + list.get().get(index - 1).getType()).request().buildGet().submit(new GenericType<List<Measurement>>() {
                });
                List<Measurement> measurements = listM.get();
                if (measurements == null) {
                    System.out.println("Nessuna Misurazione per il sensore ");
                } else {
                    for (Measurement data : measurements) {
                        System.out.println(data);
                    }
                }
            }
            if (index == -1) {
                break;
            }
        }
        System.out.println("Stopping");
        thread.stopMe();
        int res = target.path("user/{username}").resolveTemplate("username", username).request().delete().getStatus();
        if (res == 200) {
            System.out.println("Done");
        }
        System.exit(0);
    }
}
