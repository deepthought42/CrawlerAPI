package com.qanairy.models.dto;

import java.util.Iterator;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minion.structs.Path;
import com.qanairy.models.Page;
import com.qanairy.models.ServicePackage;
import com.qanairy.models.Test;
import com.qanairy.models.TestRecord;
import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.IServicePackage;
import com.qanairy.persistence.ITest;
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
		
		path_key += test.getPath().generateKey();
		
		path_key += test.getResult().getKey();
		return path_key;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Test create(OrientConnectionFactory conn, Test test) {
		ITest test_record = find(conn, test.getKey());
		
		if(test_record == null){
			test_record = convertToRecord(conn, test);
			conn.save();
		}
		return test;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Does not allow updating domain
	 */
	public Test update(OrientConnectionFactory conn, Test test) {
		ITest test_record = find(conn, test.getKey());
		if(test_record != null){
			test_record.setCorrect(test.isCorrect());
			test_record.setGroups(test.getGroups());
			test_record.setName(test.getName());
			test_record.setRecords(test.getRecords());
			test_record.setResult(test.getResult().convertToRecord(conn, test));
			conn.save();
		}
		
		return test;
	}

	/**
	 * {@inheritDoc}
	 */
	public ITest convertToRecord(OrientConnectionFactory connection, Test test){
		ITest test_record = connection.getTransaction().addVertex("class:"+ITest.class.getCanonicalName()+","+UUID.randomUUID(), ITest.class);
		log.info("setting test_record properties");
		test_record.setPath(test.getPath().convertToRecord(connection));
		log.info("setting test_record result");
		test_record.setResult(test.getResult().convertToRecord(connection));
		test_record.setDomain(test.getDomain().convertToRecord(connection));
		test_record.setName(test.getName());
		test_record.setCorrect(test.isCorrect());
		test_record.setGroups(test.getGroups());
		
		for(TestRecord record : test.getRecords()){
			test_record.addRecord(record.convertToRecord(connection));
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
		Test test = new Test();
		
		test.setDomain(itest.getDomain());
		test.setKey(itest.getKey());
		test.setName(itest.getName());
		test.setCorrect(itest.getCorrect());
		test.setPath(Path.convertFromRecord(itest.getPath()));
		test.setRecords(TestRecord.convertFromRecord(itest.getRecords()));
		test.setResult(Page.convertFromRecord(itest.getResult()));
		test.setGroups(itest.getGroups());
		
		return test;
	}

	@Override
	public ITest find(OrientConnectionFactory connection, String key) {
		@SuppressWarnings("unchecked")
		Iterable<ITest> tests = (Iterable<ITest>) DataAccessObject.findByKey(key, connection, ITest.class);
		Iterator<ITest> iter = tests.iterator();
		
		ITest test_record = null; 
		if(iter.hasNext()){
			test_record = iter.next();
		}
		
		return test_record;
	}

}