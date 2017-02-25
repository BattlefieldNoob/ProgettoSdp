package sensor;

import sensor.utility.Logging;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * Created by antonio on 27/05/16.
 * la classe tiene traccia dei SensorInputThread esistenti e agisce se si verificano 2 connessioni
 */
class SensorUdpThread extends Thread {

    private DatagramSocket in;
    private boolean running = true;
    private SensorHandle sensor;
    private Logging log = Logging.getInstance();


    SensorUdpThread(SensorHandle sensor, int port) {
        this.sensor = sensor;
        try {
            in = new DatagramSocket(port);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        start();
    }

    void stopMe() {
        running = false;
        in.close();
    }

    @Override
    public void run() {
        while (running) {
            byte[] buffer = new byte[1024];
            DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
            try {
                in.receive(dp);
                String event = new String(dp.getData(), 0, dp.getLength());
                if (event.contains("CLOSE")) {
                    sensor.closeSensorFromUdp();
                    running = false;
                }
            } catch (IOException e) {
                if(running){
                    e.printStackTrace();
                }
            }
        }
        log.info(getClass().getSimpleName() + " Terminated", getClass().getSimpleName());
    }
}
