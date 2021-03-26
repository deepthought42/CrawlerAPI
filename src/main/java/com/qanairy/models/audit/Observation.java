package com.qanairy.models.audit;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.neo4j.ogm.annotation.NodeEntity;

import com.qanairy.models.Element;
import com.qanairy.models.LookseeObject;
import com.qanairy.models.enums.ObservationType;
import com.qanairy.models.enums.Priority;

/**
 * A observation of potential error for a given {@link Element element} 
 */
@NodeEntity
public class Observation extends LookseeObject{
	private String description;
	private String type;
	private Set<String> recommendations = new HashSet<>();
	private String why_it_matters;
	private String ada_compliance;
	private String priority;
	// labels are intended to contain things like subcategory, accessibility, etc
	private Set<String> labels;
	
	public Observation() {}
	
	public Observation(
			String description, 
			String why_it_matters, 
			String ada_compliance, 
			Priority priority, 
			Set<String> recommendations,
			ObservationType type, 
			Set<String> labels
	) {
		setDescription(description);
		setWhyItMatters(why_it_matters);
		setAdaCompliance(ada_compliance);
		setPriority(priority);
		setRecommendations(recommendations);
		setType(type);
		setLabels(labels);
		setKey(generateKey());
	}
	
	public Observation(
			String description, 
			String why_it_matters, 
			String ada_compliance, 
			Priority priority,
			String key, 
			Set<String> recommendations,
			ObservationType type, 
			Set<String> labels
	) {
		setDescription(description);
		setWhyItMatters(why_it_matters);
		setAdaCompliance(ada_compliance);
		setPriority(priority);
		setRecommendations(recommendations);
		setType(type);
		setLabels(labels);
		setKey(key);
	}
	
	@Override
	public String generateKey() {
		return "observation"+getSaltString();
	}
	
	protected String getSaltString() {
        String SALTCHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < 32) { // length of the random string.
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        String saltStr = salt.toString();
        return saltStr;

    }
	
	public String getDescription() {
		return this.description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public void setType(ObservationType type) {
		this.type = type.getShortName();
	}
	
	public ObservationType getType() {
		return ObservationType.create(this.type);
	}

	public Set<String> getRecommendations() {
		return recommendations;
	}

	public void setRecommendations(Set<String> recommendations) {
		this.recommendations = recommendations;
	}
	
	public void addRecommendation(String recommendation) {
		assert recommendation != null;
		assert !recommendation.isEmpty();
		
		this.recommendations.add(recommendation);
	}
	
	public boolean removeRecommendation(String recommendation) {
		return this.getRecommendations().remove(recommendation);		
	}
	
	public String getWhyItMatters() {
		return why_it_matters;
	}

	public void setWhyItMatters(String why_it_matters) {
		this.why_it_matters = why_it_matters;
	}

	public String getAdaCompliance() {
		return ada_compliance;
	}

	public void setAdaCompliance(String ada_compliance) {
		this.ada_compliance = ada_compliance;
	}

	public Priority getPriority() {
		return Priority.create(this.priority);
	}
	
	public void setPriority(Priority priority) {
		this.priority = priority.getShortName();
	}

	public Set<String> getLabels() {
		return labels;
	}

	public void setLabels(Set<String> labels) {
		this.labels = labels;
	}
}
