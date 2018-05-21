package com.qanairy.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qanairy.models.dao.DomainDao;
import com.qanairy.persistence.Domain;

@Service
public class DomainService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected DomainDao domain_dao;

    @Autowired
    public DomainService(final DomainDao domain_dao) {
        this.domain_dao = domain_dao;
    }

    public Domain save(Domain domain) {
    	return domain_dao.save(domain);
    }

    public Domain get(String key) {
    	return domain_dao.find(key);
    }
}
