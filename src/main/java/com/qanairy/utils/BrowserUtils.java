package com.qanairy.utils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.openqa.grid.common.exception.GridException;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minion.aws.UploadObjectSingleOperation;
import com.minion.browsing.Browser;
import com.qanairy.models.Animation;
import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.Redirect;

public class BrowserUtils {
	private static Logger log = LoggerFactory.getLogger(BrowserUtils.class);

	public static Redirect getPageTransition(String initial_url, Browser browser, String host) throws GridException, IOException{
		List<String> transition_urls = new ArrayList<String>();
		List<String> image_checksums = new ArrayList<String>();
		List<String> image_urls = new ArrayList<String>();
		List<BufferedImage> images = new ArrayList<>();
		boolean transition_detected = false;
		
		long start_ms = System.currentTimeMillis();
		//while (time passed is less than 30 seconds AND transition has occurred) or transition_detected && loop not detected
		
		URL init_url = new URL(initial_url);
		String last_key = init_url.getProtocol()+"://"+init_url.getHost()+init_url.getPath();
		do{
			String new_key = browser.getDriver().getCurrentUrl();
			URL new_url = new URL(new_key);
			new_key = new_url.getProtocol()+"://"+new_url.getHost()+new_url.getPath();

			try{
	        	BufferedImage img = browser.getViewportScreenshot();
				images.add(img);
			}catch(Exception e){}
	        
	        transition_detected = !new_key.equals(last_key);
	        
			if(transition_detected ){
				log.warn("redirect transition detected");
				start_ms = System.currentTimeMillis();
				transition_urls.add(new_key);
				last_key = new_key;
			}
			//transition is detected if keys are different
		}while((System.currentTimeMillis() - start_ms) < 2000);
		
		for(BufferedImage img : images){
			try{
				String new_checksum = PageState.getFileChecksum(img);
				image_checksums.add(new_checksum);
				image_urls.add(UploadObjectSingleOperation.saveImageToS3(img, host, new_checksum, browser.getBrowserName()));
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
		
		Redirect redirect = new Redirect(initial_url, transition_urls);
		redirect.setImageChecksums(image_checksums);
		redirect.setImageUrls(image_urls);

		return redirect;
	}

	public static Animation getElementAnimation(Browser browser, ElementState element, String host, WebElement web_element) throws IOException {
		List<String> image_checksums = new ArrayList<String>();
		List<String> image_urls = new ArrayList<String>();
		List<BufferedImage> images = new ArrayList<>();
		boolean transition_detected = false;
		
		long start_ms = System.currentTimeMillis();
		//while (time passed is less than 30 seconds AND transition has occurred) or transition_detected && loop not detected
		
		String last_checksum = element.getScreenshotChecksum();
		int iterations = 0;
		do{
			//get element screenshot
			BufferedImage element_screenshot = browser.getElementScreenshot(web_element);
			
			//calculate screenshot checksum
			String new_checksum = PageState.getFileChecksum(element_screenshot);
			
			transition_detected = !new_checksum.equals(last_checksum);
			
			if(transition_detected ){
				start_ms = System.currentTimeMillis();
				image_checksums.add(new_checksum);								
				last_checksum = new_checksum;
				iterations=0;
			}
			iterations++;

			//transition is detected if keys are different
		}while(iterations < 2 && (System.currentTimeMillis() - start_ms) < 5000);
		
		int idx = 0;
		for(BufferedImage img : images){
			try{
				image_urls.add(UploadObjectSingleOperation.saveImageToS3(img, host, image_checksums.get(idx), browser.getBrowserName()));
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
		
		return new Animation(image_urls, element);
	}
}
