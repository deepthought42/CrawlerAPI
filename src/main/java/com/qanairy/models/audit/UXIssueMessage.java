package com.qanairy.models.audit;

import java.util.UUID;

import org.neo4j.ogm.annotation.NodeEntity;

import com.qanairy.models.LookseeObject;
import com.qanairy.models.enums.Priority;

@NodeEntity
public class UXIssueMessage extends LookseeObject {
	private String recommendation;
	private String priority;
	

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

	@Override
	public String generateKey() {
		return "issuemessage"+UUID.randomUUID();
	}
}
