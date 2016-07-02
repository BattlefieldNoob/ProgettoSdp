package sensor;

import sensor.utility.Logging;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

/**
 * Created by antonio on 27/05/16.
 * la classe tiene traccia dei SensorInputThread esistenti e agisce se si verificano 2 connessioni
 */
class SensorServerThread extends Thread {

    LinkedList<SensorInputThread> inputThreads = new LinkedList<>();
    private boolean running = true;
    private ServerSocket server;
    private TestSensor sensor;
    private Logging log = Logging.getInstance();

    SensorServerThread(TestSensor sensor, int port) {
        this.sensor = sensor;
        try {
            server = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void stopMe() {
        running = false;
        inputThreads.get(0).closeThread();
        inputThreads.get(0).interrupt();
        System.out.println("Wait for input thread to close");
        try {
            inputThreads.get(0).join();
        } catch (InterruptedException e) {
            System.out.println("Thread Closed");
        }
        inputThreads.clear();
        try {
            System.out.println("Closing Server");
            server.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (running) {
            //non è connesso, quindi mi metto in ascolto
            log.info("Waiting for connections...", getClass().getSimpleName());
            try {
                Socket prev = server.accept();
                inputThreads.add(new SensorInputThread(sensor, prev));
                if (inputThreads.size() > 1) {
                    SensorInputThread toRemove = inputThreads.removeFirst();
                    toRemove.closeThread();
                    toRemove.join();
                }
                log.info("Connected", getClass().getSimpleName());
            } catch (IOException e) {
                log.info("Exception OK", getClass().getSimpleName());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        log.info("Terminated", getClass().getSimpleName());
    }
}
