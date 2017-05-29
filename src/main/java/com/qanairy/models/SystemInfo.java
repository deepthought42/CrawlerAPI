package com.qanairy.models;

import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.util.XMLFragment.Child;

/**
 * 
 * 
 */
public class SystemInfo {
	private int browser_count;
	private int actor_count;
	private String key;
	private List<ChildModelTempDemo> groups = null;
	public SystemInfo(){};
	
	public SystemInfo(int count){
		this.browser_count = count;
		groups = new ArrayList<ChildModelTempDemo>();
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
