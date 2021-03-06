package server.simulator;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by civi on 22/04/16.
 */

@XmlRootElement
public class Measurement implements Comparable<Measurement> {

    private String id;
    private String type;
    private String value;
    private long timestamp;

    public Measurement(String id, String type, String value, long timestamp) {
        this.value = value;
        this.timestamp = timestamp;
        this.id = id;
        this.type = type;
    }

    public Measurement() {

    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String type) {
        this.id = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public int compareTo(Measurement m) {
        Long thisTimestamp = timestamp;
        Long otherTimestamp = m.getTimestamp();
        return thisTimestamp.compareTo(otherTimestamp);
    }

    @Override
    public String toString() {
        return "Measurement{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", value='" + value + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
