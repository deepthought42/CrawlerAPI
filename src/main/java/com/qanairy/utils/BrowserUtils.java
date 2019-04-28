package com.qanairy.utils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.openqa.grid.common.exception.GridException;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minion.aws.UploadObjectSingleOperation;
import com.minion.browsing.Browser;
import com.qanairy.models.PageState;
import com.qanairy.models.Redirect;

public class BrowserUtils {
	private static Logger log = LoggerFactory.getLogger(BrowserUtils.class);

	public static Redirect getPageTransition(String initial_url, Browser browser){
		log.warn("starting check for page redirect");
		List<String> transition_urls = new ArrayList<String>();
		List<String> image_checksums = new ArrayList<String>();
		List<String> image_urls = new ArrayList<String>();
		List<BufferedImage> images = new ArrayList<>();
		boolean transition_detected = false;
		
		long start_ms = System.currentTimeMillis();
		//while (time passed is less than 30 seconds AND transition has occurred) or transition_detected && loop not detected
		
		String last_key = initial_url;
		int iterations = 0;
		do{
			log.warn("Getting page transition..");
			try{
				WebDriverWait wait = new WebDriverWait(browser.getDriver(), 2);
				wait.until(ExpectedConditions.not(ExpectedConditions.urlMatches(last_key)));
			}catch(TimeoutException e){
				log.warn(e.getMessage());
			}
			log.warn("getting current url ");
			
			String new_key = browser.getDriver().getCurrentUrl();
			
			int params_idx = new_key.indexOf("?");
			if(params_idx > -1){
				new_key = new_key.substring(0, params_idx);
			}
			
			params_idx = new_key.indexOf("?");
			if(params_idx > -1){
				last_key = last_key.substring(0, params_idx);
			}
			transition_detected = !(new_key.equals(last_key));

			log.warn("was transition detected :: " + transition_detected+"  ....."+new_key+" :::  "+last_key);
			if(transition_detected ){
				start_ms = System.currentTimeMillis();
				
				transition_urls.add(new_key);
				
				log.warn("does last key = new key :: " + "  ....."+new_key+" :::  "+last_key);
				
				last_key = new_key;
				try {
					BufferedImage screenshot = browser.getViewportScreenshot();
					images.add(screenshot);
				} catch (GridException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				log.warn("Resetting iteration during redirect detection");
				iterations=0;
			}
			log.warn("current iteration value :: " + iterations);
			iterations++;

			//transition is detected if keys are different
		}while(iterations < 2 && (System.currentTimeMillis() - start_ms) < 30000);
		
		log.warn("redirect detection complete. uploading screenshots");
		int idx = 0;
		for(BufferedImage img : images){
			try {
				image_checksums.add(PageState.getFileChecksum(img));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(idx > transition_urls.size()){
				idx = transition_urls.size()-1;
			}
			image_urls.add(UploadObjectSingleOperation.saveImageToS3(img, transition_urls.get(idx), image_checksums.get(idx), browser.getBrowserName()));
		}
		log.warn("uploaded all images :: " + image_urls.size());
		int params_idx = initial_url.indexOf("?");

		if(params_idx > -1){
			initial_url = initial_url.substring(0, params_idx);
		}
		Redirect redirect = new Redirect(initial_url, transition_urls);
		redirect.setImageChecksums(image_checksums);
		redirect.setImageUrls(image_urls);
		log.warn("created redirect POJO");
		return redirect;
	}
}
