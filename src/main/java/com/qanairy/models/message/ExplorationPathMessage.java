package com.qanairy.models.message;

import java.util.List;
import java.util.Map;

import com.qanairy.models.Account;
import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.ExploratoryPath;


/**
 * Message class containing {@link Discovery}, {@link Account}, and a list of {@link ExploratoryPath}s
 */
public class ExplorationPathMessage{
	
	private List<ExploratoryPath> paths;
	private DiscoveryRecord discovery;
	private Account account;
	private Map<String, Object> options;
	
	public ExplorationPathMessage(List<ExploratoryPath> paths, DiscoveryRecord discovery, Account account, Map<String, Object> options){
		this.paths = paths;
		this.discovery = discovery;
		this.account = account;
		this.options = options;
	}

	public List<ExploratoryPath> getPaths() {
		return paths;
	}

	public DiscoveryRecord getDiscovery() {
		return discovery;
	}

	public Account getAccount() {
		return account;
	}

	public Map<String, Object> getOptions() {
		return options;
	}
}
