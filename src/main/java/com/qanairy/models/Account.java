package com.qanairy.models;

import java.util.Iterator;
import java.util.UUID;

import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.IAccount;
import com.qanairy.persistence.IPersistable;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * Defines the type of package paid for, which domains are registered and which Users belong to the account
 */
public class Account implements IPersistable<IAccount> {
	private String key;
	private String org_name;
	private ServicePackage service_package;
	private String payment_acct_num;
	
	public Account(){}
	
	public Account(String org_name, ServicePackage service_package, String payment_acct_num){
		this.setOrgName(org_name);
		this.setServicePackage(service_package);
		this.setPaymentAcctNum(payment_acct_num);
		this.setKey(this.generateKey());
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateKey() {
		
		return this.getOrgName();
	}

	@Override
	public IAccount convertToRecord(OrientConnectionFactory connection) {
		this.setKey(this.generateKey());
		
		IAccount acct = connection.getTransaction().addVertex("class:"+IAccount.class.getCanonicalName()+","+UUID.randomUUID(), IAccount.class);
		acct.setKey(this.key);
		acct.setOrgName(this.org_name);
		acct.setServicePackage(this.service_package.convertToRecord(connection));
		acct.setPaymentAcctNum(this.payment_acct_num);

		return acct;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IAccount create(OrientConnectionFactory connection) {
		@SuppressWarnings("unchecked")
		Iterable<IAccount> accounts = (Iterable<IAccount>) DataAccessObject.findByKey(this.getKey(), connection, IAccount.class);
		Iterator<IAccount> iter = accounts.iterator();
		  
		if(iter.hasNext()){
			//figure out throwing exception because account already exists
			return iter.next();
		}
		else{
			IAccount account = this.convertToRecord(connection);
			connection.save();
			return account;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IAccount update(OrientConnectionFactory connection) {
		@SuppressWarnings("unchecked")
		Iterable<IAccount> accounts = (Iterable<IAccount>) DataAccessObject.findByKey(this.getKey(), connection, IAccount.class);
		Iterator<IAccount> iter = accounts.iterator();
		  
		IAccount acct = null;
		if(iter.hasNext()){
			acct = iter.next();
			acct.setOrgName(this.getOrgName());
			acct.setPaymentAcctNum(this.getPaymentAcctNum());
			acct.setServicePackage(this.getServicePackage().find(connection));
		}
		
		connection.save();
		
		return acct;
	}


	@Override
	public IAccount find(OrientConnectionFactory connection) {
		@SuppressWarnings("unchecked")
		Iterable<IAccount> svc_pkgs = (Iterable<IAccount>) DataAccessObject.findByKey(this.getKey(), connection, IAccount.class);
		Iterator<IAccount> iter = svc_pkgs.iterator();
		
		IAccount account = null; 
		if(iter.hasNext()){
			account = iter.next();
		}
		
		return account;
	}
	
	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}
}
