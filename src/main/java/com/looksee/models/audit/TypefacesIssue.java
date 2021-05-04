package com.looksee.models.audit;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.looksee.models.Element;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.Priority;

/**
 * A observation of potential error for a given {@link Element element} 
 */
public class TypefacesIssue extends UXIssueMessage {	
	private List<String> typefaces = new ArrayList<>();
	
	public TypefacesIssue() {}
	
	public TypefacesIssue(
			List<String> typefaces, 
			String description, 
			String recommendation, 
			Priority priority, 
			AuditCategory category, 
			Set<String> labels, 
			String wcag_compliance) {
		assert typefaces != null;
		assert !typefaces.isEmpty();
		assert description != null;
		assert !description.isEmpty();
		assert recommendation != null;
		assert !recommendation.isEmpty();
		assert priority != null;
		assert category != null;
		assert labels != null;
		
		setTypefaces(typefaces);
		setDescription(description);
		setRecommendation(recommendation);
		setPriority(priority);
		setCategory(category);
		setLabels(labels);
		setWcagCompliance(wcag_compliance);
		setKey(this.generateKey());
	}

	public List<String> getTypefaces() {
		return typefaces;
	}


	public void setTypefaces(List<String> typefaces) {
		this.typefaces = typefaces;
	}
	
	public boolean addTypefaces(List<String> typefaces) {
		return this.typefaces.addAll(typefaces);
	}
}
