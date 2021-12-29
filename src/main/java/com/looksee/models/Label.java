package com.looksee.models;

import java.util.UUID;

public class Label extends LookseeObject{
	private String description;
	private float score;
	private float topicality;
	
	public Label() {
		setDescription("");
		setScore(0.0F);
		setTopicality(0.0F);
	}
	
	public Label(String description, float score, float topicality) {
		setDescription(description);
		setScore(score);
		setTopicality(topicality);
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public float getScore() {
		return score;
	}

	public void setScore(float score) {
		this.score = score;
	}

	public float getTopicality() {
		return topicality;
	}

	public void setTopicality(float topicality) {
		this.topicality = topicality;
	}

	@Override
	public String generateKey() {
		return "label::"+UUID.randomUUID();
	}
}
