package com.qanairy.models.audit;

import java.util.List;
import java.util.Map;

import org.neo4j.ogm.annotation.Properties;

import com.qanairy.models.ElementState;
import com.qanairy.models.LookseeObject;
import com.qanairy.models.enums.ObservationType;

/**
 * A observation of potential error for a given {@link ElementState element} 
 */
public class PropertyMapObservation extends LookseeObject implements Observation{
	private String description;
	
	@Properties
	private Map<String, List<Double>> properties;
	
	public PropertyMapObservation() {}
	
	public PropertyMapObservation(Map<String, List<Double>> properties, String description) {
		setProperties(properties);
		setDescription(description);
		setKey(this.generateKey());
	}
	
	@Override
	public String generateKey() {

		return "observation::"+org.apache.commons.codec.digest.DigestUtils.sha256Hex( getProperties() + this.getDescription() );
	}


	public Map<String, List<Double>> getProperties() {
		return properties;
	}


	public void setProperties(Map<String, List<Double>> properties) {
		this.properties = properties;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public ObservationType getType() {
		return ObservationType.PROPERTY_MAP;
	}
}
