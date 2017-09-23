package com.qanairy.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.qanairy.auth.Auth0Client;
import com.qanairy.models.Domain;
import com.qanairy.models.dto.DomainRepository;
import com.qanairy.persistence.OrientConnectionFactory;

@Service
public class DomainService {

    private final Logger logger = LogManager.getLogger(this.getClass());

    protected DomainRepository domainRepository;

    private Auth0Client auth0Client;

    @Autowired
    public DomainService(final Auth0Client auth0Client, final DomainRepository domainRepository) {
        this.auth0Client = auth0Client;
        this.domainRepository = domainRepository;
    }


    //@PreAuthorize("hasAuthority('ROLE_USER')")
    public Domain create(Domain domain) {
    	OrientConnectionFactory connection = new OrientConnectionFactory();
    	Domain domain_record = domainRepository.create(connection, domain);
    	connection.close();
        return domain_record;
    }

    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN')")
    public Domain get(String key) {
    	OrientConnectionFactory connection = new OrientConnectionFactory();
    	Domain domain = domainRepository.find(connection, key);
    	connection.close();
        return domain;
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Domain update(Domain domain) {
    	OrientConnectionFactory connection = new OrientConnectionFactory();
    	Domain domain_record = domainRepository.update(connection, domain);
    	connection.close();
        return domain_record;
    }
}
