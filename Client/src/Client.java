import server.client.ClientRequest;
import server.client.MeasurementsByFilter;
import server.data.SensorData;
import server.simulator.Measurement;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by antonio on 12/05/16.
 */
public class Client {

    private static Scanner scanner;
    private static WebTarget target;

    public static void main(String[] argv) throws ExecutionException, InterruptedException, IOException {
        if (argv.length <= 0) {
            System.out.println("Please specify gateway address");
            System.exit(1);
        } else {
            String serverAddress = argv[0];
            javax.ws.rs.client.Client client = ClientBuilder.newClient();
            target = client.target("http://" + serverAddress + ":8080").path("appsdp");
            boolean login = false;
            scanner = new Scanner(System.in);
            int port = 9100;
            String username;
            do {
                System.out.println("Please write your username:");
                username = scanner.next();
                int res = target.path("user").request().post(Entity.json(new UserData(username, "localhost", port))).getStatus();
                if (res == 401) {
                    System.out.println("Username Already used, please retry");
                } else if (res == 200) {
                    System.out.println("Login successfully");
                    login = true;
                }
            } while (!login);

            PushThread thread = new PushThread(port);
            thread.start();
            while (true) {
                Future<List<SensorData>> list = target.path("user/sensors").request().buildGet().submit(new GenericType<List<SensorData>>() {
                });
                int choose = 0;
                if (list.get().isEmpty()) {
                    System.out.println("No Sensors Available, please wait [-1 for exiting, enter for refresh]");
                    choose = scanner.nextInt();
                } else {
                    System.out.println("[1] Ask Filtered Measurements by ID");
                    System.out.println("[2] Ask Filtered Measurements by Type");
                    System.out.println("[3] Ask Last Measurement by Sensor");
                    System.out.println("[-1] Log out and Exit");
                    choose = scanner.nextInt();
                    int chooseSensor = 0;
                    switch (choose) {
                        case 1:
                            System.out.println("Available Sensors:");
                            for (int i = 0; i < list.get().size(); i++) {
                                System.out.println("[" + (i + 1) + "] " + list.get().get(i).toClientInterface());
                            }
                            chooseSensor = scanner.nextInt();
                            if (chooseSensor > 0 && chooseSensor <= list.get().size()) {
                                getInfoFromMeasurements(list.get().get(chooseSensor - 1).getId(), list.get().get(chooseSensor - 1).getType(), true);
                            }
                            break;
                        case 2:
                            List<String> types=new LinkedList<>();
                            for (int i = 0; i < list.get().size(); i++) {
                                if(!types.contains(list.get().get(i).getType())){
                                    types.add(list.get().get(i).getType());
                                }
                            }
                            System.out.println("Available Types:");
                            for (int i = 0; i < types.size(); i++) {
                                System.out.println("[" + (i + 1) + "] " + types.get(i));
                            }
                            chooseSensor = scanner.nextInt();
                            if (chooseSensor > 0 && chooseSensor <= list.get().size()) {
                                getInfoFromMeasurements(list.get().get(chooseSensor - 1).getId(), list.get().get(chooseSensor - 1).getType(), false);
                            }
                            break;
                        case 3:
                            System.out.println("Available Sensors:");
                            for (int i = 0; i < list.get().size(); i++) {
                                System.out.println("[" + (i + 1) + "] " + list.get().get(i).toClientInterface());
                            }
                            chooseSensor = scanner.nextInt();
                            if (chooseSensor > 0 && chooseSensor <= list.get().size()) {
                                getLastMeasurement(list.get().get(chooseSensor - 1).getId(), list.get().get(chooseSensor - 1).getType());
                            }
                            break;

                    }
                }
                if (choose == -1) {
                    break;
                }else{
                    System.out.println("Press enter for return to main page (will clear this measurements)");
                    System.in.read();
                    clearScreen();
                }
            }
            thread.stopMe();
            int res = target.path("user/{username}").resolveTemplate("username", username).request().delete().getStatus();
            if (res == 200) {
                System.out.println("Closed");
            }
            System.exit(0);
        }
    }

    public static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    public static void getInfoFromMeasurements(String id, String type, boolean requestById) {
        System.out.println("Enter t1 (format HH:MM)");
        String t1 = scanner.next();
        Calendar c = Calendar.getInstance();
        long t1long, t2long;
        if (t1.split(":").length == 2) {
            c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(t1.split(":")[0]) + 1);
            c.set(Calendar.MINUTE, Integer.parseInt(t1.split(":")[1]));
            c.set(Calendar.YEAR, 1970);
            c.set(Calendar.MONTH, 0);
            c.set(Calendar.WEEK_OF_MONTH, 1);
            c.set(Calendar.DAY_OF_MONTH, 1);
            t1long = c.getTimeInMillis();
        } else {
            System.out.println("Wrong Format, Retry");
            return;
        }
        System.out.println("Enter t2 (format HH:MM)");
        String t2 = scanner.next();
        if (t2.split(":").length == 2) {
            c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(t2.split(":")[0]) + 1);
            c.set(Calendar.MINUTE, Integer.parseInt(t2.split(":")[1]));
            t2long = c.getTimeInMillis();
        } else {
            System.out.println("Wrong Format, Retry");
            return;
        }
        Future<MeasurementsByFilter> result = target.path("user/measurements/" + (requestById ? "BySensor" : "ByType")).request().buildPost(Entity.json(new ClientRequest(id, type, t1long, t2long))).submit(new GenericType<MeasurementsByFilter>() {
        });
        DecimalFormat df=new DecimalFormat("###.000");
        try {
            System.out.println("Max:" + df.format(result.get().max));
            System.out.println("Average:" + df.format(result.get().mid));
            System.out.println("Min:" + df.format(result.get().min));
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void getLastMeasurement(String id, String type) {
        Future<Measurement> listM = target.path("user/measurements/{key}").resolveTemplate("key", id + "-" + type).request().buildGet().submit(new GenericType<Measurement>() {
        });
        try {
            DecimalFormat df=new DecimalFormat("###.000");
            System.out.println("Last Measurement:"+df.format(Double.parseDouble(listM.get().getValue())));
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}
