package com.qanairy.models.audit;

import java.util.Set;

import com.qanairy.models.Element;
import com.qanairy.models.enums.ObservationType;
import com.qanairy.models.enums.Priority;

/**
 * A observation of potential error for a given {@link Element element} 
 */
public class SimpleObservation extends Observation{
	public SimpleObservation() {}
	
	public SimpleObservation(
			String description, 
			String why_it_matters, 
			String ada_compliance, 
			Priority priority, 
			String key, 
			Set<String> recommendations
	) {
		setDescription(description);
		setWhyItMatters(why_it_matters);
		setAdaCompliance(ada_compliance);
		setPriority(priority);
		setRecommendations(recommendations);
		setKey(key);
	}
	
	/*
	@Override
	public String generateKey() {
		assert elements != null;
		String key = elements.parallelStream().map(ElementState::getKey).sorted().collect(Collectors.joining(""));
		
		return "observation"+org.apache.commons.codec.digest.DigestUtils.sha256Hex( key + this.getDescription() );
	}
*/

	@Override
	public ObservationType getType() {
		return ObservationType.ELEMENT;
	}
}
