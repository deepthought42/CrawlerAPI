package models;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
	@Test(groups="Regression")
	public void testCreateRecord(){
		Map<String, String> browser_screenshots = new HashMap<String, String>();
		browser_screenshots.put("chrome", "testscreenshoturl.com");
		
		com.qanairy.models.Test test;
		try {
			TestRepository test_repo = new TestRepository();
			Page page = new Page("<html></html>",
								 "http://www.test.test", browser_screenshots, new ArrayList<PageElement>(), true);
			Path path = new Path();
			path.add(page);
			test = new com.qanairy.models.Test(path, page, new Domain("www.test.test", "", "http"), "Testing Test 2");
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
	@Test(groups="Regression")
	public void testUpdateRecord(){
		Map<String, String> browser_screenshots = new HashMap<String, String>();
		browser_screenshots.put("chrome", "testscreenshoturl.com");
		
		com.qanairy.models.Test test;
		try {
			TestRepository test_repo = new TestRepository();
			Page page = new Page("<html><body></body></html>",
								 "http://www.test.test", browser_screenshots, 
								 new ArrayList<PageElement>(), true);
			Path path = new Path();
			path.add(page);
			test = new com.qanairy.models.Test(path, page, new Domain("www.test.test", "", "http"),"Testing Test 4");
			test.setKey(test_repo.generateKey(test));
			com.qanairy.models.Test test_record_create = test_repo.create(new OrientConnectionFactory(), test);

			com.qanairy.models.Test test_record = test_repo.update(new OrientConnectionFactory(), test_record_create);
			
			Assert.assertTrue(test_record.getKey().equals(test.getKey()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 */
	@Test(groups="Regression")
	public void testFindRecord(){
		Map<String, String> browser_screenshots = new HashMap<String, String>();
		browser_screenshots.put("chrome", "testscreenshoturl.com");
		
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();

		com.qanairy.models.Test test;
		try {
			Page page = new Page("<html></html>",
								 "http://www.test.test", browser_screenshots, new ArrayList<PageElement>(), true);
			Path path = new Path();
			path.add(page);
			TestRepository test_repo = new TestRepository();

			test = new com.qanairy.models.Test(path, page, new Domain("www.test.test", "", "http"), "Testing Test 3");
			test = test_repo.create(orient_connection, test);
			com.qanairy.models.Test test_record = test_repo.find(orient_connection, test.getKey());
			
			Assert.assertTrue(test_record.getKey().equals(test.getKey()));
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
}
