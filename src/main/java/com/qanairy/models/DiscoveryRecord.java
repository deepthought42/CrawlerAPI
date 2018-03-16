package com.qanairy.models;

import java.util.Date;

/**
 * Defines the type of package paid for, which domains are registered and which Users belong to the account
 */
public class DiscoveryRecord {
	private String key;
	private Date date;
	
	public DiscoveryRecord(){}
	
	
	public DiscoveryRecord(Date date){
		assert date != null;
		
		this.setDate(date);
	}
	
	public DiscoveryRecord(String key, Date date){
		assert key != null;
		assert date != null;
		
		this.setKey(key);
		this.setDate(date);
	}
			

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}


	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}
}
