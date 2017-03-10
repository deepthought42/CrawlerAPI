package com.minion.systemTesting;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.testng.annotations.Test;

import com.minion.aws.UploadObjectSingleOperation;
import com.minion.browsing.Browser;
import com.minion.structs.Path;
import com.qanairy.models.Page;


public class PathTest {
	
	@Test
	public void createPath(){
		Path path = new Path(null);
		Page page = null;
		try {
			Browser browser = new Browser(null);
			URL page_url = new URL(browser.getDriver().getCurrentUrl());
			page = new Page(browser.getDriver().getPageSource(), 
							browser.getDriver().getCurrentUrl(), 
							UploadObjectSingleOperation.saveImageToS3(Browser.getScreenshot(browser.getDriver()), page_url.getHost(), page_url.getPath().toString()), 
							Browser.getVisibleElements(browser.getDriver(), ""));
			browser.close();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		path.getPath().add(page);
	}
}
