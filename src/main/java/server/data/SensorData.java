package server.data;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by antonio on 06/05/16.
 */
@XmlRootElement
public class SensorData {

    String id;

    String type;

    String address;

    int port;

    public SensorData(String id, String type, String address, int port) {
        this.id = id;
        this.type = type;
        this.address = address;
        this.port = port;
    }

    public SensorData() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "SensorData{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", address='" + address + '\'' +
                ", port=" + port +
                '}';
    }

    public String toClientInterface() {
        return "Sensor " + id + " (type = " + type + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SensorData that = (SensorData) o;

        if (port != that.port) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;
        return address != null ? address.equals(that.address) : that.address == null;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (address != null ? address.hashCode() : 0);
        result = 31 * result + port;
        return result;
    }
}
