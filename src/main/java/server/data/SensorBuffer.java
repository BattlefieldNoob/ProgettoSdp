package server.data;


import server.simulator.Buffer;
import server.simulator.Measurement;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by antonio on 11/05/16.
 */
public class SensorBuffer implements Buffer<Measurement> {

    private final List<Measurement> measurements = new LinkedList<>();

    @Override
    public void add(Measurement measurement) {
        synchronized (measurements) {
            measurements.add(measurement);
        }
    }

    @Override
    public List<Measurement> readAllAndClean() {
        List<Measurement> toReturn;
        synchronized (measurements) {
            toReturn = new ArrayList<>(measurements);
            measurements.clear();
        }
        return toReturn;
    }
}
