package com.qanairy.models.audit;

import java.util.List;
import java.util.stream.Collectors;

import org.neo4j.ogm.annotation.Relationship;

import com.qanairy.models.ElementState;

/**
 * A observation of potential error for a given {@link ElementState element} 
 */
public class ElementObservation extends Observation{

	@Relationship(type = "FOR")
	private List<ElementState> elements;
	
	public ElementObservation() {}
	
	public ElementObservation(List<ElementState> element, String description) {
		setElement(element);
		setDescription(description);
		setKey(this.generateKey());
	}
	
	@Override
	public String generateKey() {
		assert elements != null;
		String key = elements.parallelStream().map(ElementState::getKey).sorted().collect(Collectors.joining(""));
		
		return "observation::"+org.apache.commons.codec.digest.DigestUtils.sha256Hex( key + this.getDescription() );
	}


	public List<ElementState> getElement() {
		return elements;
	}


	public void setElement(List<ElementState> element) {
		this.elements = element;
	}
}
