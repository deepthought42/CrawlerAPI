package com.qanairy.models.dao.impl;

import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qanairy.models.dao.TestUserDao;
import com.qanairy.persistence.TestUser;
import com.qanairy.persistence.OrientConnectionFactory;

public class TestUserDaoImpl implements TestUserDao{
	private static Logger log = LoggerFactory.getLogger(TestUserDaoImpl.class);
	
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
		TestUser test_user = null;
		OrientConnectionFactory connection = new OrientConnectionFactory();

		try{
			test_user = connection.getTransaction().getFramedVertices("key", key, TestUser.class).next();
		}catch(NoSuchElementException e){
			log.error("Error requesting action record from database");
		}
		connection.close();
		return test_user;
	}

	@Override
	public TestUser findByUsername(String username) {
		TestUser test_user = null;
		OrientConnectionFactory connection = new OrientConnectionFactory();

		try{
			test_user = connection.getTransaction().getFramedVertices("username", username, TestUser.class).next();
		}catch(NoSuchElementException e){
			log.error("Error requesting action record from database");
		}
		connection.close();
		return test_user;
	}
}
