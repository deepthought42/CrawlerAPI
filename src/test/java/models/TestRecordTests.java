package models;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;
import com.qanairy.models.Page;
import com.qanairy.models.PageElement;
import com.qanairy.models.Path;
import com.qanairy.models.ScreenshotSet;
import com.qanairy.models.TestRecord;
import com.qanairy.models.dto.TestRecordRepository;
import com.qanairy.persistence.ITestRecord;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * 
 *
 */
public class TestRecordTests {
	
	@Test(groups="Regression")
	public void testRecordCreateRecord(){
		List<ScreenshotSet> browser_screenshots = new ArrayList<ScreenshotSet>();
		String browser_name = "chrome";

		browser_screenshots.add(new ScreenshotSet("fulltestscreenshot.com", "testscreenshoturl.com", browser_name));
		
		TestRecordRepository test_record_repo = new TestRecordRepository();
		com.qanairy.models.Test test = new com.qanairy.models.Test();
		test.setKey("TempTestKey");
		Page page = null;
		try {
			page = new Page("<html><body></body></html>",
							"http://www.test.test", browser_screenshots, 
							new ArrayList<PageElement>(), true);
		} catch (IOException e) {
			Assert.assertFalse(true);
		}
		Path path = new Path();
		path.add(page);
		
		test.setPath(path);
		test.setResult(page);
		TestRecord test_record = new TestRecord(new Date(), true, browser_name, page, -1L);	
		ITestRecord test_record_record = test_record_repo.save(new OrientConnectionFactory(), test_record);
		
		Assert.assertTrue(test_record_record.getPassing().equals(test_record.getPassing()));
	}
	
	@Test(groups="Regression")
	public void testRecordUpdateRecord(){
		List<ScreenshotSet> browser_screenshots = new ArrayList<ScreenshotSet>();
		String browser_name = "chrome";
		browser_screenshots.add(new ScreenshotSet("fulltestscreenshot.com", "testscreenshoturl.com", browser_name));
		
		TestRecordRepository test_record_repo = new TestRecordRepository();

		com.qanairy.models.Test test = new com.qanairy.models.Test();
		test.setKey("TempTestKey");
		Page page = null;
		try {
			page = new Page("<html><body></body></html>",
							"http://www.test.test", 
							browser_screenshots, 
							new ArrayList<PageElement>(), true);
		} catch (IOException e) {
			Assert.assertFalse(true);
		}
		Path path = new Path();
		path.add(page);
		
		test.setPath(path);
		test.setResult(page);
		TestRecord test_record = new TestRecord(new Date(), true, browser_name, page, -1L);
		ITestRecord saved_test_record = test_record_repo.save(new OrientConnectionFactory(), test_record);
		ITestRecord test_record_record = test_record_repo.save(new OrientConnectionFactory(), test_record);
		
		Assert.assertTrue(test_record_record.getKey().equals(saved_test_record.getKey()));
	}
	
	@Test(groups="Regression")
	public void testRecordFindRecord(){
		List<ScreenshotSet> browser_screenshots = new ArrayList<ScreenshotSet>();
		String browser_name = "chrome";
		browser_screenshots.add(new ScreenshotSet("fulltestscreenshot.com", "testscreenshoturl.com", browser_name));
		
		TestRecordRepository test_record_repo = new TestRecordRepository();

		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		com.qanairy.models.Test test = new com.qanairy.models.Test();
		test.setKey("TempTestKey");
		Page page = null;
		try {
			page = new Page("<html><body></body></html>",
							"http://www.test.test", 
							browser_screenshots, 
							new ArrayList<PageElement>(), true);
		} catch (IOException e) {
			Assert.assertFalse(true);
		}
		Path path = new Path();
		path.add(page);
		
		test.setPath(path);
		test.setResult(page);
		TestRecord test_record = new TestRecord(new Date(), true, browser_name, page, -1L);
		ITestRecord saved_test_record = test_record_repo.save(orient_connection, test_record);
		TestRecord test_record_record = test_record_repo.find(orient_connection, saved_test_record.getKey());
		
		Assert.assertTrue(test_record_record.getKey().equals(saved_test_record.getKey()));
	}
}
