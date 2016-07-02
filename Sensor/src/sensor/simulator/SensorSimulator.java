package sensor.simulator;

import sensor.data.SensorBuffer;

import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by antonio on 02/07/16.
 */
public class SensorSimulator {

    final Buffer<Measurement> measurementBuffer = new SensorBuffer();
    final List<Measurement> tempMeasurementList = new LinkedList<>();
    Simulator sensorSimulator;
    Thread sensorThread;
    Timer timer = new Timer();
    String sensorType;

    public SensorSimulator(String id, String type) {
        switch (type) {
            case "light":
                sensorSimulator = new LightSimulator(id, measurementBuffer);
                break;
            case "temperature":
                sensorSimulator = new TemperatureSimulator(id, measurementBuffer);
                break;
            case "accelerometer":
                sensorSimulator = new AccelerometerSimulator(id, measurementBuffer);
                timer.schedule(new SensorTimerTask(), 0, 4000);
                break;
            default:
                System.err.println("Error");
                return;
        }
        sensorType = type;
        sensorThread = new Thread(sensorSimulator);
        sensorThread.start();
    }

    public List<Measurement> getMeasurementList() {
        if (sensorType.equals("accelerometer")) {
            synchronized (tempMeasurementList) {
                return tempMeasurementList;
            }
        } else {
            synchronized (measurementBuffer) {
                return measurementBuffer.readAllAndClean();
            }
        }
    }

    public void stopMeGently() {
        sensorSimulator.stopMeGently();
        timer.cancel();
    }

    private class SensorTimerTask extends TimerTask {
        @Override
        public void run() {
            synchronized (tempMeasurementList) {
                synchronized (measurementBuffer) {
                    List<Measurement> measurementList = measurementBuffer.readAllAndClean();
                    if (!measurementList.isEmpty()) {
                        double value = 0;
                        for (Measurement measurement : measurementList) {
                            value += Double.valueOf(measurement.getValue());
                        }
                        tempMeasurementList.add(new Measurement(measurementList.get(0).getId(), measurementList.get(0).getType(), String.valueOf(value / measurementList.size()), measurementList.get(measurementList.size() - 1).getTimestamp()));
                    }
                }
            }
        }
    }
}
