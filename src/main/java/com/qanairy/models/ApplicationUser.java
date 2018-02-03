package com.qanairy.models;

/**
 * 
 * 
 */
public class ApplicationUser {

    private String key;
    private String username;
    private String password;

    public ApplicationUser(){}
    
    public ApplicationUser(String username, String pass){
    	setUsername(username);
    	setPassword(pass);
    }
    
    public ApplicationUser(String key, String username, String pass){
    	setKey(key);
    	setUsername(username);
    	setPassword(pass);
    }
    
    public String getKey() {
        return key;
    }
    
    public void setKey(String key) {
    	this.key = key;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
