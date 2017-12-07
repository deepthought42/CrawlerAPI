package com.qanairy.models;

/**
 * Defines user information that can be used during testing
 */
public class TestUser {
	private String key;
	private String username;
	private String password;
	
	public TestUser(String username, String password){
		this.setKey(null);
		this.username = username;
		this.password = password;
	}
	
	public TestUser(String key, String username, String password){
		this.setKey(key);
		this.username = username;
		this.password = password;
	}
	
	public String getUsername(){
		return this.username;
	}
	
	public String getPassword(){
		return this.password;
	}

	public String getKey(){
		return this.key;
	}
	
	public void setKey(String key) {
		this.key = key;
	}
}
