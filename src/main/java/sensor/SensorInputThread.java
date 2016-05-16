package sensor;

import com.google.gson.Gson;
import sensor.data.Token;

import javax.xml.crypto.Data;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

/**
 * Created by antonio on 11/05/16.
 * Thread che accetta le richieste dal nodo precedente
 */
public class SensorInputThread extends Thread {

    TestSensor sensor;
    private boolean running=true;
    boolean connected=false;
    DataInputStream in;
    DataOutputStream out;
    ServerSocket server;
    Socket prevSocket;
    private SensorComputeThread computeThread;
    private Object lock;
    private int port;
    private String TAG=getClass().getSimpleName();

    public SensorInputThread (TestSensor sensor,Object lock,int port){
        this.sensor=sensor;
        this.lock=lock;
        this.port=port;
        try {
            server = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopMe(){
        running=false;
        try {
            if(prevSocket!=null) {
                in.close();
                out.close();
                prevSocket.close();
            }
            if(server!=null && !server.isClosed())
                server.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        Gson gson=new Gson();
        while(running) {
            //è connesso a qualcosa?
            if(server == null || prevSocket == null || in==null){
                //non è connesso, quindi mi metto in ascolto
                System.out.println(TAG+" In attesa di connessioni...");
                try {
                    prevSocket = server.accept();
                    in = new DataInputStream(prevSocket.getInputStream());
                    out= new DataOutputStream(prevSocket.getOutputStream());
                    System.out.println(TAG+" Connected");
                } catch (IOException e) {
                    System.out.println("Tutto calcolato");
                   // e.printStackTrace();
                }
            }else {
                String message="";
                System.out.println(TAG+" In attesa di cose...");
                try {
                    message=in.readUTF();
                    out.write(255);//invio ACK
                    System.out.println(TAG+" letto :"+message);
                    //ho letto qualcosa, la rendo disponibile e "avviso" outputThread
                    System.out.println(TAG+" Setto il messaggio come 'da computare'");
                    sensor.setToken(gson.fromJson(message, Token.class));
                } catch (IOException e) {
                    System.out.println("Tutto ok");
                }

            }
        }
        System.out.println(getClass().getSimpleName()+" Terminated");
    }

    public void resetServer(){
        if(prevSocket!=null && !prevSocket.isClosed()){
            try {
                in.close();
                prevSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        prevSocket=null;
        in=null;
        connected=false;
    }
}
