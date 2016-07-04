package sensor.Factories;

import sensor.simulator.SensorSimulator;
import sensor.utility.Logging;

/**
 * Created by antonio on 04/07/16.
 */
abstract public class AbstractSensorFactory {

    public static AbstractSensorFactory getFactory(String type){
        System.out.println("return factory for type "+type);
        switch (type){
            case "light":
                return new LightSensorFactory();
            case "temperature":
                return new TemperatureSensorFactory();
            case "accelerometer":
                return new AccelerometerSensorFactory();
            default:
                return new ErrorSensor(type);
        }
    }

    public abstract SensorSimulator createSensorSimulator(String id);

    private static class ErrorSensor extends AbstractSensorFactory{

        ErrorSensor(String type){
            System.err.println("Type not recognized ("+type+"), Please Stop this program");
        }
        @Override
        public SensorSimulator createSensorSimulator(String id) {
            System.err.println("Type not recognized, this program will stop now");
            System.exit(1);
            return null;
        }
    }
}
