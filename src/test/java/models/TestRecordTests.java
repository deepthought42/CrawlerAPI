package models;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;
import com.qanairy.models.PageStatePOJO;
import com.qanairy.models.ScreenshotSetPOJO;
import com.qanairy.models.TestRecordPOJO;
import com.qanairy.models.dao.TestRecordDao;
import com.qanairy.models.dao.impl.TestRecordDaoImpl;
import com.qanairy.persistence.PageElement;
import com.qanairy.persistence.PageState;
import com.qanairy.persistence.ScreenshotSet;
import com.qanairy.persistence.TestRecord;

/**
 * 
 *
 */
public class TestRecordTests {
	
	@Test(groups="Regression")
	public void testRecordSaveRecord(){
		List<ScreenshotSet> browser_screenshots = new ArrayList<ScreenshotSet>();
		String browser_name = "chrome";

		browser_screenshots.add(new ScreenshotSetPOJO("fulltestscreenshot.com", "testscreenshoturl.com", browser_name));
		
		TestRecordDao test_record_dao = new TestRecordDaoImpl();
		PageState page = null;
		try {
			page = new PageStatePOJO("<html><body></body></html>",
							"http://www.test.test", browser_screenshots, 
							new ArrayList<PageElement>(), true);
		} catch (IOException e) {
			Assert.assertFalse(true);
		}
		
		TestRecord test_record = new TestRecordPOJO(new Date(), null, browser_name, page, -1L);	
		TestRecord test_record_record = test_record_dao.save(test_record);
		
		Assert.assertEquals(test_record_record.getPassing(), test_record.getPassing());
		Assert.assertEquals(test_record_record.getBrowser(), test_record.getBrowser());
		Assert.assertEquals(test_record_record.getKey(), test_record.getKey());
		Assert.assertEquals(test_record_record.getRunTime(), test_record.getRunTime());
		Assert.assertEquals(test_record_record.getRanAt(), test_record.getRanAt());
		//Assert.assertTrue(((PageState)test_record_record.getResult()).equals(test_record.getResult()));
	}
}
