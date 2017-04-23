package com.qanairy.models.dto;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.qanairy.models.QanairyUser;
import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.IQanairyUser;
import com.qanairy.persistence.IPersistable;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * 
 */
@Component
public class QanairyUserRepository implements IPersistable<QanairyUser, IQanairyUser> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateKey(QanairyUser qanairy_user) {
		return qanairy_user.getEmail();
	}

	@Override
	public IQanairyUser convertToRecord(OrientConnectionFactory connection, QanairyUser qanairyUser) {
		qanairyUser.setKey(generateKey(qanairyUser));
		IQanairyUser qanairy_user = connection.getTransaction().addVertex("class:"+IQanairyUser.class.getSimpleName()+","+UUID.randomUUID(), IQanairyUser.class);
		qanairy_user.setKey(qanairyUser.getKey());
		qanairy_user.setEmail(qanairyUser.getEmail());

		return qanairy_user;
	}

	@Override
	public QanairyUser convertFromRecord(IQanairyUser qanairyUser) {
		return new QanairyUser(qanairyUser.getKey(), qanairyUser.getEmail());
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public QanairyUser create(OrientConnectionFactory connection, QanairyUser qanairyUser) {
		qanairyUser.setKey(generateKey(qanairyUser));

		@SuppressWarnings("unchecked")
		Iterable<IQanairyUser> qanairyUsers = (Iterable<IQanairyUser>) DataAccessObject.findByKey(generateKey(qanairyUser), connection, IQanairyUser.class);
		Iterator<IQanairyUser> iter = qanairyUsers.iterator();
		  
		if(!iter.hasNext()){
			convertToRecord(connection, qanairyUser);
			connection.getTransaction().commit();
		}
		return qanairyUser;

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public QanairyUser update(OrientConnectionFactory connection, QanairyUser qanairyUser) {
		@SuppressWarnings("unchecked")
		Iterable<IQanairyUser> qanairyUsers = (Iterable<IQanairyUser>) DataAccessObject.findByKey(qanairyUser.getKey(), connection, IQanairyUser.class);
		Iterator<IQanairyUser> iter = qanairyUsers.iterator();
		  
		IQanairyUser qanairy_user = null;
		if(iter.hasNext()){
			qanairy_user = iter.next();
			qanairy_user.setEmail(qanairyUser.getEmail());
			
			connection.save();
		}
		return convertFromRecord(qanairy_user);
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public QanairyUser find(OrientConnectionFactory connection, String key) {
		@SuppressWarnings("unchecked")
		Iterable<IQanairyUser> svc_pkgs = (Iterable<IQanairyUser>) DataAccessObject.findByKey(key, connection, IQanairyUser.class);
		Iterator<IQanairyUser> iter = svc_pkgs.iterator();
		
		if(iter.hasNext()){
			return convertFromRecord(iter.next());
		}
		
		return null;
	}

	@Override
	public List<QanairyUser> findAll(OrientConnectionFactory connection) {
		// TODO Auto-generated method stub
		return null;
	} 
}