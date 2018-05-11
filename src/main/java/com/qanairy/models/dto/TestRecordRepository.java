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
	public ITestRecord save(OrientConnectionFactory connection, TestRecord record){
		record.setKey(generateKey(record));
		ITestRecord testRecord = connection.getTransaction().addVertex("class:"+ITestRecord.class.getSimpleName()+","+UUID.randomUUID(), ITestRecord.class);

		PageRepository page_repo = new PageRepository();
		testRecord.setResult(page_repo.save(connection, record.getPage()));
		testRecord.setPassing(record.getPassing());
		testRecord.setBrowser(record.getBrowser());
		testRecord.setRanAt(record.getRanAt());
		testRecord.setRunTime(record.getRunTime());
		testRecord.setKey(record.getKey());
		
		return testRecord;
	}

	/**
	 * Generates a key for this object
	 * @return generated key
	 */
	public String generateKey(TestRecord record) {
		return record.getRanAt().toString()+"::"+record.getPassing();
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
			return load(iter.next());
		}
		
		return null;
	}

	@Override
	public TestRecord load(ITestRecord obj) {
		PageRepository page_repo = new PageRepository();
		Page page = page_repo.load(obj.getResult());
		TestRecord record = new TestRecord(obj.getKey(), obj.getRanAt(), obj.getPassing(), obj.getBrowser(), page, obj.getRunTime());
		
		return record;
	}

	@Override
	public List<TestRecord> findAll(OrientConnectionFactory connection) {
		// TODO Auto-generated method stub
		return null;
	}
}