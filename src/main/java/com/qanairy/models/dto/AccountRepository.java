package com.qanairy.models.dto;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.commons.collections.IteratorUtils;
import org.springframework.stereotype.Component;

import com.qanairy.models.Account;
import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.Domain;
import com.qanairy.models.QanairyUser;
import com.qanairy.models.TestRecord;
import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.IAccount;
import com.qanairy.persistence.IDiscoveryRecord;
import com.qanairy.persistence.IDomain;
import com.qanairy.persistence.IPersistable;
import com.qanairy.persistence.ITestRecord;
import com.qanairy.persistence.OrientConnectionFactory;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;
import com.tinkerpop.frames.VertexFrame;

/**
 * 
 */
@Component
public class AccountRepository implements IPersistable<Account, IAccount> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateKey(Account acct) {
		return acct.getOrgName();
	}

	@Override
	public IAccount save(OrientConnectionFactory connection, Account account) {
		account.setKey(generateKey(account));
		@SuppressWarnings("unchecked")
		Iterable<IAccount> accounts = (Iterable<IAccount>) DataAccessObject.findByKey(account.getKey(), connection, IAccount.class);
		Iterator<IAccount> iter = accounts.iterator();
		IAccount acct_record = null;  

		if(!iter.hasNext()){
			acct_record = connection.getTransaction().addVertex("class:"+IAccount.class.getSimpleName()+","+UUID.randomUUID(), IAccount.class);
			acct_record.setKey(account.getKey());
			acct_record.setServicePackage(account.getServicePackage());
			acct_record.setCustomerToken(account.getCustomerToken());
			acct_record.setSubscriptionToken(account.getSubscriptionToken());
			acct_record.setOrgName(account.getOrgName());
		}
		else{
			acct_record = iter.next();
			account.setKey(acct_record.getKey());
			account.setServicePackage(acct_record.getServicePackage());
			account.setCustomerToken(acct_record.getCustomerToken());
			account.setSubscriptionToken(acct_record.getSubscriptionToken());
			account.setOrgName(acct_record.getOrgName());
		}
		
		acct_record.setLastDomain(account.getLastDomain());
		
		List<IDiscoveryRecord> discovery_records = new ArrayList<IDiscoveryRecord>();
		for(DiscoveryRecord record : account.getDiscoveryRecords()){
			DiscoveryRecordRepository repo = new DiscoveryRecordRepository();
			//repo.create(connection, user);
			discovery_records.add(repo.save(connection, record));
		}
		
		acct_record.setDiscoveryRecords(discovery_records);
		
		List<ITestRecord> test_records = new ArrayList<ITestRecord>();
		for(TestRecord record : account.getTestRecords()){
			TestRecordRepository repo = new TestRecordRepository();
			test_records.add(repo.save(connection, record));
		}
		
		acct_record.setTestRecords(test_records);
		acct_record.setOnboardedSteps(account.getOnboardedSteps());
		/*for(QanairyUser user : account.getUsers()){
			QanairyUserRepository repo = new QanairyUserRepository();
			//repo.create(connection, user);
			acct_record.addUser(repo.save(connection, user));
		}
		*/
		
		return acct_record;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Account load(IAccount account) {
		List<IDomain> domain_records = IteratorUtils.toList(account.getDomains().iterator());
		
		List<Domain> domains = new ArrayList<Domain>();
		DomainRepository domain_repo = new DomainRepository();
		for(IDomain domain : domain_records){
			domains.add(domain_repo.load(domain));
		}
		
		Iterator<IDiscoveryRecord> discovery_records = account.getDiscoveryRecords().iterator();
		List<DiscoveryRecord> discovery_record_list = new ArrayList<DiscoveryRecord>();
		while(discovery_records.hasNext()){
			DiscoveryRecordRepository repo = new DiscoveryRecordRepository();
			discovery_record_list.add(repo.load(discovery_records.next()));
		}
		
		Iterator<ITestRecord> test_records = account.getTestRecords().iterator();
		List<TestRecord> test_record_list = new ArrayList<TestRecord>();
		while(test_records.hasNext()){
			TestRecordRepository repo = new TestRecordRepository();
			test_record_list.add(repo.load(test_records.next()));
		}
		
		//List<IQanairyUser> user_records = IteratorUtils.toList(account.getUsers().iterator());
		/*List<QanairyUser> users = new ArrayList<QanairyUser>();
		QanairyUserRepository user_repo = new QanairyUserRepository();
		for(IQanairyUser user : user_records){
			users.add(user_repo.load(user));
		}
		*/
		
		return new Account(account.getKey(), account.getOrgName(), account.getServicePackage(), account.getCustomerToken(), account.getSubscriptionToken(),
							new ArrayList<QanairyUser>(), domains, account.getLastDomain(), discovery_record_list, test_record_list, account.getOnboardedSteps());
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public Account find(OrientConnectionFactory connection, String key) {
		@SuppressWarnings("unchecked")
		Iterable<IAccount> svc_pkgs = (Iterable<IAccount>) DataAccessObject.findByKey(key, connection, IAccount.class);
		Iterator<IAccount> iter = svc_pkgs.iterator();
		
		if(iter.hasNext()){
			return load(iter.next());
		}
		
		return null;
	}

	@Override
	public List<Account> findAll(OrientConnectionFactory conn) {
		@SuppressWarnings("unchecked")
		Iterator<OrientVertex> iter = ((Iterable<OrientVertex>) DataAccessObject.findAll(conn, IAccount.class)).iterator();
		
		List<Account> accounts = new ArrayList<Account>();
		while(iter.hasNext()){
			OrientVertex v = iter.next();
			accounts.add(load((IAccount)v));
		}
		
		return accounts;
	}

	public Account deleteDomain(OrientConnectionFactory conn, Account acct, Domain domain) {
		DomainRepository domain_repo = new DomainRepository();
		IDomain idomain = domain_repo.find(domain.getKey());
		
		@SuppressWarnings("unchecked")
		Iterable<IAccount> svc_pkgs = (Iterable<IAccount>) DataAccessObject.findByKey(acct.getKey(), conn, IAccount.class);
		Iterator<IAccount> iter = svc_pkgs.iterator();
		IAccount iacct = null;
		if(iter.hasNext()){
			iacct = iter.next();
			iacct.removeDomain(idomain);
		}
		return load(iacct);
	}

	public void delete(OrientConnectionFactory connection, String key) {
		@SuppressWarnings("unchecked")
		Iterable<IAccount> svc_pkgs = (Iterable<IAccount>) DataAccessObject.findByKey(key, connection, IAccount.class);
		Iterator<IAccount> iter = svc_pkgs.iterator();
		
		if(iter.hasNext()){
			IAccount account_vertex = iter.next();
			((VertexFrame)account_vertex).asVertex().remove();
			//connection.getTransaction().removeVertex(account_vertex);
			//account_vertex.remove();
		}
	} 
}