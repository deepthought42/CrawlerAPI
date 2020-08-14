package com.qanairy.models.journeys;

import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minion.browsing.Browser;

/**
 * A set of Steps
 */
public class NavigationStep extends Step {
	private static Logger log = LoggerFactory.getLogger(NavigationStep.class);

	private String url;
	
	public NavigationStep() {}
	
	public NavigationStep(String url) {
		assert url != null;

		setUrl(url);
		setKey(generateKey());
	}
	
	@Override
	public String generateKey() {
		return "step:"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(url.toString());
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	@Override
	public void execute(Browser browser) {
		assert browser != null;
		
		log.warn("navigation step browser  :: "+browser);
		WebDriver driver = browser.getDriver();
		driver.get(this.url);
	}
}
