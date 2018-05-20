package com.qanairy.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.qanairy.models.dao.AccountDao;
import com.qanairy.models.dao.impl.AccountDaoImpl;
import com.qanairy.persistence.Account;
import com.qanairy.persistence.Domain;
import com.qanairy.persistence.OrientConnectionFactory;

@Service
public class AccountService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected AccountRepository accountRepository;

    @Autowired
    public AccountService(final AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public static Account get(String key) {
    	AccountDao account_dao = new AccountDaoImpl();
    	Account acct = account_dao.find(connection, key);
        return acct;
    }
    
    public static Account save(Account account) {
    	AccountDao account_dao = new AccountDaoImpl();
    	Account acct = account_dao.save(account);
        return acct;
    }
    
    public static Account find(String key){
    	AccountDao account_dao = new AccountDaoImpl();
    	Account acct = account_dao.find(key);
    	return acct;
    }

	public static Account deleteDomain(Account account, Domain domain) {
		AccountDao account_dao = new AccountDaoImpl();
    	Account acct = account_dao.removeDomain(account, domain);
    	return acct;
	}


	public static void delete(Account account) {
		AccountDao account_dao = new AccountDaoImpl();
    	Account acct = account_dao.delete(conn, account.getKey()); 
   	}
}
