package com.qanairy.models.message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.PathObject;

public class PathMessage {

	private String account_key;
	private Map<String, Object> options;
	private List<String> keys;
	private List<PathObject> path_objects;
	private DiscoveryRecord discovery;
	
	public PathMessage(List<String> keys, List<PathObject> path_objects, DiscoveryRecord discovery, String account_key, Map<String, Object> options){
		this.discovery = discovery;
		this.keys = keys;
		this.path_objects = path_objects;
		this.setAccountKey(account_key);
		this.setOptions(options);
	}

	public List<String> getKeys() {
		return keys;
	}

	public List<PathObject> getPathObjects() {
		return path_objects;
	}

	public DiscoveryRecord getDiscovery() {
		return discovery;
	}

	public String getAccountKey() {
		return account_key;
	}

	public void setAccountKey(String account_key) {
		this.account_key = account_key;
	}

	public Map<String, Object> getOptions() {
		return options;
	}

	public void setOptions(Map<String, Object> options) {
		this.options = options;
	}
	
	public PathMessage clone(){
		return new PathMessage(new ArrayList<>(keys), new ArrayList<>(path_objects), discovery, account_key, options);
	}
}
