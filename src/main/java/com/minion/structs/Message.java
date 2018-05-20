package com.minion.structs;

import java.util.HashMap;
import java.util.Map;

/**
 * A data wrapper for messages to be passed around between actors. This wrapper includes in the account
 * key for a request alongside data so that actors can keep track of who they are performing work for.
 * 
 * @param <T> data object that is being passed inside of message
 */
public class Message<T> {
	private final String account_key;
	private final T datum;
	private final Map<String, Object> options;
	
	public Message(String account_key, T data){
		this.account_key = account_key;
		this.datum = data;
		this.options = new HashMap<String, Object>();
	}
	
	public Message(String account_key, T data, Map<String, Object> options){
		this.account_key = account_key;
		this.datum = data;
		this.options = options;
	}

	public String getAccountKey(){
		return this.account_key;
	}
	
	public T getData(){
		return this.datum;
	}

	public Map<String, Object> getOptions() {
		return options;
	}
	
	public Message<T> clone(){
		Message<T> msg = new Message<T>(this.getAccountKey(), this.getData(), this.getOptions());
		return msg;
		
	}
}
