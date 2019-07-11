package com.qanairy.utils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.openqa.grid.common.exception.GridException;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minion.aws.UploadObjectSingleOperation;
import com.minion.browsing.Browser;
import com.qanairy.models.Animation;
import com.qanairy.models.ElementState;
import com.qanairy.models.PageLoadAnimation;
import com.qanairy.models.PageState;
import com.qanairy.models.Redirect;
import com.qanairy.models.enums.AnimationType;
import com.qanairy.services.ScreenshotUploadService;


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

		String last_key = sanitizeUrl(initial_url);
		
		transition_urls.add(last_key);
		do{
			String new_key = sanitizeUrl(browser.getDriver().getCurrentUrl());

			transition_detected = !new_key.equals(last_key);

			if( transition_detected ){
				try{
		        	BufferedImage img = browser.getViewportScreenshot();
					images.add(img);
				}catch(Exception e){}
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

	public static Animation getAnimation(Browser browser, String host) throws IOException {
		List<String> image_checksums = new ArrayList<String>();
		List<String> image_urls = new ArrayList<String>();
		boolean transition_detected = false;
		
		long start_ms = System.currentTimeMillis();
		//while (time passed is less than 30 seconds AND transition has occurred) or transition_detected && loop not detected

		Map<String, Boolean> animated_state_checksum_hash = new HashMap<String, Boolean>();
		String last_checksum = null;
		List<Future<String>> url_futures = new ArrayList<>();
		do{
			//get element screenshot
			BufferedImage screenshot = browser.getViewportScreenshot();

			//calculate screenshot checksum
			String new_checksum = PageState.getFileChecksum(screenshot);

			transition_detected = !new_checksum.equals(last_checksum);

			if( transition_detected ){
				if( animated_state_checksum_hash.containsKey(new_checksum)){
					break;
				}
				start_ms = System.currentTimeMillis();
				image_checksums.add(new_checksum);
				animated_state_checksum_hash.put(new_checksum, Boolean.TRUE);
				last_checksum = new_checksum;
				url_futures.add(ScreenshotUploadService.uploadPageStateScreenshot(screenshot, host, new_checksum));
			}

			//transition is detected if keys are different
		}while((System.currentTimeMillis() - start_ms) < 10000);

		for(Future<String> future: url_futures){
			try {
				image_urls.add(future.get());
			} catch (InterruptedException e) {
				log.debug(e.getMessage());
			} catch (ExecutionException e) {
				log.debug(e.getMessage());
			}
		}
		
		return new Animation(image_urls, image_checksums, AnimationType.CONTINUOUS);
	}	
	
	public static PageLoadAnimation getLoadingAnimation(Browser browser, String host) throws IOException {
		List<String> image_checksums = new ArrayList<String>();
		List<String> image_urls = new ArrayList<String>();
		boolean transition_detected = false;
		long start_ms = System.currentTimeMillis();
		long total_time = System.currentTimeMillis();
		
		Map<String, Boolean> animated_state_checksum_hash = new HashMap<String, Boolean>();
		String last_checksum = null;
		String new_checksum = null;
		List<Future<String>> url_futures = new ArrayList<>();

		do{
			//get element screenshot
			BufferedImage screenshot = browser.getViewportScreenshot();

			//calculate screenshot checksum
			new_checksum = PageState.getFileChecksum(screenshot);

			transition_detected = !new_checksum.equals(last_checksum);

			if( transition_detected ){
				if(animated_state_checksum_hash.containsKey(new_checksum)){
					return null;
				}
				start_ms = System.currentTimeMillis();
				image_checksums.add(new_checksum);
				animated_state_checksum_hash.put(new_checksum, Boolean.TRUE);
				last_checksum = new_checksum;
				url_futures.add(ScreenshotUploadService.uploadPageStateScreenshot(screenshot, host, new_checksum));
			}

			//transition is detected if keys are different
		}while((System.currentTimeMillis() - start_ms) < 3000 && (System.currentTimeMillis() - total_time) < 10000);

		for(Future<String> future: url_futures){
			try {
				image_urls.add(future.get());
			} catch (InterruptedException e) {
				log.debug(e.getMessage());
			} catch (ExecutionException e) {
				log.debug(e.getMessage());
			}
		}
		
		if(!transition_detected && new_checksum.equals(last_checksum) && image_checksums.size()>1){
			return new PageLoadAnimation(image_urls, image_checksums, BrowserUtils.sanitizeUrl(browser.getDriver().getCurrentUrl()));
		}

		return null;
	}
	
	public static String sanitizeUrl(String url) throws MalformedURLException {
		String domain = url;
		int param_index = domain.indexOf("?");
		if(param_index >= 0){
			domain = domain.substring(0, param_index);
		}
		
		if(domain.charAt(0) != 'h'){
			domain= 'h'+domain;
		}
		URL new_url = new URL(domain);

		//check if host is subdomain
		String new_host = new_url.getHost();
		
		int count = new_host.split("\\.").length;
		if(count <= 2 && !new_host.startsWith("www.")){
			new_host = "www."+new_host;
		}
		String new_key = new_host+new_url.getPath();
		if(new_key.charAt(new_key.length()-1) == '/'){
			new_key = new_key.substring(0, new_key.length()-1);
		}
		return new_url.getProtocol()+"://"+new_key;
	}

	public static ElementState updateElementLocations(Browser browser, ElementState element) {
		
		WebElement web_elem = browser.findWebElementByXpath(element.getXpath());
		Point location = web_elem.getLocation();
		if(location.getX() != element.getXLocation() || location.getY() != element.getYLocation()){
			log.warn("updating element state from ::  ( " + element.getXLocation() + " , "+element.getYLocation()+" ) " + " to    ::  ( " + location.getX() + " , "+location.getY()+")");
			element.setXLocation(location.getX());
			element.setYLocation(location.getY());
		}
		
		return element;
	}

	public static boolean doesHostChange(List<String> urls) throws MalformedURLException {
		for(String url : urls){
			String last_host_and_path = "";
			URL url_obj = new URL(url);
			String host_and_path = url_obj.getHost()+url_obj.getPath();
			if(!last_host_and_path.equals(host_and_path)){
				return true;
			}
		}
		return false;
	}
}
