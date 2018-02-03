package com.qanairy.models.dto;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import com.qanairy.models.ApplicationUser;
import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.IPersistable;
import com.qanairy.persistence.IApplicationUser;
import com.qanairy.persistence.OrientConnectionFactory;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

/**
 * 
 * 
 */
public class ApplicationUserRepository implements IPersistable<ApplicationUser, IApplicationUser>{
	
	public ApplicationUserRepository() {}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateKey(ApplicationUser user) {
		assert user != null;
		return org.apache.commons.codec.digest.DigestUtils.sha256Hex(user.getUsername());
	}

	@Override
	public IApplicationUser convertToRecord(OrientConnectionFactory connection, ApplicationUser application_user) {
		application_user.setKey(generateKey(application_user));
		@SuppressWarnings("unchecked")
		Iterable<IApplicationUser> application_users = (Iterable<IApplicationUser>) DataAccessObject.findByKey(application_user.getKey(), connection, IApplicationUser.class);
		Iterator<IApplicationUser> iter = application_users.iterator();
		IApplicationUser user_record = null;  

		if(!iter.hasNext()){
			user_record = connection.getTransaction().addVertex("class:"+IApplicationUser.class.getSimpleName()+","+UUID.randomUUID(), IApplicationUser.class);
			user_record.setKey(application_user.getKey());
			user_record.setUsername(application_user.getUsername());
			user_record.setPassword(application_user.getPassword());
		}
		else{
			user_record = iter.next();
		}
		
		return user_record;
	}

	@SuppressWarnings("unchecked")
	@Override
	public ApplicationUser convertFromRecord(IApplicationUser application_user) {
		return new ApplicationUser(application_user.getKey(), application_user.getUsername(), application_user.getPassword());
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ApplicationUser create(OrientConnectionFactory connection, ApplicationUser application_user) {
		application_user.setKey(generateKey(application_user));

		@SuppressWarnings("unchecked")
		Iterable<IApplicationUser> application_users = (Iterable<IApplicationUser>) DataAccessObject.findByKey(application_user.getKey(), connection, IApplicationUser.class);
		Iterator<IApplicationUser> iter = application_users.iterator();
		  
		if(!iter.hasNext()){
			convertToRecord(connection, application_user);
		}
		
		return application_user;

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ApplicationUser update(OrientConnectionFactory connection, ApplicationUser application_user) {
		if(application_user.getKey() == null){
			application_user.setKey(generateKey(application_user));
		}
		@SuppressWarnings("unchecked")
		Iterable<IApplicationUser> application_users = (Iterable<IApplicationUser>) DataAccessObject.findByKey(application_user.getKey(), connection, IApplicationUser.class);
		Iterator<IApplicationUser> iter = application_users.iterator();
		  
		IApplicationUser application_user_record = null;
		if(iter.hasNext()){
			application_user_record = iter.next();
			application_user_record.setPassword(application_user.getPassword());
		}
		return convertFromRecord(application_user_record);
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public ApplicationUser find(OrientConnectionFactory connection, String key) {
		@SuppressWarnings("unchecked")
		Iterable<IApplicationUser> svc_pkgs = (Iterable<IApplicationUser>) DataAccessObject.findByKey(key, connection, IApplicationUser.class);
		Iterator<IApplicationUser> iter = svc_pkgs.iterator();
		
		if(iter.hasNext()){
			return convertFromRecord(iter.next());
		}
		
		return null;
	}

	@Override
	public List<ApplicationUser> findAll(OrientConnectionFactory conn) {
		@SuppressWarnings("unchecked")
		Iterator<OrientVertex> iter = ((Iterable<OrientVertex>) DataAccessObject.findAll(conn, IApplicationUser.class)).iterator();
		
		List<ApplicationUser> application_users = new ArrayList<ApplicationUser>();
		while(iter.hasNext()){
			OrientVertex v = iter.next();
			application_users.add(convertFromRecord((IApplicationUser)v));
		}
		
		return application_users;
	}
}
