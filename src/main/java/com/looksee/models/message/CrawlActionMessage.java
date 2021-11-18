package com.looksee.models.message;

import java.net.URL;

import com.looksee.models.audit.AuditRecord;
import com.looksee.models.enums.CrawlAction;

/**
 * Message for different audit actions to perform and which audit types to perform them for.
 * 
 */
public class CrawlActionMessage extends Message{
	private CrawlAction action;
	private AuditRecord audit_record;
	private boolean is_individual;
	private URL url;
	
	public CrawlActionMessage(CrawlAction action, 
							  long domain_id, 
							  long account_id, 
							  AuditRecord record, 
							  boolean is_individual,
							  URL url){
		super(domain_id, account_id, record.getId());
		
		setAction(action);
		setIsIndividual(is_individual);
		setUrl(url);
		setAuditRecord(record);
	}
	
	public CrawlAction getAction() {
		return action;
	}
	
	private void setAction(CrawlAction action) {
		this.action = action;
	}

	public AuditRecord getAuditRecord() {
		return audit_record;
	}

	public void setAuditRecord(AuditRecord audit_record) {
		this.audit_record = audit_record;
	}

	public boolean isIndividual() {
		return is_individual;
	}

	public void setIsIndividual(boolean is_individual) {
		this.is_individual = is_individual;
	}

	public URL getUrl() {
		return url;
	}

	public void setUrl(URL url) {
		this.url = url;
	}	
}
