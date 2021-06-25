package com.looksee.models.audit;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.neo4j.ogm.annotation.Relationship;

import com.looksee.models.AuditSubcategoryStat;

public class DomainAuditStats extends AuditStats{	
	
	private long total_pages_audited;
	private long high_impact_issue_count;
	private long mid_impact_issue_count;
	private long low_impact_issue_count;
	
	@Relationship(type = "HAS")
	private Set<AuditSubcategoryStat> subcategory_stats;
	
	public DomainAuditStats() {}
	
	public DomainAuditStats(long audit_record_id) {
		setStartTime(LocalDateTime.now());
		setAuditRecordId(audit_record_id);
	}
	
	public DomainAuditStats(
			long audit_record_id,
			LocalDateTime start_time,
			LocalDateTime end_time,
			long page_count,
			long content_pages_audited,
			double content_audit_progress,
			double written_content_score,
			double imagery_score,
			double videos_score,
			double audio_score,
			String content_msg,
			long info_arch_pages_audited,
			double info_arch_audit_progress,
			double seo_score,
			double menu_analysis_score,
			double performance_score,
			String info_arch_msg,
			long aesthetic_pages_audited, 
			double aesthetic_audit_progress, 
			double color_score,
			double typography_score, 
			double whitespace_score, 
			double branding_score,
			String aesthetic_msg,
			double overall_score,
			long high_impact_issues, 
			long mid_impact_issues,
			long low_impact_issues
	) {
		setStartTime(start_time);
		setEndTime(end_time);
		setAuditRecordId(audit_record_id);
		setSubcategoryStats(new HashSet<>());
		setContentPagesAudited(content_pages_audited);
		setContentAuditProgress(content_audit_progress);
		setWrittenContentScore(written_content_score);
		setImageryScore(imagery_score);
		setVideosScore(videos_score);
		setAudioScore(audio_score);

		setContentMsg(content_msg);
		setInfoArchPagesAudited(info_arch_pages_audited);
		setInfoArchAuditProgress(info_arch_audit_progress);
		setSeoScore(seo_score);
		setMenuAnalysisScore(menu_analysis_score);
		setPerformanceScore(performance_score);
		setInfoArchMsg(info_arch_msg);
		
		setAestheticPagesAudited(aesthetic_pages_audited);
		setAestheticAuditProgress(aesthetic_audit_progress);
		setColorScore(color_score);
		setTypographyScore(typography_score);
		setWhitespaceScore(whitespace_score);
		setBrandingScore(branding_score);
		setAestheticMsg(aesthetic_msg);
		
		setOverallScore(overall_score);
		setTotalPagesAudited(page_count);
		setHighImpactIssueCount(high_impact_issues);
		setMidImpactIssueCount(mid_impact_issues);
		setLowImpactIssueCount(low_impact_issues);
	}


	public long getTotalPagesAudited() {
		return total_pages_audited;
	}

	public void setTotalPagesAudited(long total_pages_audited) {
		this.total_pages_audited = total_pages_audited;
	}

	public long getHighImpactIssueCount() {
		return high_impact_issue_count;
	}

	public void setHighImpactIssueCount(long high_impact_issue_cnt) {
		this.high_impact_issue_count = high_impact_issue_cnt;
	}

	public long getMidImpactIssueCount() {
		return mid_impact_issue_count;
	}

	public void setMidImpactIssueCount(long mid_impact_issue_count) {
		this.mid_impact_issue_count = mid_impact_issue_count;
	}

	public long getLowImpactIssueCount() {
		return low_impact_issue_count;
	}

	public void setLowImpactIssueCount(long low_impact_issue_count) {
		this.low_impact_issue_count = low_impact_issue_count;
	}
}
