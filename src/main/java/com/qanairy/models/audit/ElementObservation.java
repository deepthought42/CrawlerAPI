package com.qanairy.models.audit;

import java.util.List;
import java.util.Set;

import org.neo4j.ogm.annotation.Relationship;

import com.qanairy.models.Element;
import com.qanairy.models.enums.ObservationType;
import com.qanairy.models.enums.Priority;

/**
 * A observation of potential error for a given {@link Element element} 
 */
public class ElementObservation extends Observation {	
	@Relationship(type = "FOR")
	private List<Element> elements;
	
	public ElementObservation() {}
	
	public ElementObservation(
			List<Element> elements, 
			String description, 
			String why_it_matters, 
			String ada_compliance, 
			Priority priority,
			Set<String> recommendations, 
			Set<String> labels, 
			Set<String> categories
	) {
		setElements(elements);
		setDescription(description);
		setWhyItMatters(why_it_matters);
		setAdaCompliance(ada_compliance);
		setPriority(priority);
		setLabels(labels);
		setCategories(categories);
		setRecommendations(recommendations);
		setKey(this.generateKey());
	}
	
	/*
	@Override
	public String generateKey() {
		assert elements != null;
		String key = elements.parallelStream().map(Element::getKey).sorted().collect(Collectors.joining(""));
		
		return "observation"+org.apache.commons.codec.digest.DigestUtils.sha256Hex( key + this.getDescription() );
	}
*/

	public List<Element> getElements() {
		return elements;
	}


	public void setElements(List<Element> elements) {
		this.elements = elements;
	}
	
	public boolean addElements(List<Element> elements) {
		return this.elements.addAll(elements);
	}

	@Override
	public ObservationType getType() {
		return ObservationType.ELEMENT;
	}
}
