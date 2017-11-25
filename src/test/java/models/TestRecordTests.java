package models;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.qanairy.models.Page;
import com.qanairy.models.PageElement;
import com.qanairy.models.PageSource;
import com.qanairy.models.Path;
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
		test.setKey("TempTestKey");
		Page page = null;
		try {
			page = new Page("<html><body></body></html>",
							"http://www.test.test", "", new ArrayList<PageElement>(), true);
		} catch (IOException e) {
			Assert.assertFalse(true);
		}
		Path path = new Path();
		path.add(page);
		
		test.setPath(path);
		test.setResult(page);
		TestRecord test_record = new TestRecord(new Date(), true, page);	
		TestRecord test_record_record = test_record_repo.create(new OrientConnectionFactory(), test_record);
		
		Assert.assertTrue(test_record_record.getKey().equals(test_record_repo.generateKey(test_record)));
	}
	
	@Test
	public void testRecordUpdateRecord(){
		TestRecordRepository test_record_repo = new TestRecordRepository();

		com.qanairy.models.Test test = new com.qanairy.models.Test();
		test.setKey("TempTestKey");
		Page page = null;
		try {
			page = new Page("<html><body></body></html>",
							"http://www.test.test", "", new ArrayList<PageElement>(), true);
		} catch (IOException e) {
			Assert.assertFalse(true);
		}
		Path path = new Path();
		path.add(page);
		
		test.setPath(path);
		test.setResult(page);
		TestRecord test_record = new TestRecord(new Date(), true, page);
		test_record = test_record_repo.create(new OrientConnectionFactory(), test_record);
		TestRecord test_record_record = test_record_repo.update(new OrientConnectionFactory(), test_record);
		
		Assert.assertTrue(test_record_record.getKey().equals(test_record.getKey()));
	}
	
	@Test
	public void testRecordFindRecord(){
		TestRecordRepository test_record_repo = new TestRecordRepository();

		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		com.qanairy.models.Test test = new com.qanairy.models.Test();
		test.setKey("TempTestKey");
		Page page = null;
		try {
			page = new Page("<html><body></body></html>",
							"http://www.test.test", "", new ArrayList<PageElement>(), true);
		} catch (IOException e) {
			Assert.assertFalse(true);
		}
		Path path = new Path();
		path.add(page);
		
		test.setPath(path);
		test.setResult(page);
		TestRecord test_record = new TestRecord(new Date(), true, page);
		
		test_record = test_record_repo.create(orient_connection, test_record);
		TestRecord test_record_record = test_record_repo.find(orient_connection, test_record.getKey());
		
		Assert.assertTrue(test_record_record.getKey().equals(test_record.getKey()));
	}
}
