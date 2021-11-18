package com.looksee.models.audit;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.neo4j.ogm.annotation.Relationship;

import com.looksee.models.AuditSubcategoryStat;

public class AuditStats {
	
	private LocalDateTime start_time; //time that the 
	private LocalDateTime end_time;
	private long audit_record_id;
	
	
	//crawl progress trackers
	private long elements_examined;
	private long elements_found;
	private double data_extraction_progress;
	private String data_extraction_message;
	
	private long content_pages_audited;
	private double content_score;
	private double content_audit_progress;
	private String content_msg;
	//content sub-category score
	private double written_content_score;
	private double imagery_score;
	private double videos_score;
	private double audio_score;
	
	private long info_arch_pages_audited;
	private double info_arch_score;
	private double info_arch_audit_progress;
	private String info_arch_msg;
	//info architecture audit sub-categories
	private double seo_score;
	private double menu_analysis_score;
	private double performance_score;
	
	private long aesthetic_pages_audited;
	private double aesthetic_score;
	private double aesthetic_audit_progress;
	private String aesthetic_msg;
	//aesthetic audit sub-categories
	private double color_score;
	private double typography_score;
	private double whitespace_score;
	private double branding_score;
	
	private double overall_score;

	@Relationship(type = "HAS")
	private Set<AuditSubcategoryStat> subcategory_stats;
	
	public AuditStats() {}
	
	public AuditStats(long audit_record_id) {
		setStartTime(LocalDateTime.now());
		setAuditRecordId(audit_record_id);
	}
	
	public AuditStats(
			long audit_record_id,
			LocalDateTime start_time, 
			LocalDateTime end_time,
			long content_pages_audited,
			double content_audit_progress,
			double content_score,
			String content_msg,
			double written_content_score,
			double imagery_score,
			double videos_score,
			double audio_score, 
			long info_arch_pages_audited, 
			double info_arch_audit_progress, 
			double info_arch_score, 
			String info_arch_msg, 
			double seo_score,
			double menu_analysis_score,
			double performance_score, 
			long aesthetic_pages_audited, 
			double aesthetic_audit_progress, 
			double aesthetic_score, 
			String aesthetic_msg, 
			double color_score, 
			double typography_score, 
			double whitespace_score, 
			double branding_score, 
			long elements_examined, 
			long elements_found, 
			double data_extraction_progress,
			String data_extraction_msg
	) {
		setStartTime(start_time);
		setEndTime(end_time);
		setAuditRecordId(audit_record_id);
		setSubcategoryStats(new HashSet<>());
		setContentPagesAudited(content_pages_audited);
		setContentAuditProgress(content_audit_progress);
		setContentScore(content_score);
		setContentMsg(content_msg);
		setWrittenContentScore(written_content_score);
		setImageryScore(imagery_score);
		setVideosScore(videos_score);
		setAudioScore(audio_score);
		setInfoArchPagesAudited(info_arch_pages_audited);
		setInfoArchAuditProgress(info_arch_audit_progress);
		setInfoArchMsg(info_arch_msg);
		setSeoScore(seo_score);
		setMenuAnalysisScore(menu_analysis_score);
		setPerformanceScore(performance_score);
		setColorScore(color_score);
		setTypographyScore(typography_score);
		setWhitespaceScore(whitespace_score);
		setBrandingScore(branding_score);
		setAestheticPagesAudited(aesthetic_pages_audited);
		setAestheticAuditProgress(aesthetic_audit_progress);
		setAestheticMsg(aesthetic_msg);
		setElementsExamined(elements_examined);
		setElementsFound(elements_found);
		setDataExtractionProgress(data_extraction_progress);
		setDataExtractionMessage(data_extraction_msg);
	}


	public double getOverallScore() {
		return overall_score;
	}

	public void setOverallScore(double overall_score) {
		this.overall_score = overall_score;
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

	public Set<AuditSubcategoryStat> getSubcategoryStats() {
		return subcategory_stats;
	}

	public void setSubcategoryStats(Set<AuditSubcategoryStat> subcategory_stats) {
		this.subcategory_stats = subcategory_stats;
	}

	public long getContentPagesAudited() {
		return content_pages_audited;
	}

	public void setContentPagesAudited(long content_pages_audited) {
		this.content_pages_audited = content_pages_audited;
	}

	public long getInfoArchPagesAudited() {
		return info_arch_pages_audited;
	}

	public void setInfoArchPagesAudited(long info_arch_pages_audited) {
		this.info_arch_pages_audited = info_arch_pages_audited;
	}

	public long getAestheticPagesAudited() {
		return aesthetic_pages_audited;
	}

	public void setAestheticPagesAudited(long aesthetic_pages_audited) {
		this.aesthetic_pages_audited = aesthetic_pages_audited;
	}

	public long getAuditRecordId() {
		return audit_record_id;
	}

	public void setAuditRecordId(long audit_record_id) {
		this.audit_record_id = audit_record_id;
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

	public String getContentMsg() {
		return content_msg;
	}

	public void setContentMsg(String content_msg) {
		this.content_msg = content_msg;
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

	public double getAestheticScore() {
		return aesthetic_score;
	}

	public void setAestheticScore(double aesthetic_pages_score) {
		this.aesthetic_score = aesthetic_pages_score;
	}

	public double getInfoArchScore() {
		return info_arch_score;
	}

	public void setInfoArchScore(double info_arch_pages_score) {
		this.info_arch_score = info_arch_pages_score;
	}

	public double getContentScore() {
		return content_score;
	}

	public void setContentScore(double content_pages_score) {
		this.content_score = content_pages_score;
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

	public double getAudioScore() {
		return audio_score;
	}

	public void setAudioScore(double audio_score) {
		this.audio_score = audio_score;
	}

	public double getVideosScore() {
		return videos_score;
	}

	public void setVideosScore(double videos_score) {
		this.videos_score = videos_score;
	}

	public double getSeoScore() {
		return seo_score;
	}

	public void setSeoScore(double seo_score) {
		this.seo_score = seo_score;
	}

	public double getMenuAnalysisScore() {
		return menu_analysis_score;
	}

	public void setMenuAnalysisScore(double menu_analysis_score) {
		this.menu_analysis_score = menu_analysis_score;
	}

	public double getPerformanceScore() {
		return performance_score;
	}

	public void setPerformanceScore(double performance_score) {
		this.performance_score = performance_score;
	}

	public double getColorScore() {
		return color_score;
	}

	public void setColorScore(double color_score) {
		this.color_score = color_score;
	}

	public double getTypographyScore() {
		return typography_score;
	}

	public void setTypographyScore(double typography_score) {
		this.typography_score = typography_score;
	}

	public double getWhitespaceScore() {
		return whitespace_score;
	}

	public void setWhitespaceScore(double whitespace_score) {
		this.whitespace_score = whitespace_score;
	}

	public double getBrandingScore() {
		return branding_score;
	}

	public void setBrandingScore(double branding_score) {
		this.branding_score = branding_score;
	}

	public long getElementsExamined() {
		return elements_examined;
	}

	public void setElementsExamined(long elements_examined) {
		this.elements_examined = elements_examined;
	}

	public long getElementsFound() {
		return elements_found;
	}

	public void setElementsFound(long elements_found) {
		this.elements_found = elements_found;
	}

	public double getDataExtractionProgress() {
		return data_extraction_progress;
	}

	public void setDataExtractionProgress(double data_extraction_progress) {
		this.data_extraction_progress = data_extraction_progress;
	}

	public String getDataExtractionMessage() {
		return data_extraction_message;
	}

	public void setDataExtractionMessage(String data_extraction_message) {
		this.data_extraction_message = data_extraction_message;
	}
}
