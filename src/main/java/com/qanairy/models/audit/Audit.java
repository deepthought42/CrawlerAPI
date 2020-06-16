package com.qanairy.models.audit;

import java.util.List;

import com.qanairy.models.LookseeObject;
import com.qanairy.models.PageState;
import com.qanairy.models.enums.AuditCategory;

/**
 * Defines the globally required fields for all audits
 */
public abstract class Audit extends LookseeObject{
	private String category;
	private String name; // name of the audit subcategory
	private String ada_compliance;
	private String description; //definition
	private Double score;      //scoring
	private List<String> best_practices;
	private List<String> recommendations;
	private List<String> observations;
	
	/**
	 * Construct empty action object
	 */
	public Audit(){}
	
	/**
	 * 
	 * @param category
	 * @param best_practices
	 * @param ada_compliance_description
	 * @param description
	 * @param name
	 */
	public Audit(AuditCategory category, List<String> best_practices, String ada_compliance_description, String description, String name) {
		setBestPractices(best_practices);
		setAdaCompliance(ada_compliance_description);
		setDescription(description);
		setName(name);
		setCategory(category);
		setKey(generateKey());
	}

	/**
	 * Executes the audit using the given page state and user
	 * 
	 * @param page_state {@link PageState page} to be audited
	 * @param user_id 
	 * 
	 * @return score calculated for audit on page
	 */
	public abstract double execute(PageState page_state, String user_id);
	
	public abstract Audit clone();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateKey() {
		return "audit::"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(this.getName()+this.getCategory()+this.getCreatedAt().toString()+this.getScore());
	}

	public AuditCategory getCategory() {
		return AuditCategory.create(category);
	}

	public void setCategory(AuditCategory category) {
		this.category = category.toString();
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

	public List<String> getObservations() {
		return observations;
	}

	public void setObservations(List<String> observations) {
		this.observations = observations;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Double getScore() {
		return score;
	}

	public void setScore(Double score) {
		this.score = score;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
