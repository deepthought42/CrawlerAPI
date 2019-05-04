package com.qanairy.utils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.openqa.grid.common.exception.GridException;
import org.openqa.selenium.By;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minion.aws.UploadObjectSingleOperation;
import com.minion.browsing.Browser;
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
		int last_elem_count = 0;
		int iterations = 0;
		do{
			String new_key = browser.getDriver().getCurrentUrl();
			int element_count = browser.getDriver().findElements(By.xpath("//*")).size();
			URL new_url = new URL(new_key);
			new_key = new_url.getProtocol()+"://"+new_url.getHost()+new_url.getPath();
	        
	        transition_detected = !new_key.equals(last_key) || element_count != last_elem_count;

			if(transition_detected ){
				try{
					BufferedImage screenshot = browser.getViewportScreenshot();
					images.add(screenshot);
				}catch(Exception e){}
				//start_ms = System.currentTimeMillis();
				if(!new_key.equals(last_key)){
					transition_urls.add(new_key);
					last_key = new_key;
				}
				iterations=0;
			}
			iterations++;
			//transition is detected if keys are different
		}while(iterations < 5 && (System.currentTimeMillis() - start_ms) < 10000);
		
		int idx = 0;
		for(BufferedImage img : images){
			try{
				String new_checksum = PageState.getFileChecksum(img);
				image_checksums.add(new_checksum);

				if(idx > transition_urls.size()){
					idx = transition_urls.size()-1;
				}
				image_urls.add(UploadObjectSingleOperation.saveImageToS3(img, host, image_checksums.get(idx), browser.getBrowserName()));
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

	public static void getElementAnimation(Browser browser, ElementState element, String host) throws IOException {
		// TODO Auto-generated method stub
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
			BufferedImage element_screenshot = browser.getElementScreenshot(browser.getDriver().findElement(By.xpath(element.getXpath())));
			
			//calculate screenshot checksum
			String new_checksum = PageState.getFileChecksum(element_screenshot);
			
			//check for cycle in checksums
			boolean cycle_detected = false;
			for(String checksum : image_checksums){
				if(new_checksum.equals(checksum)){
					cycle_detected = true;
					log.warn("cycle detected in animation");
					break;
				}
			}
			if(cycle_detected){
				break;
			}
			transition_detected = !new_checksum.equals(last_checksum);
			
			if(!cycle_detected && transition_detected ){
				//start_ms = System.currentTimeMillis();
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
	}
}
