package client;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by antonio on 12/05/16.
 */
@XmlRootElement
public class UserData {

    public String username;

    public String address;

    public int port;

    UserData(){

    }

    public UserData(String username, String address, int port) {
        this.username = username;
        this.address = address;
        this.port = port;
    }


}
