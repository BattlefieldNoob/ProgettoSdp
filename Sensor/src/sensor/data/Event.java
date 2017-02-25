package sensor.data;


import server.sensor.SensorData;

/**
 * Created by antonio on 13/05/16.
 */
public class Event {

    public String event;
    public SensorData target;
    public SensorData targetNext;
    public Event(String event, SensorData target, SensorData targetNext) {
        this.event = event;
        this.target = target;
        this.targetNext = targetNext;
    }

    public boolean isSentBy(SensorData sensor){
        return target.getId().equals(sensor.getId());
    }

    public boolean isInsertEvent(){
        return event.equals("Insert");
    }

    public boolean isDeleteEvent(){
        return event.equals("Delete");
    }

    public boolean isMyPrevious(SensorData me){
        return targetNext.getId().equals(me.getId());
    }

}
