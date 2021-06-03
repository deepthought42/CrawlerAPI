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
	private long pages_found;
	private long content_pages_audited;
	private double content_audit_progress;
	private String content_msg;
	
	private long info_arch_pages_audited;
	private double info_arch_audit_progress;
	private String info_arch_msg;
	
	private long aesthetic_pages_audited;
	private double aesthetic_audit_progress;
	private String aesthetic_msg;
	
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
			long page_count, 
			long content_pages_audited,
			double content_audit_progress,
			String content_msg,
			long info_arch_pages_audited,
			double info_arch_audit_progress,
			String info_arch_msg,
			long aesthetic_pages_audited,
			double aesthetic_audit_progress,
			String aesthetic_msg
	) {
		setStartTime(start_time);
		setEndTime(end_time);
		setAuditRecordId(audit_record_id);
		setSubcategoryStats(new HashSet<>());
		setContentPagesAudited(content_pages_audited);
		setContentAuditProgress(content_audit_progress);
		setContentMsg(content_msg);
		setInfoArchPagesAudited(info_arch_pages_audited);
		setInfoArchAuditProgress(info_arch_audit_progress);
		setInfoArchMsg(info_arch_msg);
		setAestheticPagesAudited(aesthetic_pages_audited);
		setAestheticAuditProgress(aesthetic_audit_progress);
		setAestheticMsg(aesthetic_msg);
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

	public long getPagesFound() {
		return pages_found;
	}

	public void setPagesFound(long pages_found) {
		this.pages_found = pages_found;
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
}
