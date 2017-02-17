package sensor.Factories;

import sensor.data.SensorBuffer;
import sensor.simulator.*;

/**
 * Created by antonio on 04/07/16.
 */
public class LightSensorFactory extends AbstractSensorFactory {

    Buffer<Measurement> buffer=new SensorBuffer();

    public LightSensorFactory(){
        System.out.println("factory of type light has been created");
    }
    @Override
    public SensorSimulator createSensorSimulator(String id) {
        System.out.println("Sensor of type light has been created");
        return new SensorSimulator(new LightSimulator(id,buffer),buffer);
    }
}
