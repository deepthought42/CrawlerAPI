package com.qanairy.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.qanairy.models.dao.AccountDao;
import com.qanairy.models.dao.impl.AccountDaoImpl;
import com.qanairy.persistence.Account;
import com.qanairy.persistence.Domain;

/**
 * 
 */
public class AccountService {
    @SuppressWarnings("unused")
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static Account get(String key) {
    	AccountDao account_dao = new AccountDaoImpl();
    	Account acct = account_dao.find(key);
        return acct;
    }
    
    public static Account save(Account account) {
    	AccountDao account_dao = new AccountDaoImpl();
    	return account_dao.save(account);
    }
    
    public static Account find(String key){
    	AccountDao account_dao = new AccountDaoImpl();
    	return account_dao.find(key);
    }

	public static void deleteDomain(Account account, Domain domain) {
		AccountDao account_dao = new AccountDaoImpl();
    	account_dao.removeDomain(account, domain);
	}


	public static void delete(Account account) {
		AccountDao account_dao = new AccountDaoImpl();
    	account_dao.remove(account); 
   	}
}
