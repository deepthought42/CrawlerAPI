package com.qanairy.utils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openqa.grid.common.exception.GridException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minion.aws.UploadObjectSingleOperation;
import com.minion.browsing.Browser;
import com.qanairy.models.Animation;
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
		String last_key = init_url.getHost()+init_url.getPath();
		if(last_key.charAt(last_key.length()-1) == '/'){
			last_key = last_key.substring(0, last_key.length()-1);
		}
		transition_urls.add(last_key);
		do{
			String new_key = browser.getDriver().getCurrentUrl();
			if(new_key.charAt(0) != 'h'){
				new_key = 'h'+new_key;
			}
			URL new_url = new URL("http://"+new_key);
			new_key = new_url.getHost()+new_url.getPath();
			if(new_key.charAt(new_key.length()-1) == '/'){
				new_key = new_key.substring(0, new_key.length()-1);
			}
			
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
		}while((System.currentTimeMillis() - start_ms) < 1000);
		
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

	public static Animation getAnimation(Browser browser, String host) throws IOException {
		List<String> image_checksums = new ArrayList<String>();
		List<String> image_urls = new ArrayList<String>();
		boolean transition_detected = false;
		
		long start_ms = System.currentTimeMillis();
		//while (time passed is less than 30 seconds AND transition has occurred) or transition_detected && loop not detected
		
		Map<String, BufferedImage> animated_state_imgs = new HashMap<String, BufferedImage>();
		String last_checksum = null;
		int index = 0;

		do{
			//get element screenshot
			BufferedImage screenshot = browser.getViewportScreenshot();
			
			//calculate screenshot checksum
			String new_checksum = PageState.getFileChecksum(screenshot);
			
			transition_detected = !new_checksum.equals(last_checksum);
			
			log.warn("new checksum :: " + new_checksum);
			log.warn("has key been seen before :: " + animated_state_imgs.containsKey(new_checksum));
			if( animated_state_imgs.containsKey(new_checksum)){
				break;
			}
			else if( transition_detected ){
				start_ms = System.currentTimeMillis();
				image_checksums.add(new_checksum);
				animated_state_imgs.put(new_checksum, screenshot);
				last_checksum = new_checksum;
				index++;
			}

			//transition is detected if keys are different
		}while((System.currentTimeMillis() - start_ms) < 10000 && index < 10);
				
		for(Map.Entry<String, BufferedImage> entry : animated_state_imgs.entrySet()){
			try{
				image_urls.add(UploadObjectSingleOperation.saveImageToS3(entry.getValue(), host, entry.getKey(), browser.getBrowserName()));
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
		
		return new Animation(image_urls);
	}
}
