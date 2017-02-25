package server.data;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by antonio on 19/02/17.
 */
@XmlRootElement
public class ServerData {

    String message;


    public ServerData() {
    }

    public ServerData(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
