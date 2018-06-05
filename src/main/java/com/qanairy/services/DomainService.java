package com.qanairy.services;

import org.springframework.stereotype.Service;
import com.qanairy.models.dao.DomainDao;
import com.qanairy.models.dao.impl.DomainDaoImpl;
import com.qanairy.persistence.Domain;

@Service
public class DomainService {

    public static Domain save(Domain domain) {
    	DomainDao domain_dao = new DomainDaoImpl();
    	return domain_dao.save(domain);
    }

    public static Domain get(String key) {
    	DomainDao domain_dao = new DomainDaoImpl();
    	return domain_dao.find(key);
    }
}
