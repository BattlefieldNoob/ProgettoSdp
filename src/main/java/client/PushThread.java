package client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * Created by antonio on 12/05/16.
 */
public class PushThread extends Thread {

    int port;
    boolean running=true;
    DatagramSocket ds;

    PushThread(int port){
        this.port=port;
    }

    public void stopMe(){
        running=false;
        ds.close();
    }

    @Override
    public void run(){
        try {
            ds=new DatagramSocket(port);
            while (running){
                byte[] buffer = new byte[1024];
                DatagramPacket dp=new DatagramPacket(buffer,buffer.length);
                ds.receive(dp);
                System.out.println("Received: "+new String(dp.getData(),0,dp.getLength()));
            }
        } catch (SocketException e) {
            //e.printStackTrace();
            System.out.println("Exit from thread");
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Thread Exited");
    }
}
