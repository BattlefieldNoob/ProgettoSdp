package sensor.data;

import server.sensor.SensorData;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by antonio on 18/02/17.
 */
public class TokenLostEvent extends Event {

    private List<SensorLifeTime> sensors = new LinkedList<>();

    public TokenLostEvent(SensorData sender){
        super("TOKENLOST",sender,null);
    }

    public void add(String id,long lifetime){
        sensors.add(new SensorLifeTime(id,lifetime));
    }

    public String getOlderSensorId(){
        int index=0;
        long higher=Long.MIN_VALUE;
        for(int i=0;i<sensors.size();i++){
            SensorLifeTime sensor=sensors.get(i);
            if(sensor.lifeTime>higher){
                higher=sensor.lifeTime;
                index=i;
            }
        }
        return sensors.get(index).id;
    }

}
