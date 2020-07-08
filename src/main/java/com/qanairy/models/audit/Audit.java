package com.qanairy.models.audit;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.neo4j.ogm.annotation.Relationship;

import com.qanairy.models.ElementState;
import com.qanairy.models.LookseeObject;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditLevel;
import com.qanairy.models.enums.AuditSubcategory;

/**
 * Defines the globally required fields for all audits
 */
public class Audit extends LookseeObject {

	private String category;
	private String subcategory; // name of the audit subcategory
	private String level;
	private String ada_compliance;
	private String description; //definition
	private double score;      //scoring
	private List<String> best_practices;
	private List<String> recommendations;
	private List<String> observations;
	
	@Relationship(type="FLAGGED")
	List<ElementState> flagged_elements = new ArrayList<>();
	
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
	public Audit(AuditCategory category, List<String> best_practices, String ada_compliance_description, String description, AuditSubcategory subcategory, double score, List<String> observations, AuditLevel level) {
		setBestPractices(best_practices);
		setAdaCompliance(ada_compliance_description);
		setDescription(description);
		setSubcategory(subcategory);
		setCategory(category);
		setScore(score);
		setObservations(observations);
		setCreatedAt(LocalDateTime.now());
		setLevel(level);
		setKey(generateKey());
	}

	public Audit clone() {
		return new Audit(getCategory(), getBestPractices(), getAdaCompliance(), getDescription(), getSubcategory(), getScore(), getObservations(), getLevel());
	}

	/**
	 * @return string of hashCodes identifying unique fingerprint of object by the contents of the object
	 */
	public String generateKey() {
		return "audit::"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(this.getSubcategory().toString()+this.getCategory()+this.getCreatedAt().toString()+this.getScore());
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

	public AuditSubcategory getSubcategory() {
		return AuditSubcategory.create(subcategory);
	}

	public void setSubcategory(AuditSubcategory subcategory) {
		this.subcategory = subcategory.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString(){
		return this.getKey();
	}

	public AuditLevel getLevel() {
		return AuditLevel.create(level);
	}

	public void setLevel(AuditLevel level) {
		this.level = level.toString();
	}
}
