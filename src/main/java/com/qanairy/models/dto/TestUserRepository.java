package com.qanairy.models.dto;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import com.qanairy.models.TestUser;
import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.ITestUser;
import com.qanairy.persistence.IPersistable;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * Handles the interaction with the db layer for {@linkplain TestUser}s
 */
@Component
public class TestUserRepository implements IPersistable<TestUser, ITestUser> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateKey(TestUser test_user) {
		return test_user.getUsername()+"::"+test_user.getPassword();
	}

	@Override
	public ITestUser save(OrientConnectionFactory connection, TestUser test_user) {
		test_user.setKey(generateKey(test_user));

		@SuppressWarnings("unchecked")
		Iterable<ITestUser> test_users = (Iterable<ITestUser>) DataAccessObject.findByKey(generateKey(test_user), connection, ITestUser.class);
		Iterator<ITestUser> iter = test_users.iterator();
		ITestUser test_user_record = null;
		
		if(!iter.hasNext()){
			test_user_record = connection.getTransaction().addVertex("class:"+ITestUser.class.getSimpleName()+","+UUID.randomUUID(), ITestUser.class);
			test_user_record.setKey(test_user.getKey());
			test_user_record.setUsername(test_user.getUsername());
			test_user_record.setPassword(test_user.getPassword());
		}
		else{
			test_user_record = iter.next();
		}
		
		return test_user_record;
	}

	@Override
	public TestUser load(ITestUser test_user) {
		return new TestUser(test_user.getKey(), test_user.getUsername(), test_user.getPassword());
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public TestUser create(OrientConnectionFactory connection, TestUser test_user) {
		test_user.setKey(generateKey(test_user));

		@SuppressWarnings("unchecked")
		Iterable<ITestUser> test_users = (Iterable<ITestUser>) DataAccessObject.findByKey(generateKey(test_user), connection, ITestUser.class);
		Iterator<ITestUser> iter = test_users.iterator();
		  
		if(!iter.hasNext()){
			save(connection, test_user);
			connection.save();
		}
		return test_user;

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public TestUser update(OrientConnectionFactory connection, TestUser test_user) {
		@SuppressWarnings("unchecked")
		Iterable<ITestUser> test_users = (Iterable<ITestUser>) DataAccessObject.findByKey(test_user.getKey(), connection, ITestUser.class);
		Iterator<ITestUser> iter = test_users.iterator();
		  
		ITestUser test_user_record = null;
		if(iter.hasNext()){
			test_user_record = iter.next();
			test_user_record.setUsername(test_user.getUsername());
			test_user_record.setPassword(test_user.getPassword());
			
			connection.save();
		}
		return load(test_user_record);
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public TestUser find(OrientConnectionFactory connection, String key) {
		@SuppressWarnings("unchecked")
		Iterable<ITestUser> svc_pkgs = (Iterable<ITestUser>) DataAccessObject.findByKey(key, connection, ITestUser.class);
		Iterator<ITestUser> iter = svc_pkgs.iterator();
		
		if(iter.hasNext()){
			return load(iter.next());
		}
		
		return null;
	}

	@Override
	public List<TestUser> findAll(OrientConnectionFactory connection) {
		// TODO Auto-generated method stub
		return null;
	}
}