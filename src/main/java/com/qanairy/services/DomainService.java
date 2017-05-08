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
        return domainRepository.create(new OrientConnectionFactory(), domain);
    }

    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN')")
    public Domain get(String key) {
        return domainRepository.find(new OrientConnectionFactory(), key);
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Domain update(Domain domain) {
        return domainRepository.update(new OrientConnectionFactory(), domain);
    }
}
