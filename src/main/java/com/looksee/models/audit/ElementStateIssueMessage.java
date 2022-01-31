package com.looksee.models.audit;


import java.util.HashSet;
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
	
	@Relationship(type = "EXAMPLE")
	private ElementState good_example;

	public ElementStateIssueMessage() {}
	
	public ElementStateIssueMessage(
			Priority priority,
			String description,
			String recommendation, 
			ElementState element, 
			AuditCategory category, 
			Set<String> labels, 
			String wcag_compliance,
			String title, 
			int points_awarded,
			int max_points
	) {
		super(	priority, 
				description, 
				ObservationType.ELEMENT,
				category,
				wcag_compliance,
				labels,
				"",
				title,
				points_awarded,
				max_points,
				new HashSet<>(),
				recommendation);
		
		setElement(element);
	}

	public ElementState getElement() {
		return element;
	}

	public void setElement(ElementState element) {
		this.element = element;
	}
	
	public ElementState getGoodExample() {
		return good_example;
	}

	public void setGoodExample(ElementState good_example) {
		this.good_example = good_example;
	}
}
