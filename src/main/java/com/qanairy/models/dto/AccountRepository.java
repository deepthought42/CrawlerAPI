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
import com.qanairy.persistence.IDomain;
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
		account.setKey(generateKey(account));
		Iterable<IAccount> svc_pkgs = (Iterable<IAccount>) DataAccessObject.findByKey(account.getKey(), connection, IAccount.class);
		Iterator<IAccount> iter = svc_pkgs.iterator();
		IAccount acct_record = null;  

		if(!iter.hasNext()){
			acct_record = connection.getTransaction().addVertex("class:"+IAccount.class.getSimpleName()+","+UUID.randomUUID(), IAccount.class);
			acct_record.setKey(account.getKey());
			
			acct_record.setServicePackage(account.getServicePackage());
			acct_record.setPaymentAcctNum(account.getPaymentAcctNum());
		}
		else{
			acct_record = iter.next();
		}
		
		acct_record.setOrgName(account.getOrgName());

		for(QanairyUser user : account.getUsers()){
			QanairyUserRepository repo = new QanairyUserRepository();
			//repo.create(connection, user);
			acct_record.addUser(repo.convertToRecord(connection, user));
		}
		
		return acct_record;
	}

	@Override
	public Account convertFromRecord(IAccount account) {
		List<QanairyUser> users = IteratorUtils.toList(account.getUsers().iterator());
		List<IDomain> domain_records = IteratorUtils.toList(account.getDomains().iterator());
		
		List<Domain> domains = new ArrayList<Domain>();
		DomainRepository domain_repo = new DomainRepository();
		int idx=0;
		for(IDomain domain : domain_records){
			domains.add(domain_repo.convertFromRecord(domain));
		}
		return new Account(account.getKey(), account.getOrgName(), account.getServicePackage(), account.getPaymentAcctNum(), users, domains);
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
			connection.save();
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
				
				Domain domain_record = repo.find(connection, domain.getUrl());
				if(domain_record == null){
					acct.addDomain(repo.convertToRecord(connection, domain));	
				}
				else{
					//check if domain is part of account before adding it to the account
					Iterator<IDomain> domain_iter = acct.getDomains().iterator();
					boolean domain_account_linked = false;
					while(domain_iter.hasNext()){
						IDomain idomain = domain_iter.next();
						if(idomain.getUrl().equals(domain.getUrl())){
							domain_account_linked = true;
						}
					}
					
					if(!domain_account_linked){
						acct.addDomain(repo.convertToRecord(connection, domain_record));
					}
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