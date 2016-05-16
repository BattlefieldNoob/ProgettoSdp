package sensor.data;

import server.data.SensorData;

/**
 * Created by antonio on 13/05/16.
 */
public class Ussaro {

    public String event;
    public SensorData target;
    public SensorData targetNext;

    public Ussaro(String event,SensorData target,SensorData targetNext){
        this.event=event;
        this.target=target;
        this.targetNext=targetNext;
    }
}
