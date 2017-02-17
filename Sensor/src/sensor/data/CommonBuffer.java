package sensor.data;

import sensor.SensorHandle;

import java.util.List;
import java.util.PriorityQueue;

/**
 * Created by antonio on 17/09/16.
 */
public class CommonBuffer {

    private final PriorityQueue<Object> buffer = new PriorityQueue<>(10,(elem1, elem2) -> {
        if (elem1 instanceof Event){
            return -1;
        }else{
            return 1;
        }
    });

    public void push(Object elem) {
        synchronized (buffer) {
            buffer.add(elem);
            synchronized (SensorHandle.outputlock) {
                SensorHandle.outputlock.notify();  // notifico la presenza di nuovi dati
            }
        }
    }

    public Object pop(){
        Object ret=null;
        if(buffer.size()>0) {
            synchronized (buffer) {
                ret = buffer.element();
            }
        }
        return ret;
    }

   /* public int size(){
        return buffer.size();
    }*/

    public boolean isDataAvailable() {
        synchronized (buffer) {
            return buffer.size() != 0;
        }
    }

    public void remove(){
        synchronized (buffer) {
            buffer.remove();
        }
    }
}
