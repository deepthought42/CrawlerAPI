package com.looksee.models.audit;

import java.time.LocalDateTime;

import com.looksee.models.enums.ExecutionStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
public class PageAuditStats extends AuditStats{
	
	@Getter
	@Setter
	private LocalDateTime startTime; //time that the 

	@Getter
	@Setter
	private LocalDateTime endTime;

	@Getter
	@Setter
	private long auditRecordId;

	private String status;
	
	@Setter
	@Getter
	private double accessibilityScore;

	@Getter
	@Setter
	private double contentScore;
	
	//content sub-category score
	private int written_content_issue_count;
	private int imagery_issue_count;
	private int video_issue_count;
	private int audit_issue_count;
	private int image_copyright_issue_count;

	@Getter
	@Setter
	private double writtenContentScore;

	@Getter
	@Setter
	private double imageryScore;

	@Getter
	@Setter
	private double videosScore;

	@Getter
	@Setter
	private double audioScore;
	
	@Getter
	@Setter
	private double infoArchitectureScore;
	
	//info architecture audit sub-categories
	private int seo_issue_count;
	private int menu_issue_count;
	private int performance_issue_count;
	private int link_issue_count;
	
	@Getter
	@Setter
	private double seoScore;

	@Getter
	@Setter
	private double menuAnalysisScore;

	@Getter
	@Setter
	private double performanceScore;

	@Getter
	@Setter
	private double linkScore;
	
	@Getter
	@Setter
	private double aestheticScore;
	
	//aesthetic audit sub-categories
	private int text_contrast_issue_count;
	private int non_text_issue_count;
	private int typography_issue_count;
	private int whitespace_issue_count;
	private int branding_issue_count;
	
	private double text_contrast_score;
	private double non_text_contrast_score;
	private double typography_score;
	private double whitespace_score;
	private double branding_score;
		
	private int total_issues;
	private double overall_score;
	
	
	public PageAuditStats(long audit_record_id) {
		setStartTime(LocalDateTime.now());
		setAuditRecordId(audit_record_id);
	}
	
	public PageAuditStats(
			long audit_record_id,
			LocalDateTime start_time,
			LocalDateTime end_time,
			double accessibility_score,
			double content_score,
			double written_content_score,
			double imagery_score,
			double videos_score,
			double audio_score,
			double info_arch_score,
			double seo_score,
			double menu_analysis_score,
			double performance_score,
			double aesthetic_score,
			double text_contrast_score,
			double non_text_contrast_score,
			double typography_score,
			double whitespace_score,
			double branding_score,
			ExecutionStatus status,
			double link_score
	) {
		setAuditRecordId(audit_record_id);
		setStartTime(start_time);
		setEndTime(end_time);
		setAccessibilityScore(accessibility_score);
		setContentScore(content_score);
		setWrittenContentScore(written_content_score);
		setImageryScore(imagery_score);
		setVideosScore(videos_score);
		setAudioScore(audio_score);
		setSeoScore(seo_score);
		setMenuAnalysisScore(menu_analysis_score);
		setPerformanceScore(performance_score);
		setTextContrastScore(text_contrast_score);
		setNonTextContrastScore(non_text_contrast_score);
		setTypographyScore(typography_score);
		setWhitespaceScore(whitespace_score);
		setBrandingScore(branding_score);
		setTotalIssues(total_issues);
		setLinkScore(link_score);
		setStatus(status);
	}


	public double getOverallScore() {
		return overall_score;
	}

	public void setOverallScore(double overall_score) {
		this.overall_score = overall_score;
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

	public int getWrittenContentIssueCount() {
		return written_content_issue_count;
	}

	public void setWrittenContentIssueCount(int written_content_issue_count) {
		this.written_content_issue_count = written_content_issue_count;
	}

	public int getImageryIssueCount() {
		return imagery_issue_count;
	}

	public void setImageryIssueCount(int imagery_issue_count) {
		this.imagery_issue_count = imagery_issue_count;
	}

	public int getVideoIssueCount() {
		return video_issue_count;
	}

	public void setVideoIssueCount(int video_issue_count) {
		this.video_issue_count = video_issue_count;
	}

	public int getAuditIssueCount() {
		return audit_issue_count;
	}

	public void setAuditIssueCount(int audit_issue_count) {
		this.audit_issue_count = audit_issue_count;
	}

	public int getSeoIssueCount() {
		return seo_issue_count;
	}

	public void setSeoIssueCount(int seo_issue_count) {
		this.seo_issue_count = seo_issue_count;
	}

	public int getMenuIssueCount() {
		return menu_issue_count;
	}

	public void setMenuIssueCount(int menu_issue_count) {
		this.menu_issue_count = menu_issue_count;
	}

	public int getPerformanceIssueCount() {
		return performance_issue_count;
	}

	public void setPerformanceIssueCount(int performance_issue_count) {
		this.performance_issue_count = performance_issue_count;
	}

	public int getTypographyIssueCount() {
		return typography_issue_count;
	}

	public void setTypographyIssueCount(int typography_issue_count) {
		this.typography_issue_count = typography_issue_count;
	}

	public int getWhitespaceIssueCount() {
		return whitespace_issue_count;
	}

	public void setWhitespaceIssueCount(int whitespace_issue_count) {
		this.whitespace_issue_count = whitespace_issue_count;
	}

	public int getBrandingIssueCount() {
		return branding_issue_count;
	}

	public void setBrandingIssueCount(int branding_issue_count) {
		this.branding_issue_count = branding_issue_count;
	}

	public int getTotalIssues() {
		return total_issues;
	}

	public void setTotalIssues(int total_issues) {
		this.total_issues = total_issues;
	}

	public int getLinkIssueCount() {
		return link_issue_count;
	}

	public void setLinkIssueCount(int link_issue_count) {
		this.link_issue_count = link_issue_count;
	}

	public int getTextContrastIssueCount() {
		return text_contrast_issue_count;
	}

	public void setTextContrastIssueCount(int text_contrast_issue_count) {
		this.text_contrast_issue_count = text_contrast_issue_count;
	}

	public int getNonTextContrastIssueCount() {
		return non_text_issue_count;
	}

	public void setNonTextContrastIssueCount(int non_text_issue_count) {
		this.non_text_issue_count = non_text_issue_count;
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

	public void setNonTextContrastScore(double non_text_contrast_score) {
		this.non_text_contrast_score = non_text_contrast_score;
	}

	public int getImageCopyrightIssueCount() {
		return image_copyright_issue_count;
	}

	public void setImageCopyrightIssueCount(int image_copyright_issue_count) {
		this.image_copyright_issue_count = image_copyright_issue_count;
	}

	public ExecutionStatus getStatus() {
		return ExecutionStatus.create(status);
	}

	public void setStatus(ExecutionStatus status) {
		this.status = status.toString();
	}
}
