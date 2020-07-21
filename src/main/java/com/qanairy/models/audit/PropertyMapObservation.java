package com.qanairy.models.audit;

import java.util.List;
import java.util.Map;

import org.neo4j.ogm.annotation.Properties;

import com.qanairy.models.ElementState;

/**
 * A observation of potential error for a given {@link ElementState element} 
 */
public class PropertyMapObservation extends Observation{

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
}
