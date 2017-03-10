package com.qanairy.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.qanairy.auth.Auth0Client;
import com.qanairy.models.Account;
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


    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Account create(Account account) {
        return accountRepository.create(new OrientConnectionFactory(), account);
    }

    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN')")
    public Account get(String key) {
        return accountRepository.convertFromRecord(accountRepository.find(new OrientConnectionFactory(), key));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Account update(Account account) {
        return accountRepository.update(new OrientConnectionFactory(), account);
    }
}
