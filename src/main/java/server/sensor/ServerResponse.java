package server.sensor;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Created by antonio on 19/02/17.
 */
@XmlRootElement
public class ServerResponse {

    int port;

    String ID;

    List<SensorData> sensorList;

    public ServerResponse(int port, String ID, List<SensorData> sensorList) {
        this.port = port;
        this.ID = ID;
        this.sensorList = sensorList;
    }

    public ServerResponse(){
        port=-1;
        ID="ERROR";
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public List<SensorData> getSensorList() {
        return sensorList;
    }

    public void setSensorList(List<SensorData> sensorList) {
        this.sensorList = sensorList;
    }
}
