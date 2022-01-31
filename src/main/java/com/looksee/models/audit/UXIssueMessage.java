package com.looksee.models.audit;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.neo4j.ogm.annotation.Relationship;

import com.looksee.models.LookseeObject;
import com.looksee.models.audit.recommend.Recommendation;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.ObservationType;
import com.looksee.models.enums.Priority;

public class UXIssueMessage extends LookseeObject {
	private String title;
	private String description;
	private String why_it_matters;
	private String recommendation;
	private String priority;
	private String type;
	private String category;
	private String wcag_compliance;
	private Set<String> labels;
	private int points;
	private int max_points;
	private int score;
	
	@Relationship(type = "RECOMMEND")
	private Set<Recommendation> recommendations;

	public UXIssueMessage() {
		setRecommendations(new HashSet<>());
	}
	
	public UXIssueMessage(
			Priority priority,
			String description, 
			ObservationType type,
			AuditCategory category,
			String wcag_compliance,
			Set<String> labels,
			String why_it_matters, 
			String title, 
			int points, 
			int max_points, 
			Set<Recommendation> recommendations, 
			String recommendation
	) {
		assert priority != null;
		assert category != null;
		assert labels != null;

		setRecommendations(recommendations);
		setPriority(priority);
		setDescription(description);
		setType(type);
		setCategory(category);
		setWcagCompliance(wcag_compliance);
		setLabels(labels);
		setWhyItMatters(why_it_matters);
		setTitle(title);
		setPoints(points);
		setMaxPoints(max_points);
		setScore( (int)((points/(double)max_points)*100) );
		setRecommendation(recommendation);
		setKey(generateKey());
	}
	
	public Priority getPriority() {
		return Priority.create(this.priority);
	}
	
	public void setPriority(Priority priority) {
		this.priority = priority.getShortName();
	}

	public Set<Recommendation> getRecommendations() {
		return recommendations;
	}

	public void setRecommendations(Set<Recommendation> recommendations) {
		this.recommendations = recommendations;
	}
	
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
	public ObservationType getType() {
		return ObservationType.create(type);
	}

	public void setType(ObservationType type) {
		this.type = type.getShortName();
	}

	public AuditCategory getCategory() {
		return AuditCategory.create(category);
	}

	public void setCategory(AuditCategory category) {
		this.category = category.getShortName();
	}

	public Set<String> getLabels() {
		return labels;
	}

	public void setLabels(Set<String> labels) {
		this.labels = labels;
	}

	@Override
	public String generateKey() {
		return "issuemessage"+UUID.randomUUID();
	}

	public String getWcagCompliance() {
		return wcag_compliance;
	}

	public void setWcagCompliance(String wcag_compliance) {
		this.wcag_compliance = wcag_compliance;
	}

	public String getWhyItMatters() {
		return why_it_matters;
	}

	public void setWhyItMatters(String why_it_matters) {
		this.why_it_matters = why_it_matters;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public int getPoints() {
		return points;
	}

	public void setPoints(int points) {
		this.points = points;
	}

	public int getMaxPoints() {
		return max_points;
	}

	public void setMaxPoints(int max_points) {
		this.max_points = max_points;
	}

	public String getRecommendation() {
		return recommendation;
	}

	public void setRecommendation(String recommendation) {
		this.recommendation = recommendation;
	}

	public int getScore() {
		return score;
	}

	public void setScore(int score) {
		this.score = score;
	}
}
