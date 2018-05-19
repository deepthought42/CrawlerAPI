package com.qanairy.models.dao.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import com.qanairy.models.TestUser;
import com.qanairy.models.dao.DomainDao;
import com.qanairy.models.dto.TestUserRepository;
import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.Domain;
import com.qanairy.persistence.ITestUser;
import com.qanairy.persistence.OrientConnectionFactory;

public class DomainDaoImpl implements DomainDao{

	@Override
	public Domain save(Domain domain) {
		domain.setKey(generateKey(domain));
		
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
		domain_record.setDiscoveryTestCount(domain.getDiscoveryTestCount());
		
		TestUserRepository test_user_repo = new TestUserRepository();
		List<TestUser> test_users = new ArrayList<TestUser>();
		for(TestUser test_user : domain.getTestUsers()){
			test_users.add(test_user_repo.save(test_user));
		}
		domain_record.setTestUsers(test_users);
		connection.close();
		return domain_record;
	}

	@Override
	public Domain find(String key) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public String generateKey(Domain domain) {
		return domain.getUrl().toString();
	}
}
