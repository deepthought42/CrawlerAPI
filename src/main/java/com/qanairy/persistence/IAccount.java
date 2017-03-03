package com.qanairy.persistence;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;

public interface IAccount {
	@Property("key")
	public String getKey();
	
	@Property("key")
	public void setKey(String key);
	
	@Property("org_name")
	public String getOrgName();
	
	@Property("org_name")
	public void setOrgName(String name);
	
	@Adjacency(label="has")
	public IServicePackage getServicePackage();

	@Adjacency(label="has")
	public void setServicePackage(IServicePackage service_package);

	@Property("payment_acct_num")
	public String getPaymentAcctNum();

	@Property("payment_acct_num")
	public void setPaymentAcctNum(String payment_acct_num);
}
