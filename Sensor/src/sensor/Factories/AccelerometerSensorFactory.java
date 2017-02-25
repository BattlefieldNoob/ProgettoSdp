package sensor.Factories;

import sensor.data.SensorBuffer;
import sensor.simulator.*;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by antonio on 04/07/16.
 */
class AccelerometerSensorFactory extends AbstractSensorFactory {

    private final Buffer<Measurement> buffer=new SensorBuffer();

    private final Buffer<Measurement> slidingWindowBuffer = new SensorBuffer();

    private final Timer timer=new Timer();

    @Override
    public SensorSimulator createSensorSimulator(String id) {
        timer.schedule(new SlidingWindow(),0,1000);
        return new SensorSimulator(new AccelerometerSimulator(id,buffer),slidingWindowBuffer);
    }

    private class SlidingWindow extends TimerTask{
        @Override
        public void run() {
            synchronized (buffer) {
                List<Measurement> list = buffer.readAllAndClean();
                if (!list.isEmpty()) {
                    Measurement last = list.get(list.size() - 1);
                    double value = 0;
                    for (Measurement measurement : list) {
                        value += Double.valueOf(measurement.getValue());
                    }
                    synchronized (slidingWindowBuffer) {
                        slidingWindowBuffer.add(new Measurement(last.getId(), last.getType(), String.valueOf(value / list.size()), last.getTimestamp()));
                    }
                }
            }
        }
    }
}
