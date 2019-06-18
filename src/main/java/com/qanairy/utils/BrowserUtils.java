package com.qanairy.utils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.openqa.grid.common.exception.GridException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minion.aws.UploadObjectSingleOperation;
import com.minion.browsing.Browser;
import com.qanairy.models.Animation;
import com.qanairy.models.PageState;
import com.qanairy.models.Redirect;
import com.qanairy.models.Transition;
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

		URL init_url = new URL(initial_url);
		String last_key = init_url.getHost()+init_url.getPath();
		if(last_key.charAt(last_key.length()-1) == '/'){
			last_key = last_key.substring(0, last_key.length()-1);
		}
		transition_urls.add(last_key);
		do{
			String domain = browser.getDriver().getCurrentUrl();
			if(domain.charAt(0) != 'h'){
				domain= 'h'+domain;
			}
			URL new_url = new URL(domain);

			String new_host = new_url.getHost();
			if(!new_host.startsWith("www.")){
				new_host = "www."+new_host;
			}
			String new_key = new_host+new_url.getPath();
			if(new_key.charAt(new_key.length()-1) == '/'){
				new_key = new_key.substring(0, new_key.length()-1);
			}

	    transition_detected = !new_key.equals(last_key);

			if( transition_detected ){
				try{
		        	BufferedImage img = browser.getViewportScreenshot();
					images.add(img);
				}catch(Exception e){}
				log.warn("redirect transition detected");
				start_ms = System.currentTimeMillis();
				transition_urls.add(new_key);
				last_key = new_key;
			}
			//transition is detected if keys are different
		}while((System.currentTimeMillis() - start_ms) < 1000);

		log.warn("uploading screenshots " );
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

		log.warn("creating redirect object");
		Redirect redirect = new Redirect(initial_url, transition_urls);
		redirect.setImageChecksums(image_checksums);
		redirect.setImageUrls(image_urls);

		return redirect;
	}

	public static Transition getTransition(String initial_url, Browser browser, String host) throws GridException, IOException{
		List<String> transition_urls = new ArrayList<String>();
		List<String> image_checksums = new ArrayList<String>();
		List<String> image_urls = new ArrayList<String>();

		boolean url_transition_detected = false;
		boolean animated_transition_detected = false;
		boolean animated_flag = false;
		boolean continuous_animation_flag = false;

		long start_ms = System.currentTimeMillis();
		//while (time passed is less than 30 seconds AND transition has occurred) or transition_detected && loop not detected

		URL init_url = new URL(initial_url);
		String last_key = init_url.getHost()+init_url.getPath();
		if(last_key.charAt(last_key.length()-1) == '/'){
			last_key = last_key.substring(0, last_key.length()-1);
		}

		String last_checksum = null;
		Map<String, Boolean> animated_state_checksum_hash = new HashMap<String, Boolean>();
		List<Future<String>> url_futures = new ArrayList<>();
		int cycles_detected = 0;

		do{
			//check new url
			String new_key = browser.getDriver().getCurrentUrl();
			if(new_key.charAt(0) != 'h'){
				new_key = 'h'+new_key;
			}
			URL new_url = new URL("http://"+new_key);
			new_key = new_url.getHost()+new_url.getPath();
			if(new_key.charAt(new_key.length()-1) == '/'){
				new_key = new_key.substring(0, new_key.length()-1);
			}

			//check for animation
			//
			//get element screenshot
			BufferedImage screenshot = browser.getViewportScreenshot();

			//calculate screenshot checksum
			String new_checksum = PageState.getFileChecksum(screenshot);

			animated_transition_detected = !new_checksum.equals(last_checksum);
	        url_transition_detected = !new_key.equals(last_key);

			log.warn("transition new checksum :: " + new_checksum);
			log.warn("has transition key been seen before :: " + animated_state_checksum_hash.containsKey(new_checksum));
			if( animated_state_checksum_hash.containsKey(new_checksum) && cycles_detected >= 10 ){
				if(animated_state_checksum_hash.keySet().size() > 1 && animated_transition_detected){
					animated_flag = true;
					continuous_animation_flag = false;
				}
				else{
					continuous_animation_flag = true;
					animated_flag = false;
				}
				break;
			}
			else if( animated_state_checksum_hash.containsKey(new_checksum) && cycles_detected < 10 ){
				cycles_detected++;
			}
			else if( animated_transition_detected ){
				start_ms = System.currentTimeMillis();
				image_checksums.add(new_checksum);
				animated_state_checksum_hash.put(new_checksum, Boolean.TRUE);
				last_checksum = new_checksum;
				url_futures.add(ScreenshotUploadService.uploadPageStateScreenshot(screenshot, host, new_checksum));
			}
			else if( url_transition_detected ){
				log.warn("redirect transition detected");
				start_ms = System.currentTimeMillis();
				transition_urls.add(new_key);
				url_futures.add(ScreenshotUploadService.uploadPageStateScreenshot(screenshot, host, new_checksum));

				last_key = new_key;
			}
			//transition is detected if keys are different
		}while((System.currentTimeMillis() - start_ms) < 30000);

		for(Future<String> future: url_futures){
			try {
				String url = future.get();
				log.warn("Getting future response ::  "+url);
				image_urls.add(url);
			} catch (InterruptedException e) {
				log.debug(e.getMessage());
			} catch (ExecutionException e) {
				log.debug(e.getMessage());
			}
		}

		log.warn("url transition detected  :: " + url_transition_detected);
		log.warn("transition urls ::   "  + transition_urls.size());
		log.warn("animation detected :: " + animated_transition_detected);

		if(transition_urls.size() > 1){
			log.warn("Redirect being returned");
			Redirect redirect = new Redirect(initial_url, transition_urls);
			redirect.setImageChecksums(image_checksums);
			redirect.setImageUrls(image_urls);
			return redirect;
		}
		else if(animated_flag){
			log.warn("Animation :: " + animated_flag);
			return new Animation(image_urls, continuous_animation_flag, new ArrayList<>(animated_state_checksum_hash.keySet()));
		}
		else if(continuous_animation_flag){
			log.warn("continuous animation  :   " +continuous_animation_flag);
			return new Animation(image_urls, continuous_animation_flag, new ArrayList<>(animated_state_checksum_hash.keySet()));
		}

		return null;
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

			log.warn("new checksum :: " + new_checksum);
			log.warn("has key been seen before :: " + animated_state_checksum_hash.containsKey(new_checksum));
			if( animated_state_checksum_hash.containsKey(new_checksum)){
				break;
			}
			else if( transition_detected ){
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

		return new Animation(image_urls, true, image_checksums);
	}
}
