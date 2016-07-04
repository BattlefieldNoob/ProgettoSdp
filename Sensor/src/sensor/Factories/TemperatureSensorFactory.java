package sensor.Factories;

import sensor.data.SensorBuffer;
import sensor.simulator.*;

/**
 * Created by antonio on 04/07/16.
 */
public class TemperatureSensorFactory extends AbstractSensorFactory {

    Buffer<Measurement> buffer=new SensorBuffer();

    public TemperatureSensorFactory(){
        System.out.println("factory of type temperature has been created");
    }

    @Override
    public SensorSimulator createSensorSimulator(String id) {
        System.out.println("Sensor of type temperature has been created");
        return new SensorSimulator(new TemperatureSimulator(id,buffer),buffer);
    }
}
