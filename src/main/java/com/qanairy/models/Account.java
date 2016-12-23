package com.qanairy.models;

import java.util.List;

/**
 *
 */
public class Account {
	private String org_name;
	private String service_package;
	private String payment_acct_num;
	private List<Domain> registered_domains;
	
	public Account(){}
	
	public Account(String org_name, String service_package, String payment_acct_num){
		this.setOrgName(org_name);
		this.setServicePackage(service_package);
		this.setPaymentAcctNum(payment_acct_num);
	}

	public String getOrgName() {
		return org_name;
	}

	public void setOrgName(String org_name) {
		this.org_name = org_name;
	}

	public String getServicePackage() {
		return service_package;
	}

	public void setServicePackage(String service_package) {
		this.service_package = service_package;
	}

	public String getPaymentAcctNum() {
		return payment_acct_num;
	}

	public void setPaymentAcctNum(String payment_acct_num) {
		this.payment_acct_num = payment_acct_num;
	}

	public List<Domain> getRegisteredDomains() {
		return registered_domains;
	}

	public void setRegisteredDomains(List<Domain> registered_domains) {
		this.registered_domains = registered_domains;
	}
}
