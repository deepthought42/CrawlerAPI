package models;

import java.net.URL;
import java.util.Date;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.qanairy.models.Page;
import com.qanairy.models.TestRecord;
import com.qanairy.models.dto.TestRecordRepository;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * 
 *
 */
public class TestRecordTests {
	
	@Test
	public void testRecordCreateRecord(){
		TestRecordRepository test_record_repo = new TestRecordRepository();
		com.qanairy.models.Test test = new com.qanairy.models.Test();
		TestRecord test_record = new TestRecord(new Date(), true, null, null);
		
		TestRecord test_record_record = test_record_repo.create(new OrientConnectionFactory(), test_record);
		
		Assert.assertTrue(test_record_record.getKey().equals(test_record.getKey()));
	}
	
	@Test
	public void testRecordUpdateRecord(){
		TestRecordRepository test_record_repo = new TestRecordRepository();

		TestRecord test_record = new TestRecord(new Date(), false);
		TestRecord test_record_record = test_record_repo.update(new OrientConnectionFactory(), test_record);
		
		Assert.assertTrue(test_record_record.getKey().equals(test_record.getKey()));
	}
	
	@Test
	public void testRecordFindRecord(){
		TestRecordRepository test_record_repo = new TestRecordRepository();

		OrientConnectionFactory orient_connection = new OrientConnectionFactory();

		TestRecord test_record = new TestRecord(new Date(), false);
		test_record_repo.create(orient_connection,test_record);
		TestRecord test_record_record = test_record_repo.find(orient_connection, test_record.getKey());
		
		Assert.assertTrue(test_record_record.getKey().equals(test_record.getKey()));
	}
}
