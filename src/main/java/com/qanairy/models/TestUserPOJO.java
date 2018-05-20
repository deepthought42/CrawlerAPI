package com.qanairy.models;

import com.qanairy.persistence.TestUser;

/**
 * Defines user information that can be used during testing
 */
public class TestUserPOJO extends TestUser {
	private String key;
	private String username;
	private String password;
	private String role;
	
	public TestUserPOJO(){}
	
	public TestUserPOJO(String username, String password, String role){
		setUsername(username);
		setPassword(password);
		setKey(generateKey());
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

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	@Override
	public void setUsername(String username) {
		this.username = username;
	}

	@Override
	public void setPassword(String password) {
		this.password = password;
	}
	

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateKey() {
		return getUsername()+"::"+getPassword();
	}
}
