package com.looksee.models.audit;

public class AuditScore {
	private double content_score;
	private double information_architecture_score;
	private double aesthetics_score;
	private double interactivity_score;
	private double accessibility_score;
	
	public AuditScore(double content_score,
					 double information_architecture_score,
					 double aesthetic_score,
					 double interactivity_score,
					 double accessibility_score) {
		setContentScore(content_score);
		setInformationArchitectureScore(information_architecture_score);
		setAestheticsScore(aesthetic_score);
		setInteractivityScore(interactivity_score);
		setAccessibilityScore(accessibility_score);
	}
	
	
	public double getContentScore() {
		return content_score;
	}
	
	public void setContentScore(double content_score) {
		this.content_score = content_score;
	}

	public double getInformationArchitectureScore() {
		return information_architecture_score;
	}

	public void setInformationArchitectureScore(double information_architecture_score) {
		this.information_architecture_score = information_architecture_score;
	}

	public double getAestheticsScore() {
		return aesthetics_score;
	}

	public void setAestheticsScore(double aesthetics_score) {
		this.aesthetics_score = aesthetics_score;
	}

	public double getInteractivityScore() {
		return interactivity_score;
	}

	public void setInteractivityScore(double interactivity_score) {
		this.interactivity_score = interactivity_score;
	}

	public double getAccessibilityScore() {
		return accessibility_score;
	}

	public void setAccessibilityScore(double accessibility_score) {
		this.accessibility_score = accessibility_score;
	}
}
