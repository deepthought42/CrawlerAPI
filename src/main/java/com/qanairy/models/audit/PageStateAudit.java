package com.qanairy.models.audit;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.List;

import com.qanairy.models.PageState;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditSubcategory;

/**
 * Defines the globally required fields for all audits
 */
public abstract class PageStateAudit extends Audit{
	
	private String ada_compliance;
	private String description; //definition
	private List<String> best_practices;
	private List<String> recommendations;
	private List<String> observations;
	
	/**
	 * Construct empty action object
	 */
	public PageStateAudit(){
		setCreatedAt(LocalDateTime.now());
	}
	
	/**
	 * 
	 * @param category
	 * @param best_practices
	 * @param ada_compliance_description
	 * @param description
	 * @param name
	 */
	public PageStateAudit(AuditCategory category, List<String> best_practices, String ada_compliance_description, String description, AuditSubcategory subcategory) {
		setBestPractices(best_practices);
		setAdaCompliance(ada_compliance_description);
		setDescription(description);
		setSubcategory(subcategory);
		setCategory(category);
		setCreatedAt(LocalDateTime.now());
		setKey(generateKey());
	}

	/**
	 * Executes the audit using the given page state and user
	 * 
	 * @param page_state {@link PageState page} to be audited
	 * @param user_id 
	 * 
	 * @return score calculated for audit on page
	 * @throws MalformedURLException 
	 * @throws URISyntaxException 
	 */
	public abstract double execute(PageState page_state, String user_id) throws MalformedURLException, URISyntaxException;
	
	public abstract PageStateAudit clone();

	/**
	 * @return string of hashCodes identifying unique fingerprint of object by the contents of the object
	 */
	public String generateKey() {
		return "audit::"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(this.getSubcategory().toString()+this.getCategory()+this.getCreatedAt().toString()+this.getScore());
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString(){
		return this.getKey();
	}
}
