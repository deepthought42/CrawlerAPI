package com.qanairy.models.dto;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import com.qanairy.models.Page;
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
		ITestRecord testRecord = connection.getTransaction().addVertex("class:"+ITestRecord.class.getSimpleName()+","+UUID.randomUUID(), ITestRecord.class);

		PageRepository page_repo = new PageRepository();
		System.out.println("Retrieving Record :: "+record);
		System.out.println("Retrieving page :: "+record.getPage());
		testRecord.setResult(page_repo.convertToRecord(connection, record.getPage()));
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
		return record.getRanAt().toString()+"::"+record.getPasses();
	}

	/**
	 * {@inheritDoc}
	 */
	public TestRecord create(OrientConnectionFactory connection, TestRecord record) {
		record.setKey(generateKey(record));
		TestRecord test_record_record = find(connection, generateKey(record));
		if(test_record_record == null){
			convertToRecord(connection, record);
			//connection.save();
		}
		else{
			record = test_record_record;
		}
		return record;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public TestRecord update(OrientConnectionFactory connection, TestRecord record) {
		assert record.getKey() != null;

		TestRecord test_record_record = find(connection, record.getKey());
		if(test_record_record != null){
			test_record_record.setPasses(record.getPasses());
			test_record_record.setRanAt(record.getRanAt());

			connection.save();
			record = test_record_record;
		}
		
		return record;
	}

	/**
	 * 
	 */
	@Override
	public TestRecord find(OrientConnectionFactory conn, String key){
		@SuppressWarnings("unchecked")
		Iterable<ITestRecord> testRecords = (Iterable<ITestRecord>) DataAccessObject.findByKey(key, conn, ITestRecord.class);
		Iterator<ITestRecord> iter = (Iterator<ITestRecord>)testRecords.iterator();
		  
		if(iter.hasNext()){
			return convertFromRecord(iter.next());
		}
		
		return null;
	}

	@Override
	public TestRecord convertFromRecord(ITestRecord obj) {
		PageRepository page_repo = new PageRepository();
		Page page = page_repo.convertFromRecord(obj.getResult());
		
		TestRecord record = new TestRecord(obj.getKey(), obj.getRanAt(), obj.getPasses(), page);
		
		return record;
	}

	@Override
	public List<TestRecord> findAll(OrientConnectionFactory connection) {
		// TODO Auto-generated method stub
		return null;
	}
}