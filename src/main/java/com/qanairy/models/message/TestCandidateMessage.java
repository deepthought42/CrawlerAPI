package com.qanairy.models.message;

import java.util.List;
import java.util.Map;

import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;

/**
 * 
 * 
 */
public class TestCandidateMessage {

	private String account_key;
	private Map<String, Object> options;
	private List<String> keys;
	private List<PathObject> path_objects;
	private DiscoveryRecord discovery;
	private PageState result_page;
	
	public TestCandidateMessage(List<String> keys, List<PathObject> path_objects, DiscoveryRecord discovery, String account_key, PageState result_page, Map<String, Object> options){
		this.discovery = discovery;
		this.keys = keys;
		this.path_objects = path_objects;
		this.result_page = result_page;
		this.account_key = account_key;
		this.options = options;
	}

	public List<String> getKeys() {
		return keys;
	}

	public List<PathObject> getPathObjects() {
		return path_objects;
	}

	public DiscoveryRecord getDiscovery() {
		return discovery;
	}

	public String getAccountKey() {
		return account_key;
	}

	public Map<String, Object> getOptions() {
		return options;
	}
	
	public PageState getResultPage() {
		return result_page;
	}
}
