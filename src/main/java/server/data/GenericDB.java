package server.data;

/**
 * Created by antonio on 12/05/16.
 */
public interface GenericDB {

    public <T extends Object>boolean create(T obj);
}
