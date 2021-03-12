package com.qanairy.models.audit;

import java.util.List;
import java.util.stream.Collectors;

import org.neo4j.ogm.annotation.Relationship;

import com.qanairy.models.Element;
import com.qanairy.models.ElementState;
import com.qanairy.models.enums.ObservationType;
import com.qanairy.models.enums.Priority;

/**
 * A observation of potential error for a given {@link Element element} 
 */
public class RenderedElementObservation extends Observation {
	
	@Relationship(type = "FOR")
	private List<ElementState> elements;
	
	public RenderedElementObservation() {}
	
	public RenderedElementObservation(
			List<ElementState> elements, 
			String description, 
			String why_it_matters, 
			String ada_compliance, 
			Priority priority) {
		setElements(elements);
		setDescription(description);
		setWhyItMatters(why_it_matters);
		setAdaCompliance(ada_compliance);
		setPriority(priority);
		setType(ObservationType.ELEMENT);
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
	public ObservationType getType() {
		return ObservationType.ELEMENT;
	}
}
