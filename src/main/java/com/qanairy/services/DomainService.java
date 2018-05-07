package com.qanairy.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.qanairy.models.Domain;
import com.qanairy.models.dto.DomainRepository;
import com.qanairy.persistence.IDomain;
import com.qanairy.persistence.OrientConnectionFactory;

@Service
public class DomainService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected DomainRepository domainRepository;

    @Autowired
    public DomainService(final DomainRepository domainRepository) {
        this.domainRepository = domainRepository;
    }

    public Domain save(Domain domain) {
    	OrientConnectionFactory connection = new OrientConnectionFactory();
    	IDomain domain_record = domainRepository.save(connection, domain);
    	connection.close();
        return domainRepository.load(domain_record);
    }

    public Domain get(String key) {
    	OrientConnectionFactory connection = new OrientConnectionFactory();
    	Domain domain = domainRepository.find(connection, key);
    	connection.close();
        return domain;
    }
}
