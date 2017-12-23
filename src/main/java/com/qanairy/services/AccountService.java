package com.qanairy.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.qanairy.auth.Auth0Client;
import com.qanairy.models.Account;
import com.qanairy.models.Domain;
import com.qanairy.models.dto.AccountRepository;
import com.qanairy.persistence.OrientConnectionFactory;

@Service
public class AccountService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected AccountRepository accountRepository;

    private Auth0Client auth0Client;

    @Autowired
    public AccountService(final Auth0Client auth0Client, final AccountRepository accountRepository) {
        this.auth0Client = auth0Client;
        this.accountRepository = accountRepository;
    }


    public Account create(Account account) {
    	OrientConnectionFactory connection = new OrientConnectionFactory();
        Account acct = accountRepository.create(connection, account);
        connection.close();
        return acct;
    }

    public Account get(String key) {
    	OrientConnectionFactory connection = new OrientConnectionFactory();
        Account acct = accountRepository.find(connection, key);
        connection.close();
        return acct;
    }

    public Account update(Account account) {
    	System.err.println("updating account");
    	OrientConnectionFactory connection = new OrientConnectionFactory();
        Account acct = accountRepository.update(connection, account);
        connection.close();
        return acct;
    }
    
    public Account find(String key){
    	OrientConnectionFactory conn = new OrientConnectionFactory();
    	Account acct = accountRepository.find(conn, key);
    	conn.close();
    	return acct;
    }

	public Account deleteDomain(Account account, Domain domain) {
		OrientConnectionFactory conn = new OrientConnectionFactory();
    	Account acct = accountRepository.deleteDomain(conn, account, domain);
    	conn.close();
    	return acct;
	}
}
