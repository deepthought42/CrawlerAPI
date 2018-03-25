package com.qanairy.models.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;


import org.slf4j.Logger;import org.slf4j.LoggerFactory;

import com.qanairy.models.Domain;
import com.qanairy.models.Group;
import com.qanairy.models.Test;
import com.qanairy.models.TestRecord;
import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.IDomain;
import com.qanairy.persistence.IGroup;
import com.qanairy.persistence.ITest;
import com.qanairy.persistence.ITestRecord;
import com.qanairy.persistence.IPersistable;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * 
 */
public class TestRepository implements IPersistable<Test, ITest> {
	private static Logger log = LoggerFactory.getLogger(Test.class);

	/**
	 * Generates a key using both path and result in order to guarantee uniqueness of key as well 
	 * as easy identity of {@link Test} when generated in the wild via discovery
	 * 
	 * @return
	 */
	public String generateKey(Test test) {
		String path_key = "";
		PathRepository path_record = new PathRepository();
		path_key += path_record.generateKey(test.getPath());
		
		PageRepository page_repo = new PageRepository();
		path_key += page_repo.generateKey(test.getResult());
		
		return path_key;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Deprecated
	public Test create(OrientConnectionFactory conn, Test test) {
		test.setKey(generateKey(test));
		Test test_record = find(conn, test.getKey());
		
		if(test_record == null){
			save(conn, test);
		}
		else{
			test = test_record;
		}
		return test;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Does not allow updating domain
	 * 
	 * @pre test key != null
	 */
	public Test update(OrientConnectionFactory conn, Test test) {
		assert test.getKey() != null;
		
		@SuppressWarnings("unchecked")
		Iterable<ITest> tests = (Iterable<ITest>) DataAccessObject.findByKey(test.getKey(), conn, ITest.class);
		Iterator<ITest> iter = tests.iterator();
		
		PageRepository page_record = new PageRepository();

		if(iter.hasNext()){
			ITest test_record = iter.next();
			test_record.setCorrect(test.isCorrect());
			List<IGroup> igroups = new ArrayList<IGroup>();
			GroupRepository group_repo = new GroupRepository();
			for(Group group : test.getGroups()){
				group_repo.save(conn, group);
			}
			test_record.setGroups(igroups);
			test_record.setName(test.getName());
			test_record.setRecords(test.getRecords());
			test_record.setResult(page_record.save(conn, test.getResult()));
			conn.save();
		}
		
		return test;
	}

	/**
	 * {@inheritDoc}
	 */
	public ITest save(OrientConnectionFactory connection, Test test){
		
		@SuppressWarnings("unchecked")
		Iterable<ITest> tests = (Iterable<ITest>) DataAccessObject.findByKey(generateKey(test), connection, ITest.class);
		Iterator<ITest> iter = tests.iterator();
		ITest test_record = null;
		if(iter.hasNext()){
			test_record = iter.next();
		}
		else{
			test_record = connection.getTransaction().addVertex("class:"+ITest.class.getSimpleName()+","+UUID.randomUUID(), ITest.class);
			test_record.setKey(generateKey(test));
			PathRepository path_record = new PathRepository();
			
			test_record.setPath(path_record.save(connection, test.getPath()));
		}
		
		PageRepository page_record = new PageRepository();
		TestRecordRepository test_record_record = new TestRecordRepository();
		
		test_record.setResult(page_record.save(connection, test.getResult()));
		
		DomainRepository domain_record = new DomainRepository();
		IDomain idomain = domain_record.save(connection, test.getDomain());
		Iterator<ITest> test_iter = idomain.getTests().iterator();
		boolean test_exists = false;
		while(test_iter.hasNext()){
			ITest itest = test_iter.next();
			if(itest.getKey().equals(test_record.getKey())){
				test_exists = true;
			}
		}
		if(!test_exists){
			test_record.addDomain(idomain);
		}
		
		for(TestRecord record : test.getRecords()){
			test_record.addRecord(test_record_record.save(connection, record));
		}
		
		List<IGroup> igroups = new ArrayList<IGroup>();
		GroupRepository group_repo = new GroupRepository();
		for(Group group : test.getGroups()){
			igroups.add(group_repo.save(connection, group));
		}
		test_record.setGroups(igroups);
		test_record.setLastRunTimestamp(test.getLastRunTimestamp());
		test_record.setRunTime(test.getRunTime());
		test_record.setName(test.getName());
		//test_record.setCorrect(test.isCorrect());
		test_record.setBrowserStatuses(test.getBrowserPassingStatuses());
		
		return test_record;
	}
	
	/**
	 * 
	 * 
	 * @param itest
	 * @return
	 */
	@Override
	public Test load(ITest itest){
		TestRecordRepository test_record = new TestRecordRepository();
		PageRepository page_record = new PageRepository();
		PathRepository path_record = new PathRepository();
		GroupRepository group_repo = new GroupRepository();
		
		Test test = new Test();
		test.setKey(itest.getKey());
		test.setName(itest.getName());
		test.setCorrect(itest.getCorrect());
		
		DomainRepository domain_repo = new DomainRepository();
		test.setDomain(domain_repo.load(itest.getDomain()));
		test.setBrowserPassingStatuses(itest.getBrowserStatuses());
		
		try{
			test.setPath(path_record.load(itest.getPath()));
		}
		catch(NullPointerException e){
			log.error("Null pointer exception occurred while setting path.\n", e.getMessage());
		}
		test.setLastRunTimestamp(itest.getLastRunTimestamp());

		try{
			test.setRunTime(itest.getRunTime());
		}catch(NullPointerException e){
			test.setRunTime(0L);
		}
		
		Iterator<ITestRecord> test_record_iter = itest.getRecords().iterator();
		List<TestRecord> test_records = new ArrayList<TestRecord>();
		while(test_record_iter != null && test_record_iter.hasNext()){
			test_records.add(test_record.load(test_record_iter.next()));
		}
		test.setRecords(test_records);
				
		test.setResult(page_record.load(itest.getResult()));
		Iterator<IGroup> group_records_iter = itest.getGroups().iterator();
		List<Group> group_records = new ArrayList<Group>();
		
		while(group_records_iter != null && group_records_iter.hasNext()){
			group_records.add(group_repo.load(group_records_iter.next()));
		}
		
		test.setGroups(group_records);
		return test;
	}

	/**
	 * 
	 * @param connection
	 * @param key
	 * @return
	 */
	@Override
	public Test find(OrientConnectionFactory connection, String key) {
		@SuppressWarnings("unchecked")
		Iterable<ITest> tests = (Iterable<ITest>) DataAccessObject.findByKey(key, connection, ITest.class);
		Iterator<ITest> iter = tests.iterator();
		
		Test test_record = null; 
		if(iter.hasNext()){
			test_record = load(iter.next());
		}
		
		return test_record;
	}

	@Override
	public List<Test> findAll(OrientConnectionFactory connection) {
		// TODO Auto-generated method stub
		return null;
	}

}