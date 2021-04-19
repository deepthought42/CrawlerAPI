package com.qanairy.models.audit;

import java.util.Set;
import java.util.UUID;

import org.neo4j.ogm.annotation.NodeEntity;

import com.qanairy.models.LookseeObject;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.ObservationType;
import com.qanairy.models.enums.Priority;

@NodeEntity
public class UXIssueMessage extends LookseeObject {
	private String recommendation;
	private String priority;
	private String description;
	private String type;
	private String category;
	private Set<String> labels;
	
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
}
