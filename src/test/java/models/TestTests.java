package models;

import java.io.IOException;
import java.util.ArrayList;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.qanairy.models.Domain;
import com.qanairy.models.Page;
import com.qanairy.models.PageElement;
import com.qanairy.models.Path;
import com.qanairy.models.dto.TestRepository;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * 
 *
 */
public class TestTests {
	
	/**
	 * 
	 */
	@Test
	public void testCreateRecord(){
		com.qanairy.models.Test test;
		try {
			TestRepository test_repo = new TestRepository();
			test = new com.qanairy.models.Test(new Path(), new Page("<html></html>","http://www.test.test", "", new ArrayList<PageElement>(), true), new Domain("http://www.test.test"));
			test.setKey(test_repo.generateKey(test));
			com.qanairy.models.Test test_record = test_repo.create(new OrientConnectionFactory(), test);
			
			Assert.assertTrue(test_record.getKey().equals(test.getKey()));
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	/**
	 * 
	 */
	@Test
	public void testUpdateRecord(){
		com.qanairy.models.Test test;
		try {
			TestRepository test_repo = new TestRepository();

			test = new com.qanairy.models.Test(new Path(), new Page("<html></html>","http://www.test.test", "", new ArrayList<PageElement>(), true), new Domain("http://www.test.test"));
			test.setKey(test_repo.generateKey(test));
			com.qanairy.models.Test test_record = test_repo.update(new OrientConnectionFactory(), test);
			
			Assert.assertTrue(test_record.getKey().equals(test.getKey()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 */
	@Test
	public void testFindRecord(){
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();

		com.qanairy.models.Test test;
		try {
			TestRepository test_repo = new TestRepository();

			test = new com.qanairy.models.Test(new Path(), new Page("<html></html>","http://www.test.test", "", new ArrayList<PageElement>(), true), new Domain("http://www.test.test"));
			test = test_repo.create(orient_connection, test);
			com.qanairy.models.Test test_record = test_repo.find(orient_connection, test.getKey());
			
			Assert.assertTrue(test_record.getKey().equals(test.getKey()));
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
}
