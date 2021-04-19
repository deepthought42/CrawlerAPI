package com.qanairy.models.audit;


import java.util.Set;

import org.neo4j.ogm.annotation.Relationship;

import com.qanairy.models.Element;
import com.qanairy.models.ElementState;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.ObservationType;
import com.qanairy.models.enums.Priority;

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
			Set<String> labels
	) {
		setElement(element);
		setPriority(priority);
		setRecommendation(recommendation);
		setDescription(description);
		setType(ObservationType.ELEMENT);
		setCategory(category);
		setLabels(labels);
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
