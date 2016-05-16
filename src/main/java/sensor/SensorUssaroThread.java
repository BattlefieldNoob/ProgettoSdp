package sensor;

import com.google.gson.Gson;
import sensor.Sensor;
import sensor.data.Ussaro;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * Created by antonio on 13/05/16.
 * La classe si occupa di ricevere messaggi udp per informare sull variazioni della rete
 *
 */
public class SensorUssaroThread extends Thread {


    int port;
    boolean running=true;
    DatagramSocket ds;
    TestSensor sensor;

    SensorUssaroThread(TestSensor sensor,int port){
        this.port=port;
        this.sensor=sensor;
    }

    public void stopMe(){
        running=false;
        ds.close();
    }

    @Override
    public void run(){
        try {
            ds=new DatagramSocket(port);
            Gson gson = new Gson();
            while (running){
                byte[] buffer = new byte[1024];
                DatagramPacket dp=new DatagramPacket(buffer,buffer.length);
                System.out.println("waiting for udp");
                ds.receive(dp);
                String message=new String(dp.getData(),0,dp.getLength());
                System.out.println("UDP Message:"+message);
                sensor.elaborateUssaro(gson.fromJson(message,Ussaro.class));
            }
        } catch (SocketException e){
            System.out.println("Exit from outputThread");
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Thread Exited");
    }


}
