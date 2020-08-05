package com.qanairy.models.audit;

import com.qanairy.models.ElementState;
import com.qanairy.models.enums.ObservationType;

/**
 * A observation of potential error for a given {@link ElementState element} 
 */
public interface Observation {
	public String getDescription();
	public void setDescription(String description);
	
	public ObservationType getType();
}
