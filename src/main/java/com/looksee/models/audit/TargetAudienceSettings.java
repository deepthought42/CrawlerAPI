package com.looksee.models.audit;

/**
 * Represents score as a combination of a score achieved and max possible score. This object also contains a set of
 * {@link UXIssueMessage issues} that were experienced while generating score
 */
public class TargetAudienceSettings {

	private String favorite_audit_category;
	private String target_user_age;
	private String target_user_education;
	
	public TargetAudienceSettings() {}
	
	public TargetAudienceSettings(String favorite_audit_category, String target_age, String target_education) {
		setFavoriteAuditCategory(favorite_audit_category);
		setTargetUserEducation(target_education);
		setTargetUserAge(target_age);
	}

	public String getFavoriteAuditCategory() {
		return favorite_audit_category;
	}

	public void setFavoriteAuditCategory(String favorite_audit_category) {
		this.favorite_audit_category = favorite_audit_category;
	}

	public String getTargetUserAge() {
		return target_user_age;
	}

	public void setTargetUserAge(String taget_user_age) {
		this.target_user_age = taget_user_age;
	}

	public String getTargetUserEducation() {
		return target_user_education;
	}

	public void setTargetUserEducation(String target_user_education) {
		this.target_user_education = target_user_education;
	}
}
