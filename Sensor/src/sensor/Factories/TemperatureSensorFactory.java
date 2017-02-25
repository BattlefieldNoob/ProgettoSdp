package sensor.Factories;

import sensor.data.SensorBuffer;
import sensor.simulator.*;

/**
 * Created by antonio on 04/07/16.
 */
public class TemperatureSensorFactory extends AbstractSensorFactory {

    public Buffer<Measurement> buffer=new SensorBuffer();

    @Override
    public SensorSimulator createSensorSimulator(String id) {
        return new SensorSimulator(new TemperatureSimulator(id,buffer),buffer);
    }
}
