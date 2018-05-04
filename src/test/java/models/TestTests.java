package models;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;
import com.qanairy.models.Domain;
import com.qanairy.models.Page;
import com.qanairy.models.PageElement;
import com.qanairy.models.Path;
import com.qanairy.models.ScreenshotSet;
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
		List<ScreenshotSet> browser_screenshots = new ArrayList<ScreenshotSet>();
		browser_screenshots.add(new ScreenshotSet("fulltestscreenshot.com", "testscreenshoturl.com", "chrome"));
		
		com.qanairy.models.Test test;
		try {
			TestRepository test_repo = new TestRepository();
			Page page = new Page("<html></html>",
								 "http://www.test.test", 
								 browser_screenshots, 
								 new ArrayList<PageElement>(), true);
			Path path = new Path();
			path.add(page);
			test = new com.qanairy.models.Test(path, page, new Domain("http", "www.test.test", "chrome", ""), "Testing Test 2");
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
		List<ScreenshotSet> browser_screenshots = new ArrayList<ScreenshotSet>();
		browser_screenshots.add(new ScreenshotSet("fulltestscreenshot.com", "testscreenshoturl.com", "chrome"));
		
		com.qanairy.models.Test test;
		try {
			TestRepository test_repo = new TestRepository();
			Page page = new Page("<html><body></body></html>",
								 "http://www.test.test", browser_screenshots, 
								 new ArrayList<PageElement>(), true);
			Path path = new Path();
			path.add(page);
			test = new com.qanairy.models.Test(path, page, new Domain("http", "www.test.test", "chrome", null),"Testing Test 4");
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
		List<ScreenshotSet> browser_screenshots = new ArrayList<ScreenshotSet>();
		browser_screenshots.add(new ScreenshotSet("fulltestscreenshot.com", "testscreenshoturl.com", "chrome"));
		
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();

		com.qanairy.models.Test test;
		try {
			Page page = new Page("<html></html>",
								 "http://www.test.test", browser_screenshots, new ArrayList<PageElement>(), true);
			Path path = new Path();
			path.add(page);
			TestRepository test_repo = new TestRepository();

			test = new com.qanairy.models.Test(path, page, new Domain("http", "www.test.test", "chrome", null), "Testing Test 3");
			test = test_repo.create(orient_connection, test);
			com.qanairy.models.Test test_record = test_repo.find(orient_connection, test.getKey());
			
			Assert.assertTrue(test_record.getKey().equals(test.getKey()));
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
}
