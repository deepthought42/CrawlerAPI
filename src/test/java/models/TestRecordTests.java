package models;

import java.util.Date;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.qanairy.models.TestRecord;
import com.qanairy.persistence.ITestRecord;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * 
 *
 */
public class TestRecordTests {
	
	@Test
	public void testRecordCreateRecord(){
		TestRecord test_record = new TestRecord(new Date(), true);
		ITestRecord test_record_record = test_record.create(new OrientConnectionFactory());
		
		Assert.assertTrue(test_record_record.getKey().equals(test_record.getKey()));
	}
	
	@Test
	public void testRecordUpdateRecord(){
		TestRecord test_record = new TestRecord(new Date(), false);
		ITestRecord test_record_record = test_record.update(new OrientConnectionFactory());
		
		Assert.assertTrue(test_record_record.getKey().equals(test_record.getKey()));
	}
	
	@Test
	public void testRecordFindRecord(){
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();

		TestRecord test_record = new TestRecord(new Date(), false);
		test_record.create(orient_connection);
		ITestRecord test_record_record = test_record.find(orient_connection);
		
		Assert.assertTrue(test_record_record.getKey().equals(test_record.getKey()));
	}
}
