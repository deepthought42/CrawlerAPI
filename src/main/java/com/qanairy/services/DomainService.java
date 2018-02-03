package com.qanairy.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.qanairy.models.Domain;
import com.qanairy.models.dto.DomainRepository;
import com.qanairy.persistence.OrientConnectionFactory;

@Service
public class DomainService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected DomainRepository domainRepository;

//    private Auth0Client auth0Client;

    @Autowired
    public DomainService(final DomainRepository domainRepository) {
        //this.auth0Client = auth0Client;
        this.domainRepository = domainRepository;
    }


    public Domain create(Domain domain) {
    	OrientConnectionFactory connection = new OrientConnectionFactory();
    	Domain domain_record = domainRepository.create(connection, domain);
    	connection.close();
        return domain_record;
    }

    public Domain get(String key) {
    	OrientConnectionFactory connection = new OrientConnectionFactory();
    	Domain domain = domainRepository.find(connection, key);
    	connection.close();
        return domain;
    }

    public Domain update(Domain domain) {
    	OrientConnectionFactory connection = new OrientConnectionFactory();
    	Domain domain_record = domainRepository.update(connection, domain);
    	connection.close();
        return domain_record;
    }
}
