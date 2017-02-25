package utils;

import server.client.MeasurementsByFilter;
import server.data.ServerData;
import server.sensor.SensorData;
import simulator.Measurement;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

/**
 * Created by antonio on 18/02/17.
 */
public class UIUtils {

    private static UIUtils Instance;

    private Scanner scanner;

    private GatewayUtils gatewayUtils;

    private UdpThread thread;


    private UIUtils(GatewayUtils gatewayUtils) {
        scanner = new Scanner(System.in);
        thread = new UdpThread(gatewayUtils.clientPort);
        thread.start();
        this.gatewayUtils = gatewayUtils;
    }

    public static UIUtils getInstance(GatewayUtils gu) {
        if (Instance == null)
            Instance = new UIUtils(gu);
        return Instance;
    }

    private int showMenu() {
        System.out.println("[1] List sensors");
        System.out.println("[2] Ask Filtered Measurements by ID");
        System.out.println("[3] Ask Filtered Measurements by Type");
        System.out.println("[4] Ask Last Measurement by Sensor");
        System.out.println("[5] Start a new sensor");
        System.out.println("[6] Stop a Sensor");
        System.out.println("[-1] Log out and Exit");
        return scanner.nextInt();
    }

    public void mainLoop() {
        while (true) {
            int choose = showMenu();
            switch (choose) {
                case 1:
                    printAllSensor();
                    break;
                case 2:
                    filterByID();
                    break;
                case 3:
                    filterByType();
                    break;
                case 4:
                    lastMeasurement();
                    break;
                case 5:
                    startSensor();
                    break;
                case 6:
                    stopSensor();
                    break;
                default:
                    break;
            }
            if (choose == -1) {
                break;
            } else {
                System.out.println("Press enter for return to main page (will clear this screen)");
                try {
                    System.in.read();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                clearScreen();
            }
        }
    }

    public String login() {
        String username;
        int result;
        do {
            System.out.println("Please write your username:");
            username = scanner.next();
            result = gatewayUtils.login(username);
            if (result == 401) {
                System.out.println("Username Already used, please retry");
            } else if (result == 200) {
                System.out.println("Login successfully");
                break;
            }
        } while (true);
        return result == 200 ? username : null;
    }

    private void startSensor() {
        System.out.println("Available Sensor types:");
        String[] types = new String[]{"Temperature","Light","Accelerometer"};
        int chooseSensor = printSensorTypes();
        if (chooseSensor > 0 && chooseSensor <= types.length) {
            DataUtils.startSensor(types[chooseSensor - 1]);
            System.out.println("Starting...");
        }else{
            System.out.println("Please select a valid type");
        }
    }

    private void stopSensor() {
        List<SensorData> list = gatewayUtils.getSensors();
        if (!list.isEmpty()) {
            System.out.println("Available Sensors:");
            int chooseSensor = printSensor();
            if (chooseSensor > 0 && chooseSensor <= list.size()) {
                thread.SendCloseMessage(list.get(chooseSensor - 1));
            }
        } else {
            System.out.println("No Sensors available");
        }
    }

    private void filterByID() {
        List<SensorData> list = gatewayUtils.getSensors();
        List<ServerData> allIds = gatewayUtils.getAllIDs();
        int chooseSensor = printSensorsId(allIds,list);
        if(allIds.isEmpty()){
            System.out.println("No data available");
        }else {
            if (chooseSensor > 0 && chooseSensor <= allIds.size()) {
                scanner.nextLine();
                long t1long, t2long;
                System.out.println("Enter t1 (format HH:MM)");
                String t1 = scanner.nextLine();
                t1long = DataUtils.timeStringToMillis(t1);

                System.out.println("Enter t2 (format HH:MM)");
                String t2 = scanner.nextLine();
                t2long = DataUtils.timeStringToMillis(t2);

                MeasurementsByFilter m = gatewayUtils.getMeasurements(allIds.get(chooseSensor - 1).getMessage(), null, t1long, t2long, true);
                if(m.status.equalsIgnoreCase("NO_DATA")){
                    System.out.println("No data available for this id");
                }else if(m.status.equalsIgnoreCase("TOO-EARLY")){
                    System.out.println("Interval does not contain measurements");
                }else if(m.status.equalsIgnoreCase("TOO-LATE")) {
                    System.out.println("Measuring not yet collected");
                }else{
                    DecimalFormat df = new DecimalFormat("###.000");
                    System.out.println("Max:" + df.format(m.max));
                    System.out.println("Average:" + df.format(m.mid));
                    System.out.println("Min:" + df.format(m.min));
                }
            }
        }
    }

    private void filterByType() {
        //      List<String> types = new LinkedList<>();
        List<SensorData> list = gatewayUtils.getSensors();
/*
        for (SensorData item : list) {
            if (!types.contains(item.getType())) {
                types.add(item.getType());
            }
        }*/

        List<ServerData> types = gatewayUtils.getAllTypes();

        int chooseSensor = printSensorsType(types);
        if (types.isEmpty()) {
            System.out.println("No data available");
        } else {
            if (chooseSensor > 0 && chooseSensor <= types.size()) {
                scanner.nextLine();
                long t1long, t2long;
                System.out.println("Enter t1 (format HH:MM)");
                String t1 = scanner.nextLine();
                t1long = DataUtils.timeStringToMillis(t1);

                System.out.println("Enter t2 (format HH:MM)");
                String t2 = scanner.nextLine();
                t2long = DataUtils.timeStringToMillis(t2);

                MeasurementsByFilter m = gatewayUtils.getMeasurements(null, types.get(chooseSensor - 1).getMessage(), t1long, t2long, false);

                if(m.status.equalsIgnoreCase("NO_DATA")){
                    System.out.println("No data available for this type");
                }else if(m.status.equalsIgnoreCase("TOO-EARLY")){
                    System.out.println("Interval does not contain measurements");
                }else if(m.status.equalsIgnoreCase("TOO-LATE")){
                    System.out.println("Measuring not yet collected");
                }else {
                    DecimalFormat df = new DecimalFormat("###.000");
                    System.out.println("Max:" + df.format(m.max));
                    System.out.println("Average:" + df.format(m.mid));
                    System.out.println("Min:" + df.format(m.min));
                }
            }
        }
    }

    private void lastMeasurement() {
        List<SensorData> list = gatewayUtils.getSensors();
        if(list.isEmpty()){
            System.out.println("No data available");
        }else {
            System.out.println("Available Sensors:");
            int chooseSensor = printSensor();
            if (chooseSensor > 0 && chooseSensor <= list.size()) {
                Measurement m = gatewayUtils.getLastMeasurement(list.get(chooseSensor - 1).getId(), list.get(chooseSensor - 1).getType());
                DecimalFormat df = new DecimalFormat("###0.000");
                double value=Double.parseDouble(m.getValue());
                String timestamp=millisToHuman(m.getTimestamp());
                System.out.println("Last Measurement:" + df.format(value) + " TimeStamp: "+timestamp);
            }
        }
    }

    String millisToHuman(long millis){
        Date date = new Date(millis);
        DateFormat formatter = new SimpleDateFormat("HH:mm:ss:SSS");
        return formatter.format(date);
    }

    private int printSensor() {
        List<SensorData> list = gatewayUtils.getSensors();
        if (!list.isEmpty()) {
            for (int i = 0; i < list.size(); i++) {
                System.out.println("[" + (i + 1) + "] " + list.get(i).toClientInterface());
            }
            return scanner.nextInt();
        } else {
            return -1;
        }
    }


    private void printAllSensor() {
        List<SensorData> list = gatewayUtils.getSensors();

        if (!list.isEmpty()) {
            System.out.println("Available Sensors:");
            for (int i = 0; i < list.size(); i++) {
                System.out.println("[" + (i + 1) + "] " + list.get(i).toClientInterface());
            }
        } else {
            System.out.println("No sensors available");
        }
    }




    private int printSensorTypes() {
        String[] types = new String[]{"Temperature","Light","Accelerometer"};
            for (int i = 0; i < types.length; i++) {
                System.out.println("[" + (i + 1) + "] " + types[i]);
            }
            return scanner.nextInt();
    }

    private static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }


    private int printSensorsId(List<ServerData> allid, List<SensorData> connected) {
        if (!allid.isEmpty()) {
            for (int i = 0; i < allid.size(); i++) {
                boolean nowConnected = false;
                for (SensorData s : connected) {
                    if (allid.get(i).getMessage().contains(s.getId())) {
                        nowConnected = true;
                        break;
                    }
                }
                System.out.println("[" + (i + 1) + "] " + allid.get(i).getMessage() + (nowConnected ? " (Connected) " : " (Disconnected)"));
            }
            return scanner.nextInt();
        } else {
            return -1;
        }
    }

    private int printSensorsType(List<ServerData> alltypes) {
        if (!alltypes.isEmpty()) {
            for (int i = 0; i < alltypes.size(); i++) {
                System.out.println("[" + (i + 1) + "] " + alltypes.get(i).getMessage() );
            }
            return scanner.nextInt();
        } else {
            return -1;
        }
    }
}

