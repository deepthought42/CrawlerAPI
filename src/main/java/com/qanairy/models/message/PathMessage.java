package com.qanairy.models.message;

import java.util.ArrayList;
import java.util.List;

import com.qanairy.models.PathObject;
import com.qanairy.models.enums.PathStatus;

import akka.actor.ActorRef;

public class PathMessage {

	private List<String> keys;
	private List<PathObject> path_objects;
	private ActorRef discovery_actor;	
	private PathStatus status;
	
	public PathMessage(List<String> keys, List<PathObject> path_objects, ActorRef discovery_actor, PathStatus status){
		setKeys(keys);
		setPathObjects(path_objects);
		setDiscoveryActor(discovery_actor);
		setStatus(status);
	}

	public List<String> getKeys() {
		return keys;
	}
	
	private void setKeys(List<String> keys) {
		this.keys = keys;
	}

	public List<PathObject> getPathObjects() {
		return path_objects;
	}
	
	private void setPathObjects(List<PathObject> path_objects) {
		this.path_objects = path_objects;
	}
	
	public PathMessage clone(){
		return new PathMessage(new ArrayList<>(keys), new ArrayList<>(path_objects), getDiscoveryActor(), getStatus());
	}

	public ActorRef getDiscoveryActor() {
		return discovery_actor;
	}

	private void setDiscoveryActor(ActorRef discovery_actor) {
		this.discovery_actor = discovery_actor;
	}

	public PathStatus getStatus() {
		return status;
	}

	private void setStatus(PathStatus status) {
		this.status = status;
	}
}
