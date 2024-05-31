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
public class AuditRecordDto {
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

	private double contentAuditScore;
	private double infoArchScore;
	private double aestheticScore;
	
	private String targetUserAge;
	private String targetUserEducation;

;
    
    public AuditRecordDto() {
		setStartTime(LocalDateTime.now());
		setStatus(ExecutionStatus.UNKNOWN);
		setUrl("");
		setStatusMessage("");
		setLevel(AuditLevel.UNKNOWN);
		setContentAuditScore(0.0);
		setInfoArchScore(0.0);
		setAestheticScore(0.0);
	}
	
	/**
	 * Constructor
	 * @param level TODO
	 * 
	 */
	public AuditRecordDto(long id,
					   ExecutionStatus status,
					   AuditLevel level,
					   LocalDateTime startTime,
					   double aestheticScore,
					   double contentAuditScore,
					   double infoArchScore,
					   LocalDateTime created_at,
					   LocalDateTime endTime,
					   String url
	) {
		setId(id);
		setStatus(status);
		setLevel(level);
		setStartTime(endTime);
		setAestheticScore(aestheticScore);
		setContentAuditScore(contentAuditScore);
		setInfoArchScore(infoArchScore);
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
		return false;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
}
