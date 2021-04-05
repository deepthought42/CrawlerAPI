package com.qanairy.models.audit;

import java.time.LocalDateTime;
import java.util.List;

import com.qanairy.models.LookseeObject;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditLevel;
import com.qanairy.models.enums.AuditName;

public class AuditReport extends LookseeObject{
	private String description; //definition
	private String ada_compliance;
	private List<String> best_practices;
	private List<String> recommendations;
	
	/**
	 * Construct empty action object
	 */
	public AuditReport(){}
	
	/**
	 * 
	 * @param category
	 * @param best_practices
	 * @param ada_compliance_description
	 * @param description
	 * @param name
	 */
	public AuditReport(List<String> best_practices, String ada_compliance_description, String description, double score, Audit audit) {
		setBestPractices(best_practices);
		setAdaCompliance(ada_compliance_description);
		setDescription(description);
		setKey(generateKey());
	}
	
	public List<String> getBestPractices() {
		return best_practices;
	}

	public void setBestPractices(List<String> best_practices) {
		this.best_practices = best_practices;
	}

	public List<String> getRecommendations() {
		return recommendations;
	}

	public void setRecommendations(List<String> recommendations) {
		this.recommendations = recommendations;
	}

	public String getAdaCompliance() {
		return ada_compliance;
	}

	public void setAdaCompliance(String ada_compliance) {
		this.ada_compliance = ada_compliance;
	}
	
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String generateKey() {
		// TODO Auto-generated method stub
		return null;
	}

}
