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
import com.qanairy.models.dao.impl.PageElementDaoImpl;
import com.qanairy.models.dao.impl.PageStateDaoImpl;
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
			PageElementDaoImpl page_elem_dao = new PageElementDaoImpl();
			PageStateDaoImpl page_dao = new PageStateDaoImpl();
			List<String> path_keys = new ArrayList<String>();
			path_keys.add(page_dao.generateKey(page));
			path_keys.add(page_elem_dao.generateKey(page_elem));
			
			List<PathObject> path_nodes = new ArrayList<PathObject>();
			path_nodes.add(page);
			path_nodes.add(page_elem);
			
			//new Domain("http", "www.test.test", "chrome", "")
			test = new com.qanairy.models.TestPOJO(path_keys, path_nodes, page, "Testing Test 2");
			test.setKey(test_dao.generateKey(test));
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
			
			PageStateDaoImpl page_dao = new PageStateDaoImpl();
			List<String> path_keys = new ArrayList<String>();
			path_keys.add(page_dao.generateKey(page));
			
			List<PathObject> path_objects = new ArrayList<PathObject>();
			path_objects.add(page);
			
			//new Domain("http", "www.test.test", "chrome", null);
			test = new com.qanairy.models.TestPOJO(path_keys, path_objects, page, "Testing Test 2");
			test.setCorrect(true);
			test.setKey(test_dao.generateKey(test));
			com.qanairy.persistence.Test test_record = test_dao.save(test);
			
			Assert.assertEquals(test_record.getKey(), test.getKey());
			Assert.assertEquals(test_record.getName(), test.getName());
			Assert.assertEquals(test_record.getCorrect(), test.getCorrect());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
