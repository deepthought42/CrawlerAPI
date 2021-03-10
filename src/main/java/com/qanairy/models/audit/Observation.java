package com.qanairy.models.audit;

import java.util.Set;

import com.qanairy.models.Element;
import com.qanairy.models.LookseeObject;
import com.qanairy.models.enums.ObservationType;

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
	
	@Override
	public String generateKey() {
		return "observation::"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(getType().getShortName()+getDescription());
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
}
