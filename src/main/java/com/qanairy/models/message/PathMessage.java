package com.qanairy.models.message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.qanairy.models.Account;
import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.PathObject;

import akka.actor.ActorRef;

public class PathMessage {

	private String account_key;
	private Map<String, Object> options;
	private List<String> keys;
	private List<PathObject> path_objects;
	private DiscoveryRecord discovery;
	private ActorRef discovery_actor;
	private Account account;
	
	public PathMessage(List<String> keys, List<PathObject> path_objects, DiscoveryRecord discovery, Map<String, Object> options){
		this.discovery = discovery;
		this.keys = keys;
		this.path_objects = path_objects;
		this.setOptions(options);
	}
	
	public PathMessage(List<String> keys, List<PathObject> path_objects, ActorRef discovery_actor){
		this.keys = keys;
		this.path_objects = path_objects;
		this.setAccountKey(account_key);
		this.setDiscoveryActor(discovery_actor);
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

	private void setAccountKey(String account_key) {
		this.account_key = account_key;
	}

	public Map<String, Object> getOptions() {
		return options;
	}

	private void setOptions(Map<String, Object> options) {
		this.options = options;
	}
	
	public PathMessage clone(){
		return new PathMessage(new ArrayList<>(keys), new ArrayList<>(path_objects), discovery, options);
	}

	public ActorRef getDiscoveryActor() {
		return discovery_actor;
	}

	private void setDiscoveryActor(ActorRef discovery_actor) {
		this.discovery_actor = discovery_actor;
	}

	public Account getAccount() {
		return account;
	}

	private void setAccount(Account account) {
		this.account = account;
	}
}
