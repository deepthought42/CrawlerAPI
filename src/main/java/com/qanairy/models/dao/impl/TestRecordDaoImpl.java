package com.qanairy.models.dao.impl;

import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.qanairy.models.TestRecordPOJO;
import com.qanairy.models.dao.PageStateDao;
import com.qanairy.models.dao.TestRecordDao;
import com.qanairy.persistence.OrientConnectionFactory;
import com.qanairy.persistence.TestRecord;

public class TestRecordDaoImpl implements TestRecordDao {
	private static Logger log = LoggerFactory.getLogger(TestRecordPOJO.class);
	
	@Override
	public TestRecord save(TestRecord record) {
		record.setKey(generateKey(record));
		OrientConnectionFactory connection = new OrientConnectionFactory();
		
		TestRecord test_record = find(record.getKey());
				
		if(test_record == null){
			test_record = connection.getTransaction().addFramedVertex(TestRecord.class);
			PageStateDao page_dao = new PageStateDaoImpl();
			test_record.setResult(page_dao.save(record.getResult()));
			test_record.setPassing(record.getPassing());
			test_record.setBrowser(record.getBrowser());
			test_record.setRanAt(record.getRanAt());
			test_record.setRunTime(record.getRunTime());
			test_record.setKey(record.getKey());
		}
		connection.close();
		return test_record;
	}

	@Override
	public TestRecord find(String key) {
		TestRecord test_record = null;
		OrientConnectionFactory connection = new OrientConnectionFactory();

		try{
			test_record = connection.getTransaction().getFramedVertices("key", key, TestRecord.class).next();
		}catch(NoSuchElementException e){
			log.error("Error requesting action record from database");
		}
		connection.close();
		return test_record;
	}
	
	/**
	 * Generates a key for this object
	 * @return generated key
	 */
	public String generateKey(TestRecord record) {
		return record.getRanAt().toString()+"::"+record.getPassing();
	}
}
