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
public class AuditDto {
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
	private String contentAuditMsg;
	
	private double infoArchitectureAuditProgress;
	private String infoArchMsg;
	
	private double aestheticAuditProgress;
	private String aestheticMsg;
	
	private double dataExtractionProgress;
	private String dataExtractionMsg;

	private String targetUserAge;
	private String targetUserEducation;

;
    
    public AuditDto() {
		setStartTime(LocalDateTime.now());
		setStatus(ExecutionStatus.UNKNOWN);
		setUrl("");
		setStatusMessage("");
		setLevel(AuditLevel.UNKNOWN);
		setContentAuditProgress(0.0);
		setContentAuditMsg("");
		setInfoArchitectureAuditProgress(0.0);
		setInfoArchMsg("");
		setAestheticAuditProgress(0.0);
		setAestheticMsg("");
		setDataExtractionProgress(0.0);
		setDataExtractionMsg("");
	}
	
	/**
	 * Constructor
	 * @param level TODO
	 * 
	 */
	public AuditDto(long id, 
					   ExecutionStatus status, 
					   AuditLevel level, 
					   LocalDateTime startTime,
					   double aestheticAuditProgress, 
					   String aestheticMsg, 
					   String contentAuditMsg, 
					   double contentAuditProgress,
					   String infoArchMsg, 
					   double infoArchAuditProgress, 
					   String dataExtractionMsg, 
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
		setAestheticMsg(aestheticMsg);
		setContentAuditMsg(contentAuditMsg);
		setContentAuditProgress(contentAuditProgress);
		setInfoArchMsg(infoArchMsg);
		setInfoArchitectureAuditProgress(infoArchAuditProgress);
		setDataExtractionMsg(dataExtractionMsg);
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

	public String getContentAuditMsg() {
		return contentAuditMsg;
	}

	public void setContentAuditMsg(String content_audit_msg) {
		this.contentAuditMsg = content_audit_msg;
	}
	
	public String getInfoArchMsg() {
		return infoArchMsg;
	}

	public void setInfoArchMsg(String info_arch_msg) {
		this.infoArchMsg = info_arch_msg;
	}

	public String getAestheticMsg() {
		return aestheticMsg;
	}

	public void setAestheticMsg(String aesthetic_msg) {
		this.aestheticMsg = aesthetic_msg;
	}

	public double getDataExtractionProgress() {
		return dataExtractionProgress;
	}

	public void setDataExtractionProgress(double data_extraction_progress) {
		this.dataExtractionProgress = data_extraction_progress;
	}

	public String getDataExtractionMsg() {
		return dataExtractionMsg;
	}

	public void setDataExtractionMsg(String data_extraction_msg) {
		this.dataExtractionMsg = data_extraction_msg;
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
