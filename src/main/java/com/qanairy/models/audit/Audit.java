package com.qanairy.models.audit;

import java.time.LocalDateTime;
import java.util.List;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditSubcategory;

/**
 * Defines the globally required fields for all audits
 */
@NodeEntity
public abstract class Audit {
	@GeneratedValue
    @Id
	private Long id;
	
	private String key;
	private String category;
	private String subcategory; // name of the audit subcategory
	private String ada_compliance;
	private String description; //definition
	private double score;      //scoring
	private List<String> best_practices;
	private List<String> recommendations;
	private List<String> observations;
	private LocalDateTime created_at;
	
	/**
	 * Construct empty action object
	 */
	public Audit(){
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
	public Audit(AuditCategory category, List<String> best_practices, String ada_compliance_description, String description, AuditSubcategory subcategory) {
		setBestPractices(best_practices);
		setAdaCompliance(ada_compliance_description);
		setDescription(description);
		setSubcategory(subcategory);
		setCategory(category);
		setCreatedAt(LocalDateTime.now());
		setKey(generateKey());
	}

	public abstract Audit clone();

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
	
	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public LocalDateTime getCreatedAt() {
		return created_at;
	}

	public void setCreatedAt(LocalDateTime created_at) {
		this.created_at = created_at;
	}
}
