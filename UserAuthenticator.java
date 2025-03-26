package com.viikko4;

import java.util.Hashtable;
import java.util.Map;

import com.sun.net.httpserver.BasicAuthenticator;

public class UserAuthenticator extends BasicAuthenticator {

    private Map<String,User> users = new Hashtable <>();

    public UserAuthenticator() {
        super("datarecord"); 
        
        users.put("username", new User("username", "password", "user.email@for-contacting.com")); 
    }

    @Override
    public boolean checkCredentials(String username, String password) {

        User user = users.get(username);
        
        return user != null && user.getPassword().equals(password);
    }
    


    public synchronized boolean addUser(String username, String password, String email) {
        if (users.containsKey(username)) {
            return false;
        }
        users.put(username, new User(username, password, email));
        return true;
    }
    
}
