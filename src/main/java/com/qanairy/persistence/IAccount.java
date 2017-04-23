package com.qanairy.persistence;

import com.qanairy.models.QanairyUser;
import com.tinkerpop.blueprints.Direction;
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
	
	@Property("service_package")
	public String getServicePackage();

	@Property("service_package")
	public void setServicePackage(String service_package);

	@Property("payment_acct_num")
	public String getPaymentAcctNum();

	@Property("payment_acct_num")
	public void setPaymentAcctNum(String payment_acct_num);
	
	@Adjacency(direction=Direction.OUT, label="has_user")
	public Iterable<IQanairyUser> getUsers();
	
	@Adjacency(direction=Direction.OUT, label="has_user")
	public void addUser(IQanairyUser user);
}
