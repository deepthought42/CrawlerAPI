package com.qanairy.models.dao.impl;

import java.util.List;
import java.util.NoSuchElementException;
import com.qanairy.models.dao.AccountDao;
import com.qanairy.models.dao.DiscoveryRecordDao;
import com.qanairy.models.dao.DomainDao;
import com.qanairy.models.dao.TestRecordDao;
import com.qanairy.persistence.Account;
import com.qanairy.persistence.DiscoveryRecord;
import com.qanairy.persistence.Domain;
import com.qanairy.persistence.OrientConnectionFactory;
import com.qanairy.persistence.TestRecord;

/**
 * 
 */
public class AccountDaoImpl implements AccountDao{

	@Override
	public Account find(String key) {
		assert key != null;
    	OrientConnectionFactory connection = new OrientConnectionFactory();
    	
		Account account = null;
		try{
			//account = connection.getTransaction().traverse(g -> g.V().has("key", key)).next(Account.class);			
			account = connection.getTransaction().getFramedVertices("key", key, Account.class).next();
		}catch(NoSuchElementException e){
			System.err.println("could not find account record");
		}
		connection.close();
		return account;
	}

	@Override
	public Account save(Account account) {
		OrientConnectionFactory connection = new OrientConnectionFactory();

		account.setKey(generateKey(account));
		//Account account_record = connection.getTransaction().traverse(g -> g.V().has("key", account.getKey())).next(Account.class);
		Account account_record = null;
		try{
			account_record = connection.getTransaction().getFramedVertices("key", account.getKey(), Account.class).next();
		}catch(NoSuchElementException e){
			System.err.println("could not find account record");
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
		account_record.setOnboardedSteps(account.getOnboardedSteps());

		DiscoveryRecordDao discovery_record_dao = new DiscoveryRecordDaoImpl();
		for(DiscoveryRecord record : account.getDiscoveryRecords()){

			boolean exists = false;
			for(DiscoveryRecord record2 : account_record.getDiscoveryRecords()){
				if(record2.getKey().equals(record.getKey())){
					exists = true;
				}
			}
			
			if(!exists){
				account_record.addDiscoveryRecord(discovery_record_dao.save(record));
			}
		}
		
		TestRecordDao test_record_dao = new TestRecordDaoImpl();
		for(TestRecord record : account.getTestRecords()){

			boolean exists = false;
			for(TestRecord record2 : account_record.getTestRecords()){
				if(record2.getKey().equals(record.getKey())){
					exists = true;
				}
			}
			
			if(!exists){
				account_record.addTestRecord(test_record_dao.save(record));
			}
		}
		
		DomainDao domain_dao = new DomainDaoImpl();
		for(Domain domain : account.getDomains()){

			boolean exists = false;
			for(Domain domain2 : account_record.getDomains()){
				if(domain2.getKey().equals(domain.getKey())){
					exists = true;
				}
			}
			
			if(!exists){
				account_record.addDomain(domain_dao.save(domain));
			}
		}
		
		connection.close();
		return account_record;
	}

	@Override
	public void updateSubscription(String key, String token){
		
	}
	
	@Override
	public void remove(Account account) {
		Account acct = find(account.getKey());
		acct.remove();
	}
	
	/**
	 * {@inheritDoc}
	 */
	private String generateKey(Account acct) {
		return acct.getOrgName();
	}

	@Override
	public void updateCustomerToken(String key, String token) {
		Account account = find(key);
		account.setCustomerToken(token);
	}

	@Override
	public List<DiscoveryRecord> getAllDiscoveryRecords() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<DiscoveryRecord> getDiscoveryRecordsByMonth(int month) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<TestRecord> getAllTestRecords() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<TestRecord> getTestRecordsByMonth(int month) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeDomain(Account account, Domain domain) {
		Account acct = find(account.getKey());
		acct.getDomains().remove(domain);
	}
}
