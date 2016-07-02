package server.data;


import server.client.UserData;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by antonio on 12/05/16.
 */
public class UsersDB {


    private static UsersDB instance;
    private final List<UserData> usersList = new LinkedList<>();

    public static UsersDB getInstance() {
        if (instance == null) {
            instance = new UsersDB();
        }
        return instance;
    }

    public boolean create(UserData newuser) {
        synchronized (usersList) {
            for (UserData username : usersList) {
                if (username.username.equals(newuser.username)) {
                    return false;
                }
            }
            usersList.add(newuser);
            return true;
        }
    }

    public boolean check(String username) {
        synchronized (usersList) {
            for (UserData user : usersList) {
                if (user.username.equals(username)) {
                    return true;
                }
            }
            return false;
        }
    }

    public boolean delete(String username) {
        synchronized (usersList) {
            for (UserData user : usersList) {
                if (user.username.equals(username)) {
                    usersList.remove(user);
                    return true;
                }
            }
        }
        return false;
    }

    public List<UserData> readAll() {
        return usersList;
    }

}
