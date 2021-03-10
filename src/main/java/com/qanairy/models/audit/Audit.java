package com.qanairy.models.audit;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.neo4j.ogm.annotation.Relationship;

import com.qanairy.models.LookseeObject;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditLevel;
import com.qanairy.models.enums.AuditName;
import com.qanairy.models.enums.AuditSubcategory;

/**
 * Defines the globally required fields for all audits
 */
public class Audit extends LookseeObject {

	private String category;
	private String subcategory;
	private String name; // name of the audit
	private String level;
	private int points;      //scoring
	private int total_possible_points;      //scoring
	private String url;
	
	@Relationship(type="OBSERVED")
	private List<Observation> observations;
	
	/**
	 * Construct empty action object
	 */
	public Audit(){
		super();
		setObservations(new ArrayList<>());
	}
	
	/**
	 * 
	 * @param category
	 * @param subcategory
	 * @param points
	 * @param observations
	 * @param level
	 * @param total_possible_points
	 * @param url TODO
	 */
	public Audit(
			AuditCategory category, 
			AuditSubcategory subcategory,
			AuditName name,
			int points, 
			List<Observation> observations, 
			AuditLevel level, 
			int total_possible_points,
			String url
	) {
		super();
		
		assert category != null;
		assert subcategory != null;
		assert observations != null;
		assert level != null;
		
		setName(name);
		setCategory(category);
		setSubcategory(subcategory);
		setPoints(points);
		setTotalPossiblePoints(total_possible_points);
		setObservations(observations);
		setCreatedAt(LocalDateTime.now());
		setLevel(level);
		setUrl(url);
		setKey(generateKey());
	}

	public Audit clone() {
		return new Audit(getCategory(), getSubcategory(), getName(), getPoints(), getObservations(), getLevel(), getTotalPossiblePoints(), getUrl());
	}

	/**
	 * @return string of hashCodes identifying unique fingerprint of object by the contents of the object
	 */
	public String generateKey() {
		return "audit::"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(this.getName().toString()+this.getCategory().toString()+this.getLevel()+getPoints()+getTotalPossiblePoints()+getCreatedAt().toString());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString(){
		return this.getKey();
	}

	public AuditCategory getCategory() {
		return AuditCategory.create(category);
	}
	
	public void setCategory(AuditCategory category) {
		this.category = category.getShortName();
	}
	
	
	public List<Observation> getObservations() {
		return observations;
	}
	
	public void setObservations(List<Observation> observations) {
		this.observations = observations;
	}
	
	
	public int getPoints() {
		return points;
	}
	
	public void setPoints(int points) {
		this.points = points;
	}
	
	public AuditName getName() {
		return AuditName.create(name);
	}
	
	public void setName(AuditName subcategory) {
		this.name = subcategory.getShortName();
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
	

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public AuditSubcategory getSubcategory() {
		return AuditSubcategory.create(subcategory);
	}

	public void setSubcategory(AuditSubcategory subcategory) {
		this.subcategory = subcategory.toString();
	}
}
