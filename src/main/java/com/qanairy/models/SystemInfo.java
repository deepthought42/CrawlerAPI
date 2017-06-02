package com.qanairy.models;

import com.qanairy.persistence.ISystemInfo;

/**
 * 
 * 
 */
public class SystemInfo implements ISystemInfo{
	private int browser_count;
	private int actor_count;
	private String key;
	
	public SystemInfo(){};
	
	public SystemInfo(int count){
		this.browser_count = count;
	}
	
	public int getBrowserCount(){
		return this.browser_count;
	}
	
	public void setBrowserCount(int count){
		this.browser_count = count;
	}

	public int getActorCount() {
		return actor_count;
	}

	public void setActorCount(int actor_count) {
		this.actor_count = actor_count;
	}

	public int incrementBrowserCount() {
		return this.browser_count++;
	}

	public int decrementBrowserCount() {
		return this.browser_count--;
	}

	public void setKey(String key) {
		this.key = key;
	}
	
	public String getKey(){
		return this.key;
	}
}
