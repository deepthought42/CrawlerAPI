package com.qanairy.models.message;

import com.qanairy.models.Domain;
import com.qanairy.models.enums.CrawlAction;

/**
 * Message for different audit actions to perform and which audit types to perform them for.
 * 
 */
public class CrawlActionMessage extends Message{
	private CrawlAction action;
	
	public CrawlActionMessage(CrawlAction action, Domain domain, String account_id){
		super(domain.getHost(), account_id);
		setAction(action);
		setDomain(domain);
	}
	
	public CrawlAction getAction() {
		return action;
	}
	
	private void setAction(CrawlAction action) {
		this.action = action;
	}	
}
