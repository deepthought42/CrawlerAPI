package com.qanairy.models.message;

import java.util.List;

import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.PathObject;

public class PathMessage {

	private List<String> keys;
	private List<PathObject> path_objects;
	private DiscoveryRecord discovery;
	
	public PathMessage(List<String> keys, List<PathObject> path_objects, DiscoveryRecord discovery){
		this.discovery = discovery;
		this.keys = keys;
		this.path_objects = path_objects;
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
	
	
}
