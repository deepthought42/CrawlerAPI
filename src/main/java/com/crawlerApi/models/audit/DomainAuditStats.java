package com.crawlerApi.models.audit;

import com.crawlerApi.models.enums.ExecutionStatus;

import lombok.Getter;
import lombok.Setter;

public class DomainAuditStats extends AuditStats{
	@Getter
	@Setter
	private long id;
	
	@Getter
	@Setter
	private int journeysExplored;

	@Getter
	@Setter
	private int journeysTotal;

	@Getter
	@Setter
	private int pageCount;
	
	@Setter
	@Getter
	private double accessibilityScore;

	@Setter
	@Getter
	private double contentScore;

	@Getter
	@Setter
	private double writtenContentScore;

	@Getter
	@Setter
	private double imageryScore;
	
	@Getter
	@Setter
	private double infoArchitectureScore;

	@Getter
	@Setter
	private double seoScore;

	@Getter
	@Setter
	private double linkScore;
	
	@Getter
	@Setter
	private double aestheticScore;

	@Getter
	@Setter
	private double textContrastScore;

	@Getter
	@Setter
	private double nonTextContrastScore;

	@Getter
	@Setter
	private ExecutionStatus status;
	
	public DomainAuditStats() {}
	
	public DomainAuditStats(long audit_record_id) {
		setId(audit_record_id);
	}
	
	public DomainAuditStats(
			long audit_record_id,
			int journeys_explored,
			int journeys_total,
			double accessibility_score,
			double content_score,
			double written_content_score,
			double imagery_score,
			double info_arch_score,
			double seo_score,
			double aesthetic_score,
			double text_contrast_score,
			double non_text_contrast_score,
			ExecutionStatus status,
			double link_score
	) {
		setId(audit_record_id);
		setJourneysExplored(journeys_explored);
		setJourneysTotal(journeys_total);
		setAccessibilityScore(accessibility_score);
		setContentScore(content_score);
		setWrittenContentScore(written_content_score);
		setImageryScore(imagery_score);
		setInfoArchitectureScore(info_arch_score);
		setSeoScore(seo_score);
		setAestheticScore(aesthetic_score);
		setTextContrastScore(text_contrast_score);
		setNonTextContrastScore(non_text_contrast_score);
		setLinkScore(link_score);
		setStatus(status);
	}
}
