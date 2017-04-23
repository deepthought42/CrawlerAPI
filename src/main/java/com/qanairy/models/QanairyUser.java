package com.qanairy.models;

/**
 * Defines the type of package paid for, which domains are registered and which Users belong to the account
 */
public class QanairyUser {
	private String key;
	private String email;
	
	public QanairyUser(){}
	
	public QanairyUser(String email){
		this.setEmail(email);
	}
	
	public QanairyUser(String key, String email){
		this.setKey(key);
		this.setEmail(email);
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}
	
	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
}
