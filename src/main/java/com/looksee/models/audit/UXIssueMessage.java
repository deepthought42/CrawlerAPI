package com.looksee.models.audit;

import java.util.Set;
import java.util.UUID;

import org.neo4j.ogm.annotation.NodeEntity;

import com.looksee.models.LookseeObject;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.ObservationType;
import com.looksee.models.enums.Priority;

@NodeEntity
public class UXIssueMessage extends LookseeObject {
	private String title;
	private String description;
	private String why_it_matters;
	private String recommendation;
	private String priority;
	private String type;
	private String category;
	private String wcag_compliance;
	private Set<String> labels;
	
	public UXIssueMessage() {}
	
	public UXIssueMessage(
			String recommendation,
			Priority priority, 
			String description,
			ObservationType type,
			AuditCategory category,
			String wcag_compliance,
			Set<String> labels, 
			String why_it_matters, 
			String title
	) {
		setRecommendation(recommendation);
		setPriority(priority);
		setDescription(description);
		setType(type);
		setCategory(category);
		setWcagCompliance(wcag_compliance);
		setLabels(labels);
		setWhyItMatters(why_it_matters);
		setTitle(title);
		setKey(generateKey());
	}
	
	public Priority getPriority() {
		return Priority.create(this.priority);
	}
	
	public void setPriority(Priority priority) {
		this.priority = priority.getShortName();
	}

	public String getRecommendation() {
		return recommendation;
	}

	public void setRecommendation(String recommendation) {
		this.recommendation = recommendation;
	}
	
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
	public ObservationType getType() {
		return ObservationType.create(type);
	}

	public void setType(ObservationType type) {
		this.type = type.getShortName();
	}

	public AuditCategory getCategory() {
		return AuditCategory.create(category);
	}

	public void setCategory(AuditCategory category) {
		this.category = category.getShortName();
	}

	public Set<String> getLabels() {
		return labels;
	}

	public void setLabels(Set<String> labels) {
		this.labels = labels;
	}

	@Override
	public String generateKey() {
		return "issuemessage"+UUID.randomUUID();
	}

	public String getWcagCompliance() {
		return wcag_compliance;
	}

	public void setWcagCompliance(String wcag_compliance) {
		this.wcag_compliance = wcag_compliance;
	}

	public String getWhyItMatters() {
		return why_it_matters;
	}

	public void setWhyItMatters(String why_it_matters) {
		this.why_it_matters = why_it_matters;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
}
