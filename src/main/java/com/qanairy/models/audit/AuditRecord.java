package com.qanairy.models.audit;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.neo4j.ogm.annotation.Relationship;

import com.qanairy.models.CrawlStat;
import com.qanairy.models.LookseeObject;

/**
 * Record detailing an set of {@link Audit audits}.
 */
public class AuditRecord extends LookseeObject {
	
	@Relationship(type = "HAS")
	private Set<Audit> audits;

	@Relationship(type = "HAS")
	private CrawlStat crawl_stats = null;
	
	public AuditRecord() {
		audits = new HashSet<>();
		setKey(generateKey());
	}

	public String generateKey() {
		return "auditrecord:"+UUID.randomUUID().toString()+org.apache.commons.codec.digest.DigestUtils.sha256Hex(System.currentTimeMillis() + "");
	}

	public Set<Audit> getAudits() {
		return audits;
	}

	public void setAudits(Set<Audit> audits) {
		this.audits = audits;
	}

	public void addAudit(Audit audit) {
		this.audits.add( audit );
	}
	
	public void addAudits(Set<Audit> audits) {
		this.audits.addAll( audits );
	}

	public void setCrawlStats(CrawlStat crawl_stats) {
		this.crawl_stats = crawl_stats;
	}
	
	public CrawlStat getCrawlStats() {
		return this.crawl_stats;
	}
}
