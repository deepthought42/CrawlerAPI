package com.qanairy.models.audit;

import java.util.List;
import java.util.Map;

import org.neo4j.ogm.annotation.Properties;

import com.qanairy.models.Element;
import com.qanairy.models.enums.ObservationType;
import com.qanairy.models.enums.Priority;

/**
 * A observation of potential error for a given {@link Element element} 
 */
public class PropertyMapObservation extends Observation{

	@Properties
	private Map<String, List<Double>> properties;
	
	public PropertyMapObservation() {}
	
	public PropertyMapObservation(
			Map<String, List<Double>> properties, 
			String description, 
			String why_it_matters, 
			String ada_compliance, 
			Priority priority) {
		setProperties(properties);
		setDescription(description);
		setWhyItMatters(why_it_matters);
		setAdaCompliance(ada_compliance);
		setPriority(priority);
		setType(ObservationType.PROPERTY_MAP);
		setKey(this.generateKey());
	}
	
	/*
	@Override
	public String generateKey() {

		return "observation::"+org.apache.commons.codec.digest.DigestUtils.sha256Hex( getProperties() + this.getDescription() );
	}
*/

	public Map<String, List<Double>> getProperties() {
		return properties;
	}


	public void setProperties(Map<String, List<Double>> properties) {
		this.properties = properties;
	}

	@Override
	public ObservationType getType() {
		return ObservationType.PROPERTY_MAP;
	}
}
