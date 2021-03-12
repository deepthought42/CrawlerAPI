package com.qanairy.models.audit;

import java.util.List;
import java.util.Random;
import java.util.Set;

import com.qanairy.models.Element;
import com.qanairy.models.ElementState;
import com.qanairy.models.LookseeObject;
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
			Priority priority
	) {
		setDescription(description);
		setWhyItMatters(why_it_matters);
		setAdaCompliance(ada_compliance);
		setPriority(priority);
		setKey(this.generateKey());
	}
	
	/*
	@Override
	public String generateKey() {
		assert elements != null;
		String key = elements.parallelStream().map(ElementState::getKey).sorted().collect(Collectors.joining(""));
		
		return "observation::"+org.apache.commons.codec.digest.DigestUtils.sha256Hex( key + this.getDescription() );
	}
*/

	@Override
	public ObservationType getType() {
		return ObservationType.ELEMENT;
	}
}
