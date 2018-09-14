package com.qanairy.models;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

/**
 * Defines user information that can be used during testing
 */
@NodeEntity
public class TestUser implements Persistable {
	
	@GeneratedValue
    @Id
	private Long id;
	
	private String key;
	private String username;
	private String password;
	private String role;
	private boolean enabled;
	
	public TestUser(){}
	
	public TestUser(String username, String password, String role, boolean isEnabled){
		setUsername(username);
		setPassword(password);
		setIsEnabled(isEnabled);
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

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
	public void setIsEnabled(boolean isEnabled){
		this.enabled = isEnabled;
	}
	
	public boolean isEnabled(){
		return this.enabled;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateKey() {
		return getUsername()+"::"+getPassword();
	}
}
