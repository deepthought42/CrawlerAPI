package com.qanairy.models.dto;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qanairy.models.Group;
import com.qanairy.models.Test;
import com.qanairy.models.TestRecord;
import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.IGroup;
import com.qanairy.persistence.ITest;
import com.qanairy.persistence.ITestRecord;
import com.qanairy.persistence.IPersistable;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * 
 */
public class TestRepository implements IPersistable<Test, ITest> {
    private static final Logger log = LoggerFactory.getLogger(Test.class);

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
		
		path_key += test.getResult().getKey();
		return path_key;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Test create(OrientConnectionFactory conn, Test test) {
		test.setKey(generateKey(test));
		Test test_record = find(conn, test.getKey());
		
		if(test_record == null){
			convertToRecord(conn, test);
			conn.save();
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
			test_record.setGroups(test.getGroups());
			test_record.setName(test.getName());
			test_record.setRecords(test.getRecords());
			test_record.setResult(page_record.convertToRecord(conn, test.getResult()));
			conn.save();
		}
		
		return test;
	}

	/**
	 * {@inheritDoc}
	 */
	public ITest convertToRecord(OrientConnectionFactory connection, Test test){
		PathRepository path_record = new PathRepository();
		PageRepository page_record = new PageRepository();
		DomainRepository domain_record = new DomainRepository();
		TestRecordRepository test_record_record = new TestRecordRepository();
		
		ITest test_record = connection.getTransaction().addVertex("class:"+ITest.class.getCanonicalName()+","+UUID.randomUUID(), ITest.class);
		log.info("setting test_record properties");
		test_record.setPath(path_record.convertToRecord(connection, test.getPath()));
		log.info("setting test_record result");
		test_record.setResult(page_record.convertToRecord(connection, test.getResult()));
		test_record.setDomain(domain_record.convertToRecord(connection, test.getDomain()));
		test_record.setName(test.getName());
		test_record.setCorrect(test.isCorrect());
		test_record.setGroups(test.getGroups());
		
		for(TestRecord record : test.getRecords()){
			test_record.addRecord(test_record_record.convertToRecord(connection, record));
		}
		test_record.setKey(test.getKey());
		
		return test_record;
	}
	
	/**
	 * 
	 * 
	 * @param itest
	 * @return
	 */
	@Override
	public Test convertFromRecord(ITest itest){
		TestRecordRepository test_record = new TestRecordRepository();
		PageRepository page_record = new PageRepository();
		PathRepository path_record = new PathRepository();
		GroupRepository group_repo = new GroupRepository();
		
		Test test = new Test();
		
		test.setDomain(itest.getDomain());
		test.setKey(itest.getKey());
		test.setName(itest.getName());
		test.setCorrect(itest.getCorrect());
		test.setPath(path_record.convertFromRecord(itest.getPath()));
		
		Iterator<ITestRecord> test_record_iter = itest.getRecords();
		List<TestRecord> test_records = new ArrayList<TestRecord>();
		while(test_record_iter != null && test_record_iter.hasNext()){
			test_records.add(test_record.convertFromRecord(test_record_iter.next()));
		}
		test.setRecords(test_records);
				
		test.setResult(page_record.convertFromRecord(itest.getResult()));
		Iterator<IGroup> group_records_iter = itest.getGroups();
		List<Group> group_records = new ArrayList<Group>();
		while(group_records_iter != null && group_records_iter.hasNext()){
			group_records.add(group_repo.convertFromRecord(group_records_iter.next()));
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
			test_record = convertFromRecord(iter.next());
		}
		
		return test_record;
	}

	@Override
	public List<Test> findAll(OrientConnectionFactory connection) {
		// TODO Auto-generated method stub
		return null;
	}

}