package com.qanairy.models.dao.impl;

import com.qanairy.models.dao.DomainDao;
import com.qanairy.models.dao.TestUserDao;
import com.qanairy.persistence.Domain;
import com.qanairy.persistence.OrientConnectionFactory;
import com.qanairy.persistence.TestUser;

public class DomainDaoImpl implements DomainDao{

	@Override
	public Domain save(Domain domain) {
		assert domain != null;
		OrientConnectionFactory connection = new OrientConnectionFactory();
		Domain domain_record = find(domain.getKey());

		if(domain_record == null){
			domain_record = connection.getTransaction().addFramedVertex(Domain.class);
			domain_record.setKey(domain.getKey());
			domain_record.setUrl(domain.getUrl());
		}

		domain_record.setLogoUrl(domain.getLogoUrl());
		domain_record.setProtocol(domain.getProtocol());
		domain_record.setDiscoveryBrowserName(domain.getDiscoveryBrowserName());
		domain_record.setTestCount(domain.getTestCount());
		
		TestUserDao test_user_dao = new TestUserDaoImpl();
		for(TestUser test_user : domain.getTestUsers()){
			domain_record.addTestUser(test_user_dao.save(test_user));
		}

		connection.close();
		return domain_record;
	}

	@Override
	public Domain find(String key) {
		// TODO Auto-generated method stub
		return null;
	}
}
