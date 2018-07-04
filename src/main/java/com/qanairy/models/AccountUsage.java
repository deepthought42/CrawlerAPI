package com.qanairy.models;

import org.neo4j.ogm.annotation.Transient;

/**
 * 
 */
@Transient
public class AccountUsage {
	private int discovery_limit;
	private int discoveries_used;
	private int test_limit;
	private int tests_used;
	
	public AccountUsage(int discovery_limit, int discoveries_used, int test_limit, int tests_used){
		this.setDiscoveryLimit(discovery_limit);
		this.setDiscoveriesUsed(discoveries_used);
		this.setTestLimit(test_limit);
		this.setTestsUsed(tests_used);
	}

	public AccountUsage(int discoveries_used, int tests_used){
		this.setDiscoveryLimit(discovery_limit);
		this.setDiscoveriesUsed(discoveries_used);
		this.setTestLimit(test_limit);
		this.setTestsUsed(tests_used);
	}
	
	public int getDiscoveryLimit() {
		return discovery_limit;
	}

	public void setDiscoveryLimit(int discovery_limit) {
		this.discovery_limit = discovery_limit;
	}

	public int getDiscoveriesUsed() {
		return discoveries_used;
	}

	public void setDiscoveriesUsed(int discoveries_used) {
		this.discoveries_used = discoveries_used;
	}

	public int getTestLimit() {
		return test_limit;
	}

	public void setTestLimit(int test_limit) {
		this.test_limit = test_limit;
	}

	public int getTestsUsed() {
		return tests_used;
	}

	public void setTestsUsed(int tests_used) {
		this.tests_used = tests_used;
	}
}
