package com.qanairy.models.audit;

import java.util.List;
import java.util.stream.Collectors;

import org.neo4j.ogm.annotation.Relationship;

import com.qanairy.models.Element;
import com.qanairy.models.ElementState;
import com.qanairy.models.LookseeObject;
import com.qanairy.models.enums.ObservationType;

/**
 * A observation of potential error for a given {@link Element element} 
 */
public class ElementStateObservation extends LookseeObject implements Observation {
	private String description;
	
	@Relationship(type = "FOR")
	private List<ElementState> elements;
	
	public ElementStateObservation() {}
	
	public ElementStateObservation(List<ElementState> elements, String description) {
		setElements(elements);
		setDescription(description);
		setKey(this.generateKey());
	}
	
	@Override
	public String generateKey() {
		assert elements != null;
		String key = elements.parallelStream().map(ElementState::getKey).sorted().collect(Collectors.joining(""));
		
		return "observation::"+org.apache.commons.codec.digest.DigestUtils.sha256Hex( key + this.getDescription() );
	}


	public List<ElementState> getElements() {
		return elements;
	}


	public void setElements(List<ElementState> elements) {
		this.elements = elements;
	}
	
	public boolean addElements(List<ElementState> elements) {
		return this.elements.addAll(elements);
	}

	@Override
	public String getDescription() {
		return this.description;
	}

	@Override
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public ObservationType getType() {
		return ObservationType.ELEMENT;
	}
}
