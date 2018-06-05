package models;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.qanairy.models.PageElementPOJO;
import com.qanairy.models.PageStatePOJO;
import com.qanairy.models.ScreenshotSetPOJO;
import com.qanairy.models.dao.impl.TestDaoImpl;
import com.qanairy.persistence.Attribute;
import com.qanairy.persistence.PageElement;
import com.qanairy.persistence.PageState;
import com.qanairy.persistence.PathObject;
import com.qanairy.persistence.ScreenshotSet;

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
		browser_screenshots.add(new ScreenshotSetPOJO("fulltestscreenshot.com", "testscreenshoturl.com", "chrome"));
		
		com.qanairy.persistence.Test test;
		try {
			TestDaoImpl test_dao = new TestDaoImpl();
			PageState page = new PageStatePOJO("<html></html>",
								 "http://www.test.test", 
								 browser_screenshots, 
								 new ArrayList<PageElement>(), 
								 true);
			
			PageElement page_elem = new PageElementPOJO("button test", "//button", "input", new ArrayList<Attribute>(), new HashMap<String, String>());
			List<String> path_keys = new ArrayList<String>();
			path_keys.add(page.getKey());
			path_keys.add(page_elem.getKey());
			
			List<PathObject> path_nodes = new ArrayList<PathObject>();
			path_nodes.add(page);
			path_nodes.add(page_elem);
			
			//new Domain("http", "www.test.test", "chrome", "")
			test = new com.qanairy.models.TestPOJO(path_keys, path_nodes, page, "Testing Test 2");
			test.setKey(test.getKey());
			test.setCorrect(true);
			com.qanairy.persistence.Test test_record = test_dao.save(test);
			
			Assert.assertEquals(test_record.getKey(), test.getKey());
			Assert.assertEquals(test_record.getName(), test.getName());
			Assert.assertEquals(test_record.getCorrect(), test.getCorrect());
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
		browser_screenshots.add(new ScreenshotSetPOJO("fulltestscreenshot.com", "testscreenshoturl.com", "chrome"));
		
		com.qanairy.persistence.Test test;
		try {
			TestDaoImpl test_dao = new TestDaoImpl();
			PageState page = new PageStatePOJO("<html><body></body></html>",
								 "http://www.test.test", 
								 browser_screenshots, 
								 new ArrayList<PageElement>(), 
								 true);
			
			List<String> path_keys = new ArrayList<String>();
			path_keys.add(page.getKey());
			
			List<PathObject> path_objects = new ArrayList<PathObject>();
			path_objects.add(page);
			
			//new Domain("http", "www.test.test", "chrome", null);
			test = new com.qanairy.models.TestPOJO(path_keys, path_objects, page, "Testing Test 2");
			test.setCorrect(true);
			test.setKey(test.getKey());
			com.qanairy.persistence.Test test_record = test_dao.save(test);
			
			Assert.assertEquals(test_record.getKey(), test.getKey());
			Assert.assertEquals(test_record.getName(), test.getName());
			Assert.assertEquals(test_record.getCorrect(), test.getCorrect());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
