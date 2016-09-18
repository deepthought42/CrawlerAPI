package com.minion.api.models;

import org.springframework.data.annotation.Id;

public class Account {
	@Id
    private String id;
    private String username;
    
    public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
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

	private String password;

    public Account(String username, String password) {
        this.username = username;
        this.password = password;
    }   
}
