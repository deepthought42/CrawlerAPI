package com.qanairy.models.dao.impl;

import java.util.List;
import java.util.NoSuchElementException;

import com.qanairy.models.dao.AccountDao;
import com.qanairy.persistence.Account;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * 
 */
public class AccountDaoImpl implements AccountDao{

	@Override
	public List<Account> getAll() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Account find(String key) {
		assert key != null;
    	OrientConnectionFactory connection = new OrientConnectionFactory();
    	
		Account account = null;
		try{
			account = connection.getTransaction().getFramedVertices("key", key, Account.class).next();
		}catch(NoSuchElementException e){
			System.err.println("could not find record");
		}
		connection.close();

		return account;
	}

	@Override
	public void save(Account account) {
		OrientConnectionFactory connection = new OrientConnectionFactory();

		account.setKey(generateKey(account));
		//Account account_record = connection.getTransaction().traverse(g -> g.V().has("key", account.getKey())).next(Account.class);
		Account account_record = null;
		try{
			account_record = connection.getTransaction().getFramedVertices("key", account.getKey(), Account.class).next();
		}catch(NoSuchElementException e){
			System.err.println("could not find record");
		}
		
		if(account_record == null){
			account_record = connection.getTransaction().addFramedVertex(Account.class);
			account_record.setKey(account.getKey());
			account_record.setCustomerToken(account.getCustomerToken());
			account_record.setSubscriptionToken(account.getSubscriptionToken());
			account_record.setOrgName(account.getOrgName());
		}
		
		account_record.setLastDomain(account.getLastDomain());		
		account_record.setDiscoveryRecords(account.getDiscoveryRecords());
		account_record.setTestRecords(account.getTestRecords());
		account_record.setOnboardedSteps(account.getOnboardedSteps());
		account_record.setUsers(account.getUsers());
		connection.save();
	}

	@Override
	public void updateSubscription(String key, String token){
		
	}
	
	@Override
	public void delete(Account account) {
		// TODO Auto-generated method stub
	}
	
	/**
	 * {@inheritDoc}
	 */
	private String generateKey(Account acct) {
		return acct.getOrgName();
	}

	
}
