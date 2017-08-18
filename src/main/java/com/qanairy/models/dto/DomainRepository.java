package com.qanairy.models.dto;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.qanairy.models.Account;
import com.qanairy.models.Domain;
import com.qanairy.models.Group;
import com.qanairy.models.Test;
import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.IAccount;
import com.qanairy.persistence.IDomain;
import com.qanairy.persistence.IPersistable;
import com.qanairy.persistence.ITest;
import com.qanairy.persistence.OrientConnectionFactory;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

/**
 * 
 */
@Component
public class DomainRepository implements IPersistable<Domain, IDomain> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateKey(Domain domain) {
		return domain.getUrl().toString();
	}

	public boolean addTest(Domain domain, ITest test){
		try{
			IDomain idomain = find(domain.getKey());
			idomain.addTest(test);
			return true;
		}catch(Exception e){
			return false;
		}
	}
	/**
	 * {@inheritDoc}
	 */
	public IDomain convertToRecord(OrientConnectionFactory connection, Domain domain) {
		domain.setKey(generateKey(domain));
		@SuppressWarnings("unchecked")
		Iterable<IDomain> domains = (Iterable<IDomain>) DataAccessObject.findByKey(generateKey(domain), connection, IDomain.class);
		Iterator<IDomain> iter = domains.iterator();
		IDomain domain_record = null;

		if(!iter.hasNext()){
			domain_record = connection.getTransaction().addVertex("class:"+IDomain.class.getSimpleName()+","+UUID.randomUUID(), IDomain.class);
			domain_record.setKey(domain.getKey());
		}
		else{
			//figure out throwing exception because domain already exists
			domain_record = iter.next();
		}
		domain_record.setUrl(domain.getUrl());
		/*TestRepository test_repo = new TestRepository();
		List<ITest> tests = new ArrayList<ITest>();
		for(Test test : domain.getTests()){
			tests.add(test_repo.convertToRecord(connection, test));
		}
		domain_record.setTests(tests);
		*/
		return domain_record;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Domain create(OrientConnectionFactory connection, Domain domain) {
		domain.setKey(generateKey(domain));
		Domain domain_record = find(connection, generateKey(domain));
		
		if(domain_record == null){
			convertToRecord(connection, domain);
			connection.save();
		}
		return domain;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Domain update(OrientConnectionFactory connection, Domain domain) {
		@SuppressWarnings("unchecked")
		Iterable<IDomain> domains = (Iterable<IDomain>) DataAccessObject.findByKey(domain.getKey(), connection, IDomain.class);
		Iterator<IDomain> iter = domains.iterator();

		if(iter.hasNext()){
			IDomain domain_record = iter.next();
			GroupRepository group_repo = new GroupRepository();
			//domain_record.setGroups(domain.getGroups());
			
			TestRepository test_repo = new TestRepository();
			//domain_record.setTests(domain.getTests());
			domain_record.setUrl(domain.getUrl());
			connection.save();
		}
		
		return domain;
	}
	
	public IDomain find(String key){
		@SuppressWarnings("unchecked")
		Iterable<IDomain> domains = (Iterable<IDomain>) DataAccessObject.findByKey(key, new OrientConnectionFactory(), IDomain.class);
		Iterator<IDomain> iter = domains.iterator();
		  
		if(iter.hasNext()){
			//figure out throwing exception because domain already exists
			return iter.next();
		}
		
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Domain find(OrientConnectionFactory connection, String key) {
		@SuppressWarnings("unchecked")
		Iterable<IDomain> domains = (Iterable<IDomain>) DataAccessObject.findByKey(key, connection, IDomain.class);
		Iterator<IDomain> iter = domains.iterator();
		  
		if(iter.hasNext()){
			//figure out throwing exception because domain already exists
			return convertFromRecord(iter.next());
		}
		
		return null;
	}

	@Override
	public Domain convertFromRecord(IDomain obj) {
		List<Test> tests = new ArrayList<Test>();
		TestRepository test_repo = new TestRepository();
		Lists.newArrayList(obj.getTests());
		Iterator<ITest> test_iter = obj.getTests().iterator();
		/*
		NOTE:: TESTS SHOULD BE LAZY LOADED, AKA ONLY WHEN THEY ARE NEEDED
		
		while(test_iter.hasNext()){
			tests.add(test_repo.convertFromRecord(test_iter.next()));
		}
		*/
		List<Group> groups = new ArrayList<Group>();
		if(obj.getGroups() != null){
			Lists.newArrayList(obj.getGroups());
		}
		
		Domain domain = new Domain(obj.getKey(), obj.getUrl().toString(), tests, groups);
		return domain;
	}
	
	public Domain convertFromRecord(OrientVertex obj) {
		List<Test> tests = new ArrayList<Test>();
		/*if(obj.getTests() != null){
			tests = Lists.newArrayList(obj.getProperty("tests"));
		}
		*/
		List<Group> groups = new ArrayList<Group>();

		Domain domain = new Domain(obj.getProperty("key"), obj.getProperty("url"), tests, groups);
		return domain;
	}
	
	@Override
	public List<Domain> findAll(OrientConnectionFactory conn) {
		@SuppressWarnings("unchecked")
		Iterable<OrientVertex> domains = (Iterable<OrientVertex>) DataAccessObject.findAll(conn, IDomain.class);
		Iterator<OrientVertex> iter = domains.iterator();
		
		List<Domain> domain = new ArrayList<Domain>();
		while(iter.hasNext()){
			OrientVertex v = iter.next();
			domain.add(convertFromRecord(v));
		}
		
		return domain;
	}	
}