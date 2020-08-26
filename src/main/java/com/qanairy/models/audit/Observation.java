package com.qanairy.models.audit;

import com.qanairy.models.Element;
import com.qanairy.models.LookseeObject;
import com.qanairy.models.enums.ObservationType;

/**
 * A observation of potential error for a given {@link Element element} 
 */
public abstract class Observation extends LookseeObject{
	private String description;
	private String type;
	
	public String getDescription() {
		return this.description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public void setType(ObservationType type) {
		this.type = type.getShortName();
	}
	
	public abstract ObservationType getType();
	

	@Override
	public String generateKey() {
		// TODO Auto-generated method stub
		return "observation::"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(getType().getShortName()+getDescription());
	}
}
