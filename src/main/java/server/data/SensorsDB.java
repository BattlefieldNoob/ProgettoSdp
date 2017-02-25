package server.data;


import server.sensor.SensorData;

import java.util.*;

/**
 * Created by antonio on 06/05/16.
 */
public class SensorsDB {

    private static SensorsDB instance;
    private final List<SensorData> sensorsList = new LinkedList<>();

    private static Integer firstFreePort=9000;
    private static Integer firstFreeID=1;


    private static final Queue<Integer> freedPorts=new LinkedList<>();
    private static final Queue<Integer> freedIDs=new LinkedList<>();


    private static final HashMap<String,Long> sensorsUpTime=new LinkedHashMap<>();

    public static SensorsDB getInstance() {
        if (instance == null) {
            instance = new SensorsDB();
        }
        return instance;
    }

    public int create(SensorData newsensor) {
        synchronized (sensorsList) {
            for (SensorData sensor : sensorsList) {
                if (sensor.getId()!=null && sensor.getId().equals(newsensor.getId())) {
                    return -1;
                }
            }
            newsensor.setPort(bindNextPort());
            newsensor.setId(bindNextID());
            sensorsList.add(newsensor);
            synchronized (sensorsUpTime){
                sensorsUpTime.put(newsensor.getId(),System.currentTimeMillis());
            }
            return sensorsList.indexOf(newsensor);
        }
    }

    public SensorData read(String id) {
        synchronized (sensorsList) {
            for (SensorData sensor : sensorsList) {
                if (sensor.getId().equals(id)) {
                    return sensor;
                }
            }
            return null;
        }
    }

    public List<SensorData> readAll() {
        List<SensorData> list=new LinkedList<>();
        synchronized (sensorsList) {
            for(SensorData sensor:sensorsList) {
                sensor.setUptime(readUptime(sensor.getId()));
                list.add(sensor);
            }
        }
        return list;
    }

    public SensorData readNext(String id) {
        for (SensorData sensor : sensorsList) {
            if (sensor.getId().equals(id)) {
                int next = sensorsList.indexOf(sensor) + 1;
                if (next >= sensorsList.size()) {
                    next = 0;
                }
                return sensorsList.get(next);
            }
        }
        return null;
    }

    public long readUptime(String id){
        long uptime=0;
        synchronized (sensorsUpTime){
            uptime=System.currentTimeMillis()-sensorsUpTime.get(id);
        }
        return uptime;
    }

    public boolean delete(String id) {
        for (SensorData sensor : sensorsList) {
            if (sensor.getId().equals(id)) {
                sensorsList.remove(sensor);
                freeID(sensor.getId());
                freePort(sensor.getPort());
                synchronized (sensorsUpTime){
                    sensorsUpTime.remove(sensor.getId());
                }
                return true;
            }
        }
        return false;
    }

    public int bindNextPort(){
        int port;
        synchronized (freedPorts) {
            if (!freedPorts.isEmpty()) {
                port = freedPorts.remove();
            } else {
                synchronized (firstFreePort) {
                    port = firstFreePort;
                    firstFreePort++;
                }
            }
        }
        return port;
    }

    public String bindNextID(){
        int id;
        synchronized (freedIDs) {
            if(!freedIDs.isEmpty()){
                id = freedIDs.remove();
            }else {
                synchronized (firstFreeID) {
                    id = firstFreeID;
                    firstFreeID++;
                }
            }
        }
        return "N0"+id;
    }


    private void freePort(int port){
        synchronized (freedPorts){
            freedPorts.add(port);
        }
    }

    private void freeID(String id){
        synchronized (freedIDs){
            freedIDs.add(Integer.parseInt(id.split("N0")[1]));
        }
    }

}
