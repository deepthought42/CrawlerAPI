package com.qanairy.models.dao.impl;

import java.util.NoSuchElementException;

import com.qanairy.models.dao.DomainDao;
import com.qanairy.models.dao.PageStateDao;
import com.qanairy.models.dao.TestUserDao;
import com.qanairy.persistence.Domain;
import com.qanairy.persistence.OrientConnectionFactory;
import com.qanairy.persistence.PageState;
import com.qanairy.persistence.TestUser;

/**
 * 
 */
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
			//check if page state exists for domain already
			boolean already_connected = false;

			for(TestUser test_user_record : domain_record.getTestUsers()){
				if(test_user.getKey().equals(test_user_record.getKey())){
					already_connected = true;
				}
			}
			if(!already_connected){
				domain_record.addTestUser(test_user_dao.save(test_user));
			}
		}

		PageStateDao page_state_dao = new PageStateDaoImpl();
		for(PageState page_state : domain.getPageStates()){
			//check if page state exists for domain already
			boolean already_connected = false;

			for(PageState page_state_record : domain_record.getPageStates()){
				if(page_state.getKey().equals(page_state_record.getKey())){
					already_connected = true;
				}
			}
			if(!already_connected){
				domain_record.addPageState(page_state_dao.save(page_state));
			}
		}
		
		connection.close();
		return domain_record;
	}

	@Override
	public Domain find(String key) {
		Domain domain = null;
		OrientConnectionFactory connection = new OrientConnectionFactory();

		try{
			domain = connection.getTransaction().getFramedVertices("key", key, Domain.class).next();
		}catch(NoSuchElementException e){
			System.err.println("could not find domain record");
		}
		connection.close();
		return domain;
	}
}
