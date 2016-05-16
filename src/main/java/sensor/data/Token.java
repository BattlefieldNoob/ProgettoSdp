package sensor.data;

import sensor.simulator.Buffer;
import sensor.simulator.Measurement;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by antonio on 10/05/16.
 */
public class Token implements Buffer<Measurement>{

    private static Token instance;

    private List<Measurement> measurements = new ArrayList<>(15);

    public static Token getInstance(){
        if(instance==null){
            instance=new Token();
        }
        return instance;
    }

    private Token(){}

    public void addMeasurement(Measurement m){
        measurements.add(m);
    }

    @Override
    public void add(Measurement m) {
        measurements.add(m);
    }

    public boolean isFull(){
        return measurements.size()>=15;
    }

    public int size(){
        return measurements.size();
    }

    @Override
    public List<Measurement> readAllAndClean() {
        List<Measurement> toReturn=new ArrayList<>(measurements);
        measurements.clear();
        return toReturn;
    }
}
