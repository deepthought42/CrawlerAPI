package com.qanairy.models.dto;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import com.qanairy.models.TestRecord;
import com.qanairy.persistence.DataAccessObject;

import com.qanairy.persistence.ITestRecord;
import com.qanairy.persistence.IPersistable;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * 
 */
public class TestRecordRepository implements IPersistable<TestRecord, ITestRecord> {
	/**
	 * {@inheritDoc}
	 */
	public ITestRecord convertToRecord(OrientConnectionFactory connection, TestRecord record){
		ITestRecord testRecord = connection.getTransaction().addVertex(UUID.randomUUID(), ITestRecord.class);

		testRecord.setPasses(record.getPasses());
		testRecord.setRanAt(record.getRanAt());
		testRecord.setKey(record.getKey());
		
		return testRecord;
	}

	/**
	 * Generates a key for thos object
	 * @return generated key
	 */
	public String generateKey(TestRecord record) {
		return record.getPage().getKey()+":"+record.getRanAt();
	}

	/**
	 * {@inheritDoc}
	 */
	public TestRecord create(OrientConnectionFactory connection, TestRecord record) {
		TestRecord test_record_record = find(connection, generateKey(record));
		if(test_record_record == null){
			convertToRecord(connection, record);
			connection.save();
		}
		
		return test_record_record;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public TestRecord update(OrientConnectionFactory connection, TestRecord record) {
		TestRecord test_record_record = find(connection, record.getKey());
		if(test_record_record != null){
			test_record_record.setPasses(record.getPasses());
			test_record_record.setRanAt(record.getRanAt());
			
			connection.save();
		}
		
		return test_record_record;
	}

	/**
	 * 
	 */
	@Override
	public TestRecord find(OrientConnectionFactory conn, String key){
		@SuppressWarnings("unchecked")
		Iterable<ITestRecord> domains = (Iterable<ITestRecord>) DataAccessObject.findByKey(key, conn, ITestRecord.class);
		Iterator<ITestRecord> iter = domains.iterator();
		  
		if(iter.hasNext()){
			return convertFromRecord(iter.next());
		}
		
		return null;
	}

	@Override
	public TestRecord convertFromRecord(ITestRecord obj) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<TestRecord> findAll(OrientConnectionFactory connection) {
		// TODO Auto-generated method stub
		return null;
	}
}