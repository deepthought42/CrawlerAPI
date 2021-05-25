package com.looksee.models;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.neo4j.ogm.annotation.Relationship;

public class AuditStats extends LookseeObject{
	
	private LocalDateTime start_time; //time that the 
	private LocalDateTime end_time;
	private long audit_record_id;
	private long pages_found;
	private long content_pages_audited;
	private long info_arch_pages_audited;
	private long aesthetic_pages_audited;
	
	@Relationship(type = "HAS")
	private Set<AuditSubcategoryStat> subcategory_stats;
	
	public AuditStats() {}
	
	public AuditStats(long audit_record_id) {
		setStartTime(LocalDateTime.now());
		setAuditRecordId(audit_record_id);
		setKey(generateKey());
	}
	
	public AuditStats(
			long audit_record_id,
			LocalDateTime start_time, 
			LocalDateTime end_time, 
			long page_count, 
			long content_pages_audited,
			long info_arch_pages_audited,
			long aesthetic_pages_audited
	) {
		setStartTime(start_time);
		setEndTime(end_time);
		setAuditRecordId(audit_record_id);
		setSubcategoryStats(new HashSet<>());
		setContentPagesAudited(content_pages_audited);
		setInfoArchPagesAudited(info_arch_pages_audited);
		setAestheticPagesAudited(aesthetic_pages_audited);
		setKey(generateKey());
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

	@Override
	public String generateKey() {
		return "auditstat"+org.apache.commons.codec.digest.DigestUtils.sha512Hex( start_time + "" + audit_record_id);
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

	public long getAestheticpagesAudited() {
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
}
