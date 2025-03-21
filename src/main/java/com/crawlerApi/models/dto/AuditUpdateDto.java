package com.crawlerApi.models.dto;

import com.crawlerApi.models.enums.AuditLevel;
import com.crawlerApi.models.enums.ExecutionStatus;

/**
 * Data transfer object for {@link Domain} object that is designed to comply with
 * the data format for browser extensions
 */
public class AuditUpdateDto {
	private long id;
	private AuditLevel level;
	private double content_score;
	private double content_progress;
	private double written_content_score;
	private double imagery_score;
	
	private double info_architecture_score;
	private double info_architecture_progress;
	private double seo_score;
	private double link_score;
	
	private double accessibility_score;
	private double accessibility_progress;
	
	private double aesthetics_score;
	private double aesthetics_progress;
	private double text_contrast_score;
	private double non_text_contrast_score;
	
	private double data_extraction_progress;
	private double overall_score;
	private String message;
	private String status;
	
	public AuditUpdateDto(){}

	public AuditUpdateDto(
			long id,
			AuditLevel level,
			double content_score,
			double content_progress,
			double written_content_score,
			double imagery_score,
			double info_architecture_score,
			double info_architecture_progress,
			double seo_score,
			double link_score, 
			double accessibility_score,
			double aesthetics_score, 
			double aesthetics_progress, 
			double text_contrast_score, 
			double element_contrast_score, 
			double data_extraction_progress, 
			String message, 
			ExecutionStatus status
	){
		setId(id);
		setLevel(level);
		
		setContentScore(content_score);
		setContentProgress(content_progress);
		setWrittenContentScore(written_content_score);
		setImageryScore(imagery_score);
		
		setInfoArchitectureScore(info_architecture_score);
		setInfoArchitectureProgress(info_architecture_progress);
		setSeoScore(seo_score);
		setLinkScore(link_score);
		
		setAccessibilityScore(accessibility_score);
		
		setAestheticsScore(aesthetics_score);
		setAestheticsProgress(aesthetics_progress);
		setTextContrastScore(text_contrast_score);
		setNonTextContrastScore(element_contrast_score);
		
		setDataExtractionProgress(data_extraction_progress);
		setOverallScore((content_score+info_architecture_score+aesthetics_score)/3);
		setMessage(message);
		setStatus(status);
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public double getContentScore() {
		return content_score;
	}

	public void setContentScore(double content_score) {
		this.content_score = content_score;
	}

	public double getInfoArchitectureScore() {
		return info_architecture_score;
	}

	public void setInfoArchitectureScore(double info_architecture_score) {
		this.info_architecture_score = info_architecture_score;
	}

	public double getAccessibilityScore() {
		return accessibility_score;
	}

	public void setAccessibilityScore(double accessibility_score) {
		this.accessibility_score = accessibility_score;
	}

	public double getAestheticsScore() {
		return aesthetics_score;
	}

	public void setAestheticsScore(double aesthetics_score) {
		this.aesthetics_score = aesthetics_score;
	}

	public double getContentProgress() {
		return content_progress;
	}

	public void setContentProgress(double content_progress) {
		this.content_progress = content_progress;
	}

	public double getInfoArchitectureProgress() {
		return info_architecture_progress;
	}

	public void setInfoArchitectureProgress(double info_architecture_progress) {
		this.info_architecture_progress = info_architecture_progress;
	}

	public double getAccessibilityProgress() {
		return accessibility_progress;
	}

	public void setAccessibilityProgress(double accessibility_progress) {
		this.accessibility_progress = accessibility_progress;
	}

	public double getAestheticsProgress() {
		return aesthetics_progress;
	}

	public void setAestheticsProgress(double aesthetics_progress) {
		this.aesthetics_progress = aesthetics_progress;
	}

	public double getDataExtractionProgress() {
		return data_extraction_progress;
	}

	public void setDataExtractionProgress(double data_extraction_progress) {
		this.data_extraction_progress = data_extraction_progress;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public ExecutionStatus getStatus() {
		return ExecutionStatus.create(status);
	}

	public void setStatus(ExecutionStatus status) {
		this.status = status.getShortName();
	}

	public AuditLevel getLevel() {
		return level;
	}

	public void setLevel(AuditLevel level) {
		this.level = level;
	}

	public double getOverallScore() {
		return overall_score;
	}

	public void setOverallScore(double overall_score) {
		this.overall_score = overall_score;
	}

	public double getTextContrastScore() {
		return text_contrast_score;
	}

	public void setTextContrastScore(double text_contrast_score) {
		this.text_contrast_score = text_contrast_score;
	}

	public double getNonTextContrastScore() {
		return non_text_contrast_score;
	}

	public void setNonTextContrastScore(double element_contrast_score) {
		this.non_text_contrast_score = element_contrast_score;
	}

	public double getWrittenContentScore() {
		return written_content_score;
	}

	public void setWrittenContentScore(double written_content_score) {
		this.written_content_score = written_content_score;
	}

	public double getImageryScore() {
		return imagery_score;
	}

	public void setImageryScore(double imagery_score) {
		this.imagery_score = imagery_score;
	}

	public double getSeoScore() {
		return seo_score;
	}

	public void setSeoScore(double seo_score) {
		this.seo_score = seo_score;
	}

	public double getLinkScore() {
		return link_score;
	}

	public void setLinkScore(double link_score) {
		this.link_score = link_score;
	}	
}