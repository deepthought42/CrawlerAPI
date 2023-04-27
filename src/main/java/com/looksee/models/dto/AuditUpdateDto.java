package com.looksee.models.dto;

import com.looksee.models.audit.AuditScore;
import com.looksee.models.enums.AuditLevel;

/**
 * Data transfer object that contains the progress of each audit category
 *
 */
public class AuditUpdateDto {
	
	private long auditRecordId;
	private AuditLevel auditType;
	private double dataExtractionProgress;
	private double aestheticAuditProgress; 
	private double contentAuditProgress; 
	private double infoArchitectureAuditProgress;
	private double overallProgress;
	private int completePages;
	private int totalPages;
	private AuditScore auditScore;

	public AuditUpdateDto(long audit_record_id, 
						  AuditLevel audit_type, 
						  double data_extraction_progress,
						  double aesthetic_audit_progress, 
						  double content_audit_progress, 
						  double info_architecture_audit_progress,
						  double overall_progress, 
						  int complete_pages, 
						  int total_pages, 
						  AuditScore score) 
	{
		setAuditRecordId(audit_record_id);
		setAuditType(audit_type);
		setDataExtractionProgress(data_extraction_progress);
		setAestheticAuditProgress(aesthetic_audit_progress);
		setContentAuditProgress(content_audit_progress);
		setInfoArchitectureAuditProgress(info_architecture_audit_progress);
		setOverallProgress(overall_progress);
		setCompletePages(complete_pages);
		setTotalPages(total_pages);
		setAuditScore(score);
	}

	public long getAuditRecordId() {
		return auditRecordId;
	}

	public void setAuditRecordId(long auditRecordId) {
		this.auditRecordId = auditRecordId;
	}

	public AuditLevel getAuditType() {
		return auditType;
	}

	public void setAuditType(AuditLevel auditType) {
		this.auditType = auditType;
	}

	public double getDataExtractionProgress() {
		return dataExtractionProgress;
	}

	public void setDataExtractionProgress(double dataExtractionProgress) {
		this.dataExtractionProgress = dataExtractionProgress;
	}

	public double getAestheticAuditProgress() {
		return aestheticAuditProgress;
	}

	public void setAestheticAuditProgress(double aestheticAuditProgress) {
		this.aestheticAuditProgress = aestheticAuditProgress;
	}

	public double getContentAuditProgress() {
		return contentAuditProgress;
	}

	public void setContentAuditProgress(double contentAuditProgress) {
		this.contentAuditProgress = contentAuditProgress;
	}

	public double getInfoArchitectureAuditProgress() {
		return infoArchitectureAuditProgress;
	}

	public void setInfoArchitectureAuditProgress(double infoArchitectureAuditProgress) {
		this.infoArchitectureAuditProgress = infoArchitectureAuditProgress;
	}

	public double getOverallProgress() {
		return overallProgress;
	}

	public void setOverallProgress(double overallProgress) {
		this.overallProgress = overallProgress;
	}

	public int getCompletePages() {
		return completePages;
	}

	public void setCompletePages(int completePages) {
		this.completePages = completePages;
	}

	public int getTotalPages() {
		return totalPages;
	}

	public void setTotalPages(int totalPages) {
		this.totalPages = totalPages;
	}

	public AuditScore getAuditScore() {
		return auditScore;
	}

	public void setAuditScore(AuditScore auditScore) {
		this.auditScore = auditScore;
	}
	
}
