package com.qanairy.models.audit;

import java.util.List;
import java.util.Set;

import org.neo4j.ogm.annotation.Relationship;

import com.qanairy.models.Element;
import com.qanairy.models.enums.Priority;

/**
 * A observation of potential error for a given {@link Element element} 
 */
public class ElementIssueMessage extends UXIssueMessage {	
	@Relationship(type = "FOR")
	private Element element;
	
	public ElementIssueMessage() {}
	
	public ElementIssueMessage(
			List<Element> elements, 
			String description, 
			String why_it_matters, 
			String ada_compliance, 
			Priority priority,
			Set<String> recommendations, 
			Set<String> labels, 
			Set<String> categories
	) {
		setKey(this.generateKey());
	}
	
	public ElementIssueMessage(
			Element elements, 
			Priority priority,
			String recommendation
	) {
		setElements(elements);
		setPriority(priority);
		setRecommendation(recommendation);
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

	public Element getElement() {
		return element;
	}


	public void setElements(Element element) {
		this.element = element;
	}
}
