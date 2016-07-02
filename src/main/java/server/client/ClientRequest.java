package server.client;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by antonio on 02/07/16.
 */
@XmlRootElement
public class ClientRequest {
    public String id;
    public String type;
    public long t1;
    public long t2;

    public ClientRequest(String id, String type, long t1, long t2) {
        this.id = id;
        this.type = type;
        this.t1 = t1;
        this.t2 = t2;
    }

    public ClientRequest() {

    }
}
