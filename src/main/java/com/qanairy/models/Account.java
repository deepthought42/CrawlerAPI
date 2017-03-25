package com.qanairy.models;

/**
 * Defines the type of package paid for, which domains are registered and which Users belong to the account
 */
public class Account {
	private String key;
	private String org_name;
	private ServicePackage service_package;
	private String payment_acct_num;
	
	public Account(){}
	
	public Account(String org_name, ServicePackage service_package, String payment_acct_num){
		this.setOrgName(org_name);
		this.setServicePackage(service_package);
		this.setPaymentAcctNum(payment_acct_num);
	}
	
	public Account(String key, String org_name, ServicePackage service_package, String payment_acct_num){
		this.setKey(key);
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

	public ServicePackage getServicePackage() {
		return service_package;
	}

	public void setServicePackage(ServicePackage service_package) {
		this.service_package = service_package;
	}

	public String getPaymentAcctNum() {
		return payment_acct_num;
	}

	public void setPaymentAcctNum(String payment_acct_num) {
		this.payment_acct_num = payment_acct_num;
	}
	
	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}
}
