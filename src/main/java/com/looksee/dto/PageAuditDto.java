package com.looksee.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.ExecutionStatus;

/**
 * Client facing audit record.
 */
public class PageAuditDto {
    private long id;
    private String url;
	private String status;
	private String statusMessage;
	private String level;

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
	@JsonSerialize(using = LocalDateTimeSerializer.class)
	private LocalDateTime startTime;

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
	@JsonSerialize(using = LocalDateTimeSerializer.class)
	private LocalDateTime endTime;

	private double contentAuditProgress;
	private double contentAuditScore;
	
	private double infoArchitectureAuditProgress;
	private double infoArchScore;
	
	private double aestheticAuditProgress;
	private double aestheticScore;
	
	private double dataExtractionProgress;

	private String targetUserAge;
	private String targetUserEducation;

;
    
    public PageAuditDto() {
		setStartTime(LocalDateTime.now());
		setStatus(ExecutionStatus.UNKNOWN);
		setUrl("");
		setStatusMessage("");
		setLevel(AuditLevel.UNKNOWN);
		setContentAuditProgress(0.0);
		setContentAuditScore(0.0);
		setInfoArchitectureAuditProgress(0.0);
		setInfoArchScore(0.0);
		setAestheticAuditProgress(0.0);
		setAestheticScore(0.0);
		setDataExtractionProgress(0.0);
	}
	
	/**
	 * Constructor
	 * @param level TODO
	 * 
	 */
	public PageAuditDto(long id,
					   ExecutionStatus status,
					   AuditLevel level,
					   LocalDateTime startTime,
					   double aestheticAuditProgress,
					   double aestheticScore,
					   double contentAuditScore,
					   double contentAuditProgress,
					   double infoArchScore,
					   double infoArchAuditProgress,
					   double dataExtractionProgress,
					   LocalDateTime created_at,
					   LocalDateTime endTime,
					   String url
	) {
		setId(id);
		setStatus(status);
		setLevel(level);
		setStartTime(endTime);
		setAestheticAuditProgress(dataExtractionProgress);
		setAestheticScore(aestheticScore);
		setContentAuditScore(contentAuditScore);
		setContentAuditProgress(contentAuditProgress);
		setInfoArchScore(infoArchScore);
		setInfoArchitectureAuditProgress(infoArchAuditProgress);
		setDataExtractionProgress(dataExtractionProgress);
		setStartTime(created_at);
		setEndTime(endTime);
		setUrl(url);
	}

	public String generateKey() {
		return "auditrecord:" + UUID.randomUUID().toString() + org.apache.commons.codec.digest.DigestUtils.sha256Hex(System.currentTimeMillis() + "");
	}

    public void setId(long id){
        this.id = id;
    }

    public long getId(){
        return id;
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
		return startTime;
	}

	public void setStartTime(LocalDateTime start_time) {
		this.startTime = start_time;
	}

	public LocalDateTime getEndTime() {
		return endTime;
	}

	public void setEndTime(LocalDateTime end_time) {
		this.endTime = end_time;
	}
	
	public double getContentAuditProgress() {
		return contentAuditProgress;
	}

	public void setContentAuditProgress(double content_audit_progress) {
		this.contentAuditProgress = content_audit_progress;
	}

	public double getInfoArchitechtureAuditProgress() {
		return infoArchitectureAuditProgress;
	}

	public void setInfoArchitectureAuditProgress(double info_arch_audit_progress) {
		this.infoArchitectureAuditProgress = info_arch_audit_progress;
	}

	public double getAestheticAuditProgress() {
		return aestheticAuditProgress;
	}

	public void setAestheticAuditProgress(double aesthetic_audit_progress) {
		this.aestheticAuditProgress = aesthetic_audit_progress;
	}

	public double getContentAuditScore() {
		return contentAuditScore;
	}

	public void setContentAuditScore(double content_audit_score) {
		this.contentAuditScore = content_audit_score;
	}
	
	public double getInfoArchScore() {
		return infoArchScore;
	}

	public void setInfoArchScore(double info_arch_score) {
		this.infoArchScore = info_arch_score;
	}

	public double getAestheticScore() {
		return aestheticScore;
	}

	public void setAestheticScore(double aesthetic_score) {
		this.aestheticScore = aesthetic_score;
	}

	public double getDataExtractionProgress() {
		return dataExtractionProgress;
	}

	public void setDataExtractionProgress(double data_extraction_progress) {
		this.dataExtractionProgress = data_extraction_progress;
	}

	public String getTargetUserAge() {
		return targetUserAge;
	}

	public void setTargetUserAge(String target_user_age) {
		this.targetUserAge = target_user_age;
	}

	public String getTargetUserEducation() {
		return targetUserEducation;
	}

	public void setTargetUserEducation(String target_user_education) {
		this.targetUserEducation = target_user_education;
	}

	public String getStatusMessage() {
		return statusMessage;
	}

	public void setStatusMessage(String status_message) {
		this.statusMessage = status_message;
	}
	
	@Override
	public String toString() {
		return this.getId()+", "+this.getUrl()+", "+this.getStatus()+", "+this.getStatusMessage();
	}
	
	public boolean isComplete() {
		return (this.getAestheticAuditProgress() >= 1.0
				&& this.getContentAuditProgress() >= 1.0
				&& this.getInfoArchitechtureAuditProgress() >= 1.0
				&& this.getDataExtractionProgress() >= 1.0);
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
}
