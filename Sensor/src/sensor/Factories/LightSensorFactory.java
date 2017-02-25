package sensor.Factories;

import sensor.data.SensorBuffer;
import sensor.simulator.*;

/**
 * Created by antonio on 04/07/16.
 */
public class LightSensorFactory extends AbstractSensorFactory {

    private Buffer<Measurement> buffer=new SensorBuffer();

    @Override
    public SensorSimulator createSensorSimulator(String id) {
        return new SensorSimulator(new LightSimulator(id,buffer),buffer);
    }
}
