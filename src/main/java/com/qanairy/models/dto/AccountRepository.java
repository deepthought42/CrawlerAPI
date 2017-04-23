package com.qanairy.models.dto;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.commons.collections.IteratorUtils;
import org.springframework.stereotype.Component;

import com.beust.jcommander.internal.Lists;
import com.qanairy.models.Account;
import com.qanairy.models.QanairyUser;
import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.IAccount;
import com.qanairy.persistence.IPersistable;
import com.qanairy.persistence.OrientConnectionFactory;

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
		IAccount acct = connection.getTransaction().addVertex("class:"+IAccount.class.getSimpleName()+","+UUID.randomUUID(), IAccount.class);
		acct.setKey(account.getKey());
		acct.setOrgName(account.getOrgName());
		
		acct.setServicePackage(account.getServicePackage());
		acct.setPaymentAcctNum(account.getPaymentAcctNum());

		for(QanairyUser user : account.getUsers()){
			QanairyUserRepository repo = new QanairyUserRepository();
			//repo.create(connection, user);
			acct.addUser(repo.convertToRecord(connection, user));
		}
		return acct;
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
		@SuppressWarnings("unchecked")
		Iterable<IAccount> accounts = (Iterable<IAccount>) DataAccessObject.findByKey(account.getKey(), connection, IAccount.class);
		Iterator<IAccount> iter = accounts.iterator();
		  
		IAccount acct = null;
		if(iter.hasNext()){
			acct = iter.next();
			acct.setOrgName(account.getOrgName());
			acct.setPaymentAcctNum(account.getPaymentAcctNum());
			ServicePackageRepository svc_pkg_record = new ServicePackageRepository();
			
			
			//acct.setServicePackage(svc_pkg_record.convertToRecord(connection, account.getServicePackage()));
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
	public List<Account> findAll(OrientConnectionFactory connection) {
		// TODO Auto-generated method stub
		return null;
	} 
}