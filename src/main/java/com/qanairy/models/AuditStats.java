package com.qanairy.models;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.neo4j.ogm.annotation.Relationship;

public class AuditStats extends LookseeObject{
	
	private LocalDateTime start_time; //time that the 
	private LocalDateTime end_time;
	private String host;
	
	@Relationship(type = "HAS")
	private CrawlStat crawl_stats;
	
	@Relationship(type = "HAS")
	private Set<AuditSubcategoryStat> subcategory_stats;
	
	public AuditStats() {}
	
	public AuditStats(String host_url) {
		setStartTime(LocalDateTime.now());
		setHost(host_url);
		setCrawlStats(new CrawlStat(host_url));
		setKey(generateKey());
	}
	
	public AuditStats(String host_url, LocalDateTime start_time, LocalDateTime end_time, int page_count, double avg_time_per_page) {
		setStartTime(start_time);
		setEndTime(end_time);
		setCrawlStats(new CrawlStat(host_url));
		setHost(host_url);
		setSubcategoryStats(new HashSet<>());
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

	public CrawlStat getCrawlStats() {
		return crawl_stats;
	}

	public void setCrawlStats(CrawlStat crawl_stats) {
		this.crawl_stats = crawl_stats;
	}

	public Set<AuditSubcategoryStat> getSubcategoryStats() {
		return subcategory_stats;
	}

	public void setSubcategoryStats(Set<AuditSubcategoryStat> subcategory_stats) {
		this.subcategory_stats = subcategory_stats;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host_url) {
		this.host = host_url;
	}

	@Override
	public String generateKey() {
		return "auditstat"+org.apache.commons.codec.digest.DigestUtils.sha512Hex( start_time + host);
	}
}
