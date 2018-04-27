package com.qanairy.models;

public class AccountUsage {
	int discovery_limit;
	int discoveries_used;
	int test_limit;
	int tests_used;
	
	public AccountUsage(int discovery_limit, int discoveries_used, int test_limit, int tests_used){
		this.discovery_limit = discovery_limit;
		this.discoveries_used = discoveries_used;
		this.test_limit = test_limit;
		this.tests_used = tests_used;
	}
}
