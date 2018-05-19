package com.qanairy.models.dto;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.qanairy.models.Domain;
import com.qanairy.models.Test;
import com.qanairy.models.TestUser;
import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.IDomain;
import com.qanairy.persistence.IPersistable;
import com.qanairy.persistence.ITest;
import com.qanairy.persistence.ITestUser;
import com.qanairy.persistence.OrientConnectionFactory;


/**
 * 
 */
@Component
public class DomainRepository implements IPersistable<Domain, IDomain> {

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
	public IDomain save(OrientConnectionFactory connection, Domain domain) {
		domain.setKey(generateKey(domain));
		@SuppressWarnings("unchecked")
		Iterable<IDomain> domains = (Iterable<IDomain>) DataAccessObject.findByKey(generateKey(domain), connection, IDomain.class);
		Iterator<IDomain> iter = domains.iterator();
		IDomain domain_record = null;

		if(!iter.hasNext()){
			domain_record = connection.getTransaction().addVertex("class:"+IDomain.class.getSimpleName()+","+UUID.randomUUID(), IDomain.class);
			domain_record.setKey(domain.getKey());
			domain_record.setUrl(domain.getUrl());
		}
		else{
			//figure out throwing exception because domain already exists
			domain_record = iter.next();
			domain.setUrl(domain_record.getUrl());
		}
		domain_record.setLogoUrl(domain.getLogoUrl());
		domain_record.setProtocol(domain.getProtocol());
		domain_record.setDiscoveryBrowserName(domain.getDiscoveryBrowser());
		domain_record.setDiscoveryTestCount(domain.getTestCount());
		
		TestUserRepository test_user_repo = new TestUserRepository();
		List<ITestUser> test_users = new ArrayList<ITestUser>();
		for(TestUser test_user : domain.getTestUsers()){
			test_users.add(test_user_repo.save(connection, test_user));
		}
		domain_record.setTestUsers(test_users);

		return domain_record;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public IDomain find(String key){
		OrientConnectionFactory connection = new OrientConnectionFactory();
		@SuppressWarnings("unchecked")
		Iterable<IDomain> domains = (Iterable<IDomain>) DataAccessObject.findByKey(key, connection, IDomain.class);
		Iterator<IDomain> iter = domains.iterator();
		  
		if(iter.hasNext()){
			//figure out throwing exception because domain already exists
			return iter.next();
		}
		connection.close();
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
			return load(iter.next());
		}
		
		return null;
	}

	@Override
	public Domain load(IDomain obj) {
		List<Test> tests = new ArrayList<Test>();
		/*TestRepository test_repo = new TestRepository();
		Lists.newArrayList(obj.getTests());
		Iterator<ITest> test_iter = obj.getTests().iterator();
		
		//NOTE:: TESTS SHOULD BE LAZY LOADED, AKA ONLY WHEN THEY ARE NEEDED
		
		while(test_iter.hasNext()){
			tests.add(test_repo.load(test_iter.next()));
		}
		*/
		TestUserRepository test_user_repo = new TestUserRepository();
		List<TestUser> test_users = new ArrayList<TestUser>();
		for(ITestUser test_user : obj.getTestUsers()){
			test_users.add(test_user_repo.load(test_user));
		}

		return new Domain(obj.getKey(), obj.getUrl(), obj.getLogoUrl(), tests, obj.getProtocol(), test_users, obj.getDiscoveryBrowserName(), obj.getDiscoveryTestCount());
	}
	
	public Domain load(OrientVertex obj) {
		List<Test> tests = new ArrayList<Test>();
		/*if(obj.getTests() != null){
			tests = Lists.newArrayList(obj.getProperty("tests"));
		}
		*/

		return new Domain(obj.getProperty("key"), obj.getProperty("url"), obj.getProperty("logo_url"), tests, obj.getProperty("protocol"), obj.getProperty("test_cnt"));
	}
	
	@Override
	public List<Domain> findAll(OrientConnectionFactory conn) {
		@SuppressWarnings("unchecked")
		Iterable<OrientVertex> domains = (Iterable<OrientVertex>) DataAccessObject.findAll(conn, IDomain.class);
		Iterator<OrientVertex> iter = domains.iterator();
		
		List<Domain> domain = new ArrayList<Domain>();
		while(iter.hasNext()){
			OrientVertex v = iter.next();
			domain.add(load(v));
		}
		
		return domain;
	}	
}