package com.looksee.models.audit;


import java.util.Set;

import org.neo4j.ogm.annotation.Relationship;

import com.looksee.models.Element;
import com.looksee.models.ElementState;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.ObservationType;
import com.looksee.models.enums.Priority;

/**
 * A observation of potential error for a given {@link Element element} 
 */
public class ElementStateIssueMessage extends UXIssueMessage {	
	@Relationship(type = "FOR")
	private ElementState element;
	
	public ElementStateIssueMessage() {}
	
	public ElementStateIssueMessage(
			Priority priority,
			String description,
			String recommendation, 
			ElementState element, 
			AuditCategory category, 
			Set<String> labels, 
			String wcag_compliance
	) {
		setPriority(priority);
		setDescription(description);
		setRecommendation(recommendation);
		setElement(element);
		setType(ObservationType.ELEMENT);
		setCategory(category);
		setLabels(labels);
		setWcagCompliance(wcag_compliance);
		setKey(this.generateKey());
	}
	
	/*
	@Override
	public String generateKey() {
		assert elements != null;
		String key = elements.parallelStream().map(ElementState::getKey).sorted().collect(Collectors.joining(""));
		
		return "observation"+org.apache.commons.codec.digest.DigestUtils.sha256Hex( key + this.getDescription() );
	}
*/

	public ElementState getElement() {
		return element;
	}


	public void setElement(ElementState element) {
		this.element = element;
	}
}
