package com.qanairy.models.dao.impl;

import java.util.Iterator;
import java.util.UUID;

import com.qanairy.models.dao.TestUserDao;
import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.OrientConnectionFactory;
import com.qanairy.persistence.TestUser;

public class TestUserDaoImpl implements TestUserDao{

	@Override
	public TestUser save(TestUser user) {
		user.setKey(generateKey(user));

		TestUser test_user_record = find(user.getKey());
		
		OrientConnectionFactory connection = new OrientConnectionFactory();
		if(test_user_record == null){
			test_user_record = connection.getTransaction().addFramedVertex(TestUser.class);
			test_user_record.setKey(user.getKey());
			test_user_record.setUsername(user.getUsername());
			test_user_record.setPassword(user.getPassword());
		}
		connection.close();

		return test_user_record;
	}

	@Override
	public TestUser find(String key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TestUser findByUsername(String username) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public String generateKey(TestUser test_user) {
		return test_user.getUsername()+"::"+test_user.getPassword();
	}
}
