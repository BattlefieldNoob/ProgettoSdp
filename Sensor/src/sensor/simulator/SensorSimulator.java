package sensor.simulator;

import java.util.List;

/**
 * Created by antonio on 02/07/16.
 */
public class SensorSimulator {

    Simulator sensorSimulator;

    Thread sensorThread;

    Buffer<Measurement> measurementBuffer;
    public SensorSimulator(Simulator sensor, Buffer<Measurement> buffer) {
        measurementBuffer=buffer;
        sensorSimulator=sensor;
        sensorThread = new Thread(sensorSimulator);
        sensorThread.start();
    }

    public List<Measurement> getMeasurementList() {
        return measurementBuffer.readAllAndClean();
    }

    public void stopMeGently() {
        sensorSimulator.stopMeGently();
    }
}
