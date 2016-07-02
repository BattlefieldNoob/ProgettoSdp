package server.data;


import java.util.LinkedList;
import java.util.List;

/**
 * Created by antonio on 06/05/16.
 */
public class SensorsDB {

    private static SensorsDB instance;
    private final List<SensorData> sensorsList = new LinkedList<>();

    public static SensorsDB getInstance() {
        if (instance == null) {
            instance = new SensorsDB();
        }
        return instance;
    }

    public boolean create(SensorData newsensor) {
        synchronized (sensorsList) {
            for (SensorData sensor : sensorsList) {
                if (sensor.getId().equals(newsensor.getId())) {
                    return false;
                }
            }
            sensorsList.add(newsensor);
            return true;
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
        synchronized (sensorsList) {
            return sensorsList;
        }
    }

    public SensorData readNext(String id) {
        for (SensorData sensor : sensorsList) {
            if (sensor.getId().equals(id)) {
                //Ti ho trovato
                int next = sensorsList.indexOf(sensor) + 1;
                if (next >= sensorsList.size()) {
                    next = 0;
                }
                return sensorsList.get(next);
            }
        }
        return null;
    }

    public boolean delete(String id) {
        for (SensorData sensor : sensorsList) {
            if (sensor.getId().equals(id)) {
                //Ti ho trovato
                sensorsList.remove(sensor);
                return true;
            }
        }
        return false;
    }
}
