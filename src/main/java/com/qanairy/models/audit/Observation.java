package com.qanairy.models.audit;

import java.util.Random;
import java.util.Set;

import com.qanairy.models.Element;
import com.qanairy.models.LookseeObject;
import com.qanairy.models.enums.ObservationType;
import com.qanairy.models.enums.Priority;

/**
 * A observation of potential error for a given {@link Element element} 
 */
public abstract class Observation extends LookseeObject{
	private String description;
	private String type;
	private String explanation; //Further explanation apart from the description. Reason it matters, etc
	private Set<String> recommendations;
	private String why_it_matters;
	private String ada_compliance;
	private String priority;
	
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
	
	public abstract ObservationType getType();

	public Set<String> getRecommendations() {
		return recommendations;
	}

	public void setRecommendations(Set<String> recommendations) {
		this.recommendations = recommendations;
	}
	
	public void addRecommendation(String recommendation) {
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
}
