package com.qanairy.utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minion.browsing.Browser;

public class BrowserUtils {
	private static Logger log = LoggerFactory.getLogger(BrowserUtils.class);

	public static List<String> getPageTransition(Browser browser) throws MalformedURLException{
		List<String> transition_keys = new ArrayList<String>();
		boolean transition_detected = false;
		
		long start_ms = System.currentTimeMillis();
		//while (time passed is less than 30 seconds AND transition has occurred) or transition_detected && loop not detected
		String last_key = null;
		do{
			URL url = new URL(browser.getDriver().getCurrentUrl());
			String url_string = url.getProtocol()+"://"+url.getHost()+"/"+url.getPath();
			
			int element_count = browser.getDriver().findElements(By.xpath("//*")).size();
			String new_key = url_string+":"+element_count;
			transition_detected = (last_key != null && !new_key.equals(last_key));
			last_key = new_key;
			
			if(transition_detected){
				start_ms = System.currentTimeMillis();
				transition_keys.add(new_key);
			}
			//transition is detected if keys are different
		}while((System.currentTimeMillis() - start_ms) < 3000);
		
		return transition_keys;
	}
}
