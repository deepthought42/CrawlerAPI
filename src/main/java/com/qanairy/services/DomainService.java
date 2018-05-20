package com.qanairy.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qanairy.models.dao.DomainDao;
import com.qanairy.persistence.Domain;
import com.qanairy.persistence.OrientConnectionFactory;

@Service
public class DomainService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected DomainDao domain_dao;

    @Autowired
    public DomainService(final DomainDao domain_dao) {
        this.domain_dao = domain_dao;
    }

    public Domain save(Domain domain) {
    	OrientConnectionFactory connection = new OrientConnectionFactory();
    	Domain domain_record = domain_dao.save(connection, domain);
    	connection.close();
        return domain_dao.load(domain_record);
    }

    public Domain get(String key) {
    	OrientConnectionFactory connection = new OrientConnectionFactory();
    	Domain domain = domain_dao.find(key);
    	connection.close();
        return domain;
    }
}
