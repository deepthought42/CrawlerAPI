package models;

import java.io.IOException;
import java.util.ArrayList;
import org.testng.Assert;
import org.testng.annotations.Test;
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
		Page page;
		try {
			page = new Page("<html></html>",
							"http://www.test.test", 
							"testscreenshoturl.com",
							new ArrayList<PageElement>(), 
							false);
			PageRepository page_repo = new PageRepository();
			
			Page page_record = page_repo.create(new OrientConnectionFactory(), page);
			
			Assert.assertTrue(page_record.getKey().equals(page.getKey()));
			Assert.assertTrue(page_record.getElementCounts().keySet().size() == page.getElementCounts().keySet().size());
			Assert.assertTrue(page_record.getImageWeight() == page.getImageWeight());
			Assert.assertTrue(page_record.getTotalWeight() == page.getTotalWeight());
			Assert.assertTrue(page_record.getScreenshot().equals(page.getScreenshot().toString()));
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
		try {
			Page page = new Page("<html></html>",
								 "http://www.test.test", 
								 null, 
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
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();

		Page page;
		try {
			page = new Page("<html><body></body></html>",
							"http://www.test11.test", 
							null, 
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
