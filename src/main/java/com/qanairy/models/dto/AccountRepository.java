package com.qanairy.models.dto;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.commons.collections.IteratorUtils;
import org.springframework.stereotype.Component;

import com.qanairy.models.Account;
import com.qanairy.models.Domain;
import com.qanairy.models.QanairyUser;
import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.IAccount;
import com.qanairy.persistence.IPersistable;
import com.qanairy.persistence.OrientConnectionFactory;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

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
	public IAccount convertToRecord(OrientConnectionFactory connection, Account account) {
		Account acct = find(connection, account.getKey());
		IAccount acct_record = null;  
		if(acct != null){
			acct_record = connection.getTransaction().addVertex("class:"+IAccount.class.getSimpleName()+","+UUID.randomUUID(), IAccount.class);
			acct_record.setKey(account.getKey());
			acct_record.setOrgName(account.getOrgName());
			
			acct_record.setServicePackage(account.getServicePackage());
			acct_record.setPaymentAcctNum(account.getPaymentAcctNum());

			for(QanairyUser user : account.getUsers()){
				QanairyUserRepository repo = new QanairyUserRepository();
				//repo.create(connection, user);
				acct_record.addUser(repo.convertToRecord(connection, user));
			}

		}
		return acct_record;
	}

	@Override
	public Account convertFromRecord(IAccount account) {
		List<QanairyUser> users = IteratorUtils.toList(account.getUsers().iterator());
		return new Account(account.getKey(), account.getOrgName(), account.getServicePackage(), account.getPaymentAcctNum(), users);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Account create(OrientConnectionFactory connection, Account account) {
		account.setKey(generateKey(account));

		@SuppressWarnings("unchecked")
		Iterable<IAccount> accounts = (Iterable<IAccount>) DataAccessObject.findByKey(account.getKey(), connection, IAccount.class);
		Iterator<IAccount> iter = accounts.iterator();
		  
		if(!iter.hasNext()){
			convertToRecord(connection, account);
			connection.getTransaction().commit();
		}
		return account;

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Account update(OrientConnectionFactory connection, Account account) {
		if(account.getKey() == null){
			account.setKey(generateKey(account));
		}
		@SuppressWarnings("unchecked")
		Iterable<IAccount> accounts = (Iterable<IAccount>) DataAccessObject.findByKey(account.getKey(), connection, IAccount.class);
		Iterator<IAccount> iter = accounts.iterator();
		  
		IAccount acct = null;
		if(iter.hasNext()){
			acct = iter.next();
			acct.setOrgName(account.getOrgName());
			acct.setPaymentAcctNum(account.getPaymentAcctNum());
			acct.setServicePackage(account.getServicePackage());
			
			for(Domain domain : account.getDomains()){
				DomainRepository repo = new DomainRepository();
				
				if(repo.find(connection, domain.getUrl()) == null){
					acct.addDomain(repo.convertToRecord(connection, domain));	
				}
			}
			
			connection.save();
		}
		return convertFromRecord(acct);
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
			return convertFromRecord(iter.next());
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
			accounts.add(convertFromRecord((IAccount)v));
		}
		
		return accounts;
	} 
}