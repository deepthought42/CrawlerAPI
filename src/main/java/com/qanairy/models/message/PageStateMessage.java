package com.qanairy.models.message;

import java.util.Map;

import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.PageState;

public class PageStateMessage {

	private String account_key;
	private PageState page_state;
	private DiscoveryRecord discovery;
	private Map<String, Object> options;

	public PageStateMessage(String account_key, PageState page_state, DiscoveryRecord discovery, Map<String, Object> options){
		this.account_key = account_key;
		this.discovery = discovery;
		this.page_state = page_state;
		this.options = options;
	}

	public PageState getPageState() {
		return page_state;
	}

	public DiscoveryRecord getDiscovery() {
		return discovery;
	}
	
	public String getAccountKey(){
		return account_key;
	}
	
	public Map<String, Object> getOptions(){
		return options;
	}
}
