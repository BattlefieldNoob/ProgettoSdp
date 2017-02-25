package utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import server.sensor.SensorData;

import java.io.IOException;
import java.net.*;

/**
 * Created by antonio on 12/05/16.
 */
public class UdpThread extends Thread {

    private int port;
    private boolean running = true;
    private DatagramSocket ds;

    UdpThread(int port) {

        this.port = port;
    }

    void stopMe() {
        running = false;
        ds.close();
    }

    @Override
    public void run() {
        try {
            ds = new DatagramSocket(port);
            while (running) {
                byte[] buffer = new byte[1024];
                DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
                ds.receive(dp);
                String udpevent = new String(dp.getData(), 0, dp.getLength());
                JsonParser parser = new JsonParser();
                JsonObject obj = parser.parse(udpevent).getAsJsonObject();
                String event=obj.getAsJsonPrimitive("event").getAsString();
                String id=obj.getAsJsonPrimitive("id").getAsString();
                String type=obj.getAsJsonPrimitive("type").getAsString();
                String address=obj.getAsJsonPrimitive("address").getAsString();
                String port=obj.getAsJsonPrimitive("port").getAsString();
                System.out.println("Sensor " + id + " (type=" + type +") at " + address + ":" + port+
                        (event.equalsIgnoreCase("Enter") ? " is entered into network":" is exited from network"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    void SendCloseMessage(SensorData toClose) {
        try {
            byte[] buffer = "CLOSE".getBytes();
            DatagramPacket dp = new DatagramPacket(buffer, buffer.length,InetAddress.getByName(toClose.getAddress()),toClose.getPort());
            ds.send(dp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
