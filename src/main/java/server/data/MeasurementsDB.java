package server.data;


import server.simulator.Measurement;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by antonio on 12/05/16.
 */
public class MeasurementsDB {

    private static MeasurementsDB instance;
    private final HashMap<String, List<Measurement>> measurementsHash = new HashMap<>();

    public static MeasurementsDB getInstance() {
        if (instance == null) {
            instance = new MeasurementsDB();
        }
        return instance;
    }

    public boolean addMeasurements(List<Measurement> measurements) {
        synchronized (measurementsHash) {
            for (Measurement measurement : measurements) {
                String key = measurement.getId() + "-" + measurement.getType();
                System.out.println("working with key " + key);
                List<Measurement> list;
                if (measurementsHash.containsKey(key)) {//se lo contiene, prendi la lista e allungala
                    list = measurementsHash.get(key);
                } else {//se non lo contiene, creala
                    list = new LinkedList<>();
                }
                list.add(measurement);
                measurementsHash.put(key, list);
            }
            return true;
        }
    }

    public List<Measurement> readById(String key) {
        synchronized (measurementsHash) {
            return measurementsHash.get(key);
        }
    }
}
