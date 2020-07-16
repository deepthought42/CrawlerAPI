package com.qanairy.models.audit;

import com.qanairy.models.ElementState;
import com.qanairy.models.LookseeObject;

/**
 * A observation of potential error for a given {@link ElementState element} 
 */
public abstract class Observation<T> extends LookseeObject{
	private String description;
	
	public Observation() {}
	
	public Observation(String description) {
		setDescription(description);
		setKey(this.generateKey());
	}
	
	public abstract T get();
	
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
