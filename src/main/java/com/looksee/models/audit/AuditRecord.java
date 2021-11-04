package com.looksee.models.audit;

import java.time.LocalDateTime;
import java.util.UUID;

import com.looksee.models.LookseeObject;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.ExecutionStatus;

/**
 * Record detailing an set of {@link Audit audits}.
 */
public class AuditRecord extends LookseeObject {
	
	private String status;
	private String level;
	private LocalDateTime start_time;
	private LocalDateTime end_time;
	private double content_audit_progress;
	private String content_audit_msg;
	
	private double info_arch_audit_progress;
	private String info_arch_msg;
	
	private double aesthetic_audit_progress;
	private String aesthetic_msg;
	
	private double data_extraction_progress;
	private String data_extraction_msg;

	private String favorite_audit_category;
	private String target_user_age;
	private String target_user_education;
	
	public AuditRecord() {
		setStartTime(LocalDateTime.now());
	}
	
	/**
	 * Constructor
	 * @param level TODO
	 * 
	 * @pre audit_stats != null;
	 */
	public AuditRecord(ExecutionStatus status, AuditLevel level) {
		setKey(generateKey());
		setStatus(status);
		setLevel(level);
		
		setContentAuditProgress(0.0);
		setInfoArchAuditProgress(0.0);
		setAestheticAuditProgress(0.0);
		
		setStartTime(LocalDateTime.now());
	}

	public String generateKey() {
		return "auditrecord:" + UUID.randomUUID().toString() + org.apache.commons.codec.digest.DigestUtils.sha256Hex(System.currentTimeMillis() + "");
	}

	public ExecutionStatus getStatus() {
		return ExecutionStatus.create(status);
	}

	public void setStatus(ExecutionStatus status) {
		this.status = status.getShortName();
	}

	public AuditLevel getLevel() {
		return AuditLevel.create(level);
	}

	public void setLevel(AuditLevel level) {
		this.level = level.toString();
	}

	public LocalDateTime getStartTime() {
		return start_time;
	}

	public void setStartTime(LocalDateTime start_time) {
		this.start_time = start_time;
	}

	public LocalDateTime getEndTime() {
		return end_time;
	}

	public void setEndTime(LocalDateTime end_time) {
		this.end_time = end_time;
	}
	
	public double getContentAuditProgress() {
		return content_audit_progress;
	}

	public void setContentAuditProgress(double content_audit_progress) {
		this.content_audit_progress = content_audit_progress;
	}

	public double getInfoArchAuditProgress() {
		return info_arch_audit_progress;
	}

	public void setInfoArchAuditProgress(double info_arch_audit_progress) {
		this.info_arch_audit_progress = info_arch_audit_progress;
	}

	public double getAestheticAuditProgress() {
		return aesthetic_audit_progress;
	}

	public void setAestheticAuditProgress(double aesthetic_audit_progress) {
		this.aesthetic_audit_progress = aesthetic_audit_progress;
	}

	public String getContentAuditMsg() {
		return content_audit_msg;
	}

	public void setContentAuditMsg(String content_audit_msg) {
		this.content_audit_msg = content_audit_msg;
	}
	
	public String getInfoArchMsg() {
		return info_arch_msg;
	}

	public void setInfoArchMsg(String info_arch_msg) {
		this.info_arch_msg = info_arch_msg;
	}

	public String getAestheticMsg() {
		return aesthetic_msg;
	}

	public void setAestheticMsg(String aesthetic_msg) {
		this.aesthetic_msg = aesthetic_msg;
	}

	public double getDataExtractionProgress() {
		return data_extraction_progress;
	}

	public void setDataExtractionProgress(double data_extraction_progress) {
		this.data_extraction_progress = data_extraction_progress;
	}

	public String getDataExtractionMsg() {
		return data_extraction_msg;
	}

	public void setDataExtractionMsg(String data_extraction_msg) {
		this.data_extraction_msg = data_extraction_msg;
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

	public void setTargetUserAge(String target_user_age) {
		this.target_user_age = target_user_age;
	}

	public String getTargetUserEducation() {
		return target_user_education;
	}

	public void setTargetUserEducation(String target_user_education) {
		this.target_user_education = target_user_education;
	}
}
