package models;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.qanairy.models.ScreenshotSetPOJO;
import com.qanairy.models.dao.ScreenshotSetDao;
import com.qanairy.models.dao.impl.ScreenshotSetDaoImpl;
import com.qanairy.persistence.ScreenshotSet;

/**
 * Defines all tests for the service package Repository
 */
public class ScreenshotSetTests {
	
	@Test(groups="Regression")
	public void assertScreenshotSetPersists(){
		ScreenshotSet screenshot_set= new ScreenshotSetPOJO("full_url.jpg","viewport_url.jpg","chrome");
		ScreenshotSetDao screenshot_set_dao = new ScreenshotSetDaoImpl();
		screenshot_set_dao.save(screenshot_set);
		
		ScreenshotSet screenshot_set_record = screenshot_set_dao.find(screenshot_set.getKey());
		Assert.assertEquals(screenshot_set_record.getKey(), screenshot_set.getKey());
		Assert.assertEquals(screenshot_set_record.getBrowser(), screenshot_set.getBrowser());
		Assert.assertEquals(screenshot_set_record.getFullScreenshot(), screenshot_set.getFullScreenshot());
		Assert.assertEquals(screenshot_set_record.getViewportScreenshot(), screenshot_set.getViewportScreenshot());
	}
}
