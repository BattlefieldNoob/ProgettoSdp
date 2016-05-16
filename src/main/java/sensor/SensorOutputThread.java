package sensor;

import com.google.gson.Gson;
import sensor.data.Ussaro;
import server.data.SensorData;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import java.io.*;
import java.net.*;

/**
 * Created by antonio on 11/05/16.
 */
public class SensorOutputThread extends Thread{
    DataOutputStream out;
    DataInputStream in;
    Socket nextSocket;
    TestSensor sensor;
    boolean sendToMyself=false;
    boolean running=true;
    SensorData nextdata;
    javax.ws.rs.client.Client client;
    WebTarget target;
    Gson gson=new Gson();
    String TAG=getClass().getSimpleName();
    public Object lock;


    public SensorOutputThread(TestSensor thisSensor,Object lockObject) throws IOException {
        this.lock=lockObject;
        this.sensor=thisSensor;
        client = ClientBuilder.newClient();
        target = client.target("http://localhost:8080").path("appsdp/sensor");
        System.out.println("SensorOutputReady");
    }

    public void resetSocket() throws IOException {
        if(nextSocket!=null) {
            synchronized (nextSocket) {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
                if (!nextSocket.isClosed()) {
                    nextSocket.close();
                }
            }
        }
        nextSocket = null;
        out = null;
        in = null;
    }

    public void configureConnectionWithNext(SensorData next) throws InterruptedException, IOException {
        if(next==null){
            System.out.println(TAG+" non ho un sensore successivo");
            resetSocket();
        }
        else{
            System.out.println(TAG+" il sensore successivo è:"+next.getAddress() +":"+next.getPort());
            if(nextSocket!=null && !nextSocket.isClosed() && nextSocket.getInetAddress().equals(InetAddress.getByName(next.getAddress())) && nextSocket.getPort()==next.getPort()){
                System.out.println("Sono già conneso, non faccio nulla");
            }else {
                System.out.println("Tento la connessione con il next");
                resetSocket();
                //tento la connessione
                boolean connected = false;
                while (!connected) {
                    try {
                        nextSocket = new Socket(next.getAddress(), next.getPort());
                        out = new DataOutputStream(nextSocket.getOutputStream());
                        in = new DataInputStream(nextSocket.getInputStream());
                        System.out.println(TAG + " Connected to nextSocket");
                        connected = true;
                    } catch (IOException e) {
                        System.out.println(TAG + " not connected, retry");
                        sleep(1000);
                    }
                }
            }
        }
    }

    public void sendUdpMessage(Ussaro message,SensorData sendTo){
        DatagramSocket ds = null;
        try {
            ds = new DatagramSocket();
            String data=gson.toJson(message);
            System.out.println("UDP Send to "+InetAddress.getByName(sendTo.getAddress())+" "+sendTo.getPort());
            DatagramPacket dp = new DatagramPacket(data.getBytes(), data.getBytes().length, InetAddress.getByName(sendTo.getAddress()), sendTo.getPort());
            ds.send(dp);
            System.out.println("UDP Send ok");
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopMe(){
        running=false;
       /* synchronized (lock){
            lock.notify();
        }*/
    }


    @Override
    public void run(){
        while(running || sensor.isDataAvaiable()){//se running e sono disponibili info
            if(!running){
                System.out.println("Sto ancora inviando, ora esco");
            }
            synchronized (lock) {
                try {
                    if(sensor.isSendToMyself()){
                        System.out.println("invio a me stesso");
                        sensor.getComputedToken();
                    }
                    if (nextSocket != null) {
                        synchronized (nextSocket) {
                            if(out != null && !nextSocket.isClosed()) {
                                if (!sensor.isDataAvaiable()) {
                                    System.out.println(TAG + " : waiting for data");
                                    lock.wait();//waiting for computed data
                                }
                                System.out.println(TAG + " : send data to " + nextSocket.getInetAddress().toString() + " port " + nextSocket.getPort());
                                String message;
                                if (sensor.isDataAvaiable()) {
                                    message = gson.toJson(sensor.getComputedToken());
                                    sensor.token = null;
                                    out.writeUTF(message); //send data
                                    System.out.println("wait for ACK");
                                    in.read();//aspetto l'ok
                                    System.out.println(TAG + " : data sended :" + message);
                                }
                            }
                        }
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    System.out.println("Tutto ok");
                    e.printStackTrace();
                }
            }
        }
        System.out.println(getClass().getSimpleName()+" Terminated");
    }
}
