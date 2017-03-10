package com.qanairy.models.dto;

import java.util.Iterator;
import java.util.UUID;

import com.qanairy.models.Domain;
import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.IDomain;
import com.qanairy.persistence.IPersistable;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * 
 */
public class DomainRepository implements IPersistable<Domain, IDomain> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateKey(Domain domain) {
		return domain.getUrl().toString();
	}

	/**
	 * {@inheritDoc}
	 */
	public IDomain convertToRecord(OrientConnectionFactory connection, Domain domain) {
		IDomain domain_record = connection.getTransaction().addVertex("class:"+IDomain.class.getCanonicalName()+","+UUID.randomUUID(), IDomain.class);
		domain_record.setKey(generateKey(domain));
		domain_record.setUrl(domain.getUrl());
		domain_record.setTests(domain.getTests());
		domain_record.setGroups(domain.getGroups());
		return domain_record;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Domain create(OrientConnectionFactory connection, Domain domain) {
		domain.setKey(generateKey(domain));
		IDomain domain_record = find(connection, generateKey(domain));
		
		if(domain_record != null){
			domain_record = convertToRecord(connection, domain);
			connection.save();
		}
		return domain;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Domain update(OrientConnectionFactory connection, Domain domain) {
		IDomain domain_record = find(connection, domain.getKey());
		if(domain_record != null){
			domain_record.setGroups(domain.getGroups());
			domain_record.setTests(domain.getTests());
			domain_record.setUrl(domain.getUrl());
			connection.save();
		}
		
		return domain;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public IDomain find(OrientConnectionFactory connection, String key) {
		@SuppressWarnings("unchecked")
		Iterable<IDomain> domains = (Iterable<IDomain>) DataAccessObject.findByKey(key, connection, IDomain.class);
		Iterator<IDomain> iter = domains.iterator();
		  
		if(iter.hasNext()){
			//figure out throwing exception because domain already exists
			return iter.next();
		}
		
		return null;
	}

	@Override
	public Domain convertFromRecord(IDomain obj) {
		// TODO Auto-generated method stub
		return null;
	}	
}