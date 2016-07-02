package server.client;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by antonio on 02/07/16.
 */
@XmlRootElement
public class MeasurementsByFilter {

    public String id;
    public long t1;
    public long t2;
    public double min, mid, max;

    public MeasurementsByFilter(String id, long t1, long t2, double min, double mid, double max) {
        this.id = id;
        this.t1 = t1;
        this.t2 = t2;
        this.min = min;
        this.mid = mid;
        this.max = max;
    }

    public MeasurementsByFilter() {

    }
}
