package sensor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * Created by antonio on 12/05/16.
 */
class PushThread extends Thread {

    private int port;
    private boolean running = true;
    private DatagramSocket ds;

    PushThread(int port) {
        this.port = port;
    }

    public void stopMe() {
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
                String event =new String(dp.getData(), 0, dp.getLength());
                JsonParser parser=new JsonParser();
                JsonObject obj=parser.parse(event).getAsJsonObject();
                if(obj.getAsJsonPrimitive("event").getAsString().equals("Enter")){
                    System.out.println("SensorHandle "+obj.getAsJsonPrimitive("id").getAsString()+" (type="+ obj.getAsJsonPrimitive("type").getAsString()+
                            ") at "+obj.getAsJsonPrimitive("address").getAsString()+":"+obj.getAsJsonPrimitive("port").getAsString()+" Entered into network");
                }else{
                    System.out.println("SensorHandle "+obj.getAsJsonPrimitive("id").getAsString()+" (type="+ obj.getAsJsonPrimitive("type").getAsString()+
                            ") at "+obj.getAsJsonPrimitive("address").getAsString()+":"+obj.getAsJsonPrimitive("port").getAsString()+" Left the network");
                }
            }
        } catch (SocketException e) {
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
