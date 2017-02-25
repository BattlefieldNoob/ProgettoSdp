package server.data;




import server.simulator.Measurement;

import java.util.*;

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
                List<Measurement> list;
                if (measurementsHash.containsKey(key)) {
                    //se lo contiene, prendi la lista e inserisci le nuove misurazioni
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
        List<Measurement> measurementsByID = new LinkedList<>();
        synchronized (measurementsHash) {
            Set<Map.Entry<String, List<Measurement>>> set = measurementsHash.entrySet();
            for (Map.Entry<String, List<Measurement>> entry : set) {
                if (entry.getKey().contains(key)) {
                    measurementsByID.addAll(entry.getValue());
                }
            }
        }
        return measurementsByID;
    }

    public List<ServerData> getAllIDs() {
        List<ServerData> IDs= new LinkedList<>();
        synchronized (measurementsHash) {
            for(String id:measurementsHash.keySet()) {
                IDs.add(new ServerData(id));
            }
        }
        return IDs;
    }

    public List<ServerData> getAllTypes() {
        List<ServerData> types= new LinkedList<>();
        Set<Map.Entry<String, List<Measurement>>> set = measurementsHash.entrySet();
        for (Map.Entry<String, List<Measurement>> entry : set) {
            if (!types.contains(entry.getValue().get(0).getType())){
                types.add(new ServerData(entry.getValue().get(0).getType()));
            }
        }
        return types;
    }

    public Measurement readLastById(String key) {
        synchronized (measurementsHash) {
            List<Measurement> measurements = measurementsHash.get(key);
            return measurements.get(measurements.size() - 1);
        }
    }
}
