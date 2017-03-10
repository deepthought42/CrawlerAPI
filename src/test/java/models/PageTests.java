package models;

import java.io.IOException;
import java.util.ArrayList;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.qanairy.models.Page;
import com.qanairy.models.PageElement;
import com.qanairy.persistence.IPage;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * 
 *
 */
public class PageTests {
	
	@Test
	public void pageCreateRecord(){
		Page page;
		try {
			page = new Page("<html></html>","http://www.test.test", null, new ArrayList<PageElement>(), true);
			IPage page_record = page.create(new OrientConnectionFactory());
			
			Assert.assertTrue(page_record.getKey().equals(page.getKey()));
			Assert.assertTrue(page_record.getElementCounts().keySet().size() == page.getElementCounts().keySet().size());
			Assert.assertTrue(page_record.getImageWeight() == page.getImageWeight());
			Assert.assertTrue(page_record.getTotalWeight() == page_record.getTotalWeight());
			//Assert.assertTrue(page_record.getScreenshot().equals(page.getScreenshot().toString()));
			Assert.assertTrue(page_record.getType().equals(page.getType()));
			Assert.assertTrue(page_record.isLandable() == page.isLandable());
			Assert.assertTrue(page_record.getKey().equals(page.getKey()));
			Assert.assertTrue(page_record.getSrc().equals(page.getSrc()));
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}
	
	@Test
	public void pageUpdateRecord(){
		try {
			Page page = new Page("<html></html>","http://www.test.test", null, new ArrayList<PageElement>(), false);
			IPage page_record = page.update(new OrientConnectionFactory());
			
			Assert.assertTrue(page_record.getKey().equals(page.getKey()));
			Assert.assertTrue(page_record.getElementCounts().keySet().size() == page.getElementCounts().keySet().size());
			Assert.assertTrue(page_record.getImageWeight() == page.getImageWeight());
			Assert.assertTrue(page_record.getTotalWeight() == page.getTotalWeight());
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
	
	@Test
	public void pageFindRecord(){
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();

		Page page;
		try {
			page = new Page("<html></html>","http://www.test.test", null, new ArrayList<PageElement>(), true);
			page.create(orient_connection);
			IPage page_record = page.find(orient_connection);
			
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
