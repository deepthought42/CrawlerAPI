package com.minion.systemTesting;

import java.io.IOException;
import java.net.MalformedURLException;

import org.testng.annotations.Test;

import com.minion.browsing.Browser;
import com.minion.browsing.Page;
import com.minion.structs.Path;


public class PathTest {
	
	@Test
	public void createPath(){
		Path path = new Path(null);
		Page page = null;
		try {
			Browser browser = new Browser(null);

			page = new Page(browser.getDriver().getPageSource(), browser.getDriver().getCurrentUrl(), Browser.getScreenshot(browser.getDriver()), Browser.getVisibleElements(browser.getDriver(), ""));
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
