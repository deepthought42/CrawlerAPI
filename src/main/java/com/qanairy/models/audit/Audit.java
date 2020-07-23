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
	private int points;      //scoring
	private int total_possible_points;      //scoring

;
	
	@Relationship("OBSERVED")
	private List<Observation> observations;
	
	@Relationship(type="FLAGGED")
	List<ElementState> flagged_elements = new ArrayList<>();
	
	/**
	 * Construct empty action object
	 */
	public Audit(){}
	
	/**
	 * 
	 * @param category
	 * @param total_possible_points TODO
	 * @param name
	 */
	public Audit(AuditCategory category, AuditSubcategory subcategory, int score, List<Observation> observations, AuditLevel level, int total_possible_points) {
		setSubcategory(subcategory);
		setCategory(category);
		setScore(score);
		setTotalPossiblePoints(total_possible_points);
		setObservations(observations);
		setCreatedAt(LocalDateTime.now());
		setLevel(level);
		setKey(generateKey());
	}

	public Audit clone() {
		return new Audit(getCategory(), getSubcategory(), getScore(), getObservations(), getLevel(), getTotalPossiblePoints());
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

	

	public List<Observation> getObservations() {
		return observations;
	}

	public void setObservations(List<Observation> observations) {
		this.observations = observations;
	}


	public int getScore() {
		return points;
	}

	public void setScore(int score) {
		this.points = score;
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

	public int getTotalPossiblePoints() {
		return total_possible_points;
	}

	public void setTotalPossiblePoints(int total_possible_points) {
		this.total_possible_points = total_possible_points;
	}
}
