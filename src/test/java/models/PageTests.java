package models;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.minion.browsing.Browser;
import com.minion.browsing.Crawler;
import com.qanairy.models.Page;
import com.qanairy.models.PageElement;
import com.qanairy.models.dto.PageRepository;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * 
 *
 */
public class PageTests {
	
	@Test(groups="Regression")
	public void pageCreateRecord(){
		Map<String, String> browser_screenshots = new HashMap<String, String>();
		browser_screenshots.put("chrome", "testscreenshoturl.com");
		Page page;
		try {
			page = new Page("<html></html>",
							"http://www.test.test", 
							browser_screenshots,
							new ArrayList<PageElement>(), 
							false);
			PageRepository page_repo = new PageRepository();
			
			Page page_record = page_repo.create(new OrientConnectionFactory(), page);
			
			Assert.assertTrue(page_record.getKey().equals(page.getKey()));
			Assert.assertTrue(page_record.getElementCounts().keySet().size() == page.getElementCounts().keySet().size());
			Assert.assertTrue(page_record.getImageWeight() == page.getImageWeight());
			Assert.assertTrue(page_record.getTotalWeight() == page.getTotalWeight());
			
			//assert each element matches
			for(String browser : page_record.getBrowserScreenshots().keySet()){
				Assert.assertTrue(page.getBrowserScreenshots().containsKey(browser));	
			}
			
			Assert.assertTrue(page_record.getType().equals(page.getType()));
			Assert.assertTrue(page_record.isLandable() == page.isLandable());
			Assert.assertTrue(page_record.getKey().equals(page.getKey()));
			//Assert.assertTrue(page_record.getSrc().getSrc().equals(page.getSrc().getSrc()));
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}
	
	@Test(groups="Regression")
	public void pageUpdateRecord(){
		Map<String, String> browser_screenshots = new HashMap<String, String>();
		browser_screenshots.put("chrome", "testscreenshoturl.com");
		try {
			Page page = new Page("<html></html>",
								 "http://www.test.test", 
								 browser_screenshots, 
								 new ArrayList<PageElement>(), 
								 false);
			PageRepository page_repo = new PageRepository();
			
			Page page_record = page_repo.update(new OrientConnectionFactory(), page);
			
			Assert.assertTrue(page_record.getKey().equals(page_repo.generateKey(page)));
			Assert.assertTrue(page_record.getElementCounts().keySet().size() == page.getElementCounts().keySet().size());
			Assert.assertTrue(page_record.getImageWeight() == page.getImageWeight());
			Assert.assertTrue(page_record.getTotalWeight() == page.getTotalWeight());
			//Assert.assertTrue(page_record.getScreenshot().equals(page.getScreenshot()));
			Assert.assertTrue(page_record.getType().equals(page.getType()));
			Assert.assertTrue(page_record.isLandable() == page.isLandable());
			//Assert.assertTrue(page_record.getSrc().getSrc().equals(page.getSrc().getSrc()));
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail();
		}
		
	}
	
	@Test(groups="Regression")
	public void pageFindRecord(){
		Map<String, String> browser_screenshots = new HashMap<String, String>();
		browser_screenshots.put("chrome", "testscreenshoturl.com");
		
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();

		Page page;
		try {
			page = new Page("<html><body></body></html>",
							"http://www.test11.test", 
							browser_screenshots, 
							new ArrayList<PageElement>(), 
							true);
			PageRepository page_repo = new PageRepository();

			page = page_repo.create(orient_connection, page);
			Page page_record = page_repo.find(orient_connection, page.getKey());
			
			Assert.assertTrue(page_record.getKey().equals(page.getKey()));
			Assert.assertTrue(page_record.getElementCounts().keySet().size() == page.getElementCounts().keySet().size());
			Assert.assertTrue(page_record.getImageWeight() == page.getImageWeight());
			Assert.assertTrue(page_record.getTotalWeight() == page_record.getTotalWeight());
			//Assert.assertTrue(page_record.getScreenshot().equals(page.getScreenshot()));
			Assert.assertTrue(page_record.getType().equals(page.getType()));
			Assert.assertTrue(page_record.isLandable() == page.isLandable());
			Assert.assertTrue(page_record.getKey().equals(page.getKey()));
			Assert.assertTrue(page_record.getSrc().equals(page.getSrc()));
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}
}
