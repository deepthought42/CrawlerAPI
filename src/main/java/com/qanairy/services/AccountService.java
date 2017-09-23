package com.qanairy.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.qanairy.auth.Auth0Client;
import com.qanairy.models.Account;
import com.qanairy.models.dto.AccountRepository;
import com.qanairy.persistence.OrientConnectionFactory;

@Service
public class AccountService {

    private final Logger logger = LogManager.getLogger(this.getClass());

    protected AccountRepository accountRepository;

    private Auth0Client auth0Client;

    @Autowired
    public AccountService(final Auth0Client auth0Client, final AccountRepository accountRepository) {
        this.auth0Client = auth0Client;
        this.accountRepository = accountRepository;
    }


   // @PreAuthorize("hasAuthority('qanairy')")
    public Account create(Account account) {
    	OrientConnectionFactory connection = new OrientConnectionFactory();
        return accountRepository.create(connection, account);
    }

    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('qanairy')")
    public Account get(String key) {
    	OrientConnectionFactory connection = new OrientConnectionFactory();
        return accountRepository.find(connection, key);
    }

    @PreAuthorize("hasAuthority('qanairy')")
    public Account update(Account account) {
    	OrientConnectionFactory connection = new OrientConnectionFactory();
        return accountRepository.update(connection, account);
    }
    
    @PreAuthorize("hasAuthority('qanairy')")
    public Account find(String key){
    	OrientConnectionFactory conn = new OrientConnectionFactory();
    	Account acct = accountRepository.find(conn, key);
    	conn.close();
    	return acct;
    }
}
