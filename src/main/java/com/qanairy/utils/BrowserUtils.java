package com.qanairy.utils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.grid.common.exception.GridException;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minion.aws.UploadObjectSingleOperation;
import com.minion.browsing.Browser;
import com.qanairy.models.Animation;
import com.qanairy.models.ElementState;
import com.qanairy.models.LookseeObject;
import com.qanairy.models.PageLoadAnimation;
import com.qanairy.models.PageState;
import com.qanairy.models.Redirect;
import com.qanairy.models.enums.AnimationType;
import com.qanairy.models.enums.BrowserType;
import com.qanairy.services.ScreenshotUploadService;


public class BrowserUtils {
	private static Logger log = LoggerFactory.getLogger(BrowserUtils.class);

	public static Redirect getPageTransition(String initial_url, Browser browser, String host, String user_id) throws GridException, IOException{
		List<String> transition_urls = new ArrayList<String>();
		List<String> image_checksums = new ArrayList<String>();
		List<String> image_urls = new ArrayList<String>();
		List<BufferedImage> images = new ArrayList<>();
		boolean transition_detected = false;

		long start_ms = System.currentTimeMillis();
		//while (time passed is less than 30 seconds AND transition has occurred) or transition_detected && loop not detected

		String last_key = sanitizeUrl(initial_url);
		
		//transition_urls.add(last_key);
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
		}while((System.currentTimeMillis() - start_ms) < 3000);

		for(BufferedImage img : images){
			try{
				String new_checksum = PageState.getFileChecksum(img);
				image_checksums.add(new_checksum);
				image_urls.add(UploadObjectSingleOperation.saveImageToS3ForUser(img, host, new_checksum, BrowserType.create(browser.getBrowserName()), user_id));
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

	public static Animation getAnimation(Browser browser, String host, String user_id) throws IOException {
		List<String> image_checksums = new ArrayList<String>();
		List<String> image_urls = new ArrayList<String>();
		boolean transition_detected = false;
		
		long start_ms = System.currentTimeMillis();

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
				url_futures.add(ScreenshotUploadService.uploadPageStateScreenshot(screenshot, host, new_checksum, BrowserType.create(browser.getBrowserName()), user_id));
			}
		}while((System.currentTimeMillis() - start_ms) < 2000);

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
	
	/**
	 * Watches for an animation that occurs during page load
	 * 
	 * @param browser
	 * @param host
	 * @param user_id TODO
	 * @return
	 * @throws IOException
	 * 
	 * @pre browser != null
	 * @pre host != null
	 * @pre host != empty
	 */
	public static PageLoadAnimation getLoadingAnimation(Browser browser, String host, String user_id) throws IOException {
		assert browser != null;
		assert host != null;
		assert !host.isEmpty();
		
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
				image_checksums.add(new_checksum);
				animated_state_checksum_hash.put(new_checksum, Boolean.TRUE);
				last_checksum = new_checksum;
				url_futures.add(ScreenshotUploadService.uploadPageStateScreenshot(screenshot, host, new_checksum, BrowserType.create(browser.getBrowserName()), user_id));
				start_ms = System.currentTimeMillis();
			}
		}while((System.currentTimeMillis() - start_ms) < 1000 && (System.currentTimeMillis() - total_time) < 10000);

		for(Future<String> future: url_futures){
			try {
				image_urls.add(future.get());
			} catch (InterruptedException e) {
				log.debug(e.getMessage());
			} catch (ExecutionException e) {
				log.debug(e.getMessage());
			}
		}
		
		if(!transition_detected && new_checksum.equals(last_checksum) && image_checksums.size()>2){
			return new PageLoadAnimation(image_urls, image_checksums, BrowserUtils.sanitizeUrl(browser.getDriver().getCurrentUrl()));
		}

		return null;
	}
	
	public static PageLoadAnimation detectShortAnimation(Browser browser, String host, String user_id) throws IOException {
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
				url_futures.add(ScreenshotUploadService.uploadPageStateScreenshot(screenshot, host, new_checksum, BrowserType.create(browser.getBrowserName()), user_id));
			}
		}while((System.currentTimeMillis() - start_ms) < 500 && System.currentTimeMillis()-total_time < 3000);

		for(Future<String> future: url_futures){
			try {
				image_urls.add(future.get());
			} catch (InterruptedException e) {
				log.debug(e.getMessage());
			} catch (ExecutionException e) {
				log.debug(e.getMessage());
			}
		}
		
		if(!transition_detected && new_checksum.equals(last_checksum) && image_checksums.size()>2){
			return new PageLoadAnimation(image_urls, image_checksums, BrowserUtils.sanitizeUrl(browser.getDriver().getCurrentUrl()));
		}

		return null;
	}
	
	public static String sanitizeUrl(String url) {
		String domain = url;
		int param_index = domain.indexOf("?");
		if(param_index >= 0){
			domain = domain.substring(0, param_index);
		}
		
		domain = domain.replace("index.html", "");
		domain = domain.replace("index.htm", "");

		if(!domain.isEmpty() && domain.charAt(domain.length()-1) == '/'){
			domain = domain.substring(0, domain.length()-1);
		}
		
		//remove any anchor link references
		int hash_index = domain.indexOf("#");
		if(hash_index > 0) {
			domain = domain.substring(0, hash_index);
		}
		
		return domain;
	}
	
	public static String sanitizeUserUrl(String url) throws MalformedURLException {
		String domain = url;
		int param_index = domain.indexOf("?");
		if(param_index >= 0){
			domain = domain.substring(0, param_index);
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
		
		new_key = new_key.replace("index.html", "");
		new_key = new_key.replace("index.htm", "");
		
		if(new_key.charAt(new_key.length()-1) == '/') {
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
	
	public static boolean doesSpanMutlipleDomains(String start_url, String end_url, List<LookseeObject> path_objects) throws MalformedURLException {
		return !(start_url.trim().contains(new URL(end_url).getHost()) || end_url.contains((new URL(PathUtils.getLastPageState(path_objects).getUrl()).getHost())));
	}

	/**
	 * Checks if url is part of domain including sub-domains
	 *  
	 * @param domain_host host of {@link Domain domain}
	 * @param url 
	 * 
	 * @return true if url is external, otherwise false
	 * 
	 * @throws MalformedURLException
	 */
	public static boolean isExternalLink(String domain_host, String url) throws MalformedURLException {
		return !url.contains(domain_host);
	}
	
	/**
	 * Extracts a {@link List list} of link urls by looking up `a` html tags and extracting the href values
	 * 
	 * @param source valid html source
	 * @return {@link List list} of link urls
	 */
	public static List<String> extractLinkUrls(String source) {
		List<String> link_urls = new ArrayList<>();
		Document document = Jsoup.parse(source);
		Elements elements = document.getElementsByTag("a");
		
		for(Element element : elements) {
			String url = element.absUrl("href");
			if(!url.isEmpty()) {
				link_urls.add(url);
			}
		}
		return link_urls;
	}
	
	/**
	 * Extracts a {@link List list} of link urls by looking up `a` html tags and extracting the href values
	 * 
	 * @param source valid html source
	 * @return {@link List list} of link urls
	 */
	public static List<ElementState> extractLinks(List<ElementState> elements) {
		List<ElementState> links = new ArrayList<>();
		
		for(ElementState element : elements) {
			if(element.getName().equalsIgnoreCase("a")) {
				links.add(element);
			}
		}
		return links;
	}
	
	/**
	 *  check if link returns valid content ie. no 404 or page not found errors when navigating to it
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public static boolean doesUrlExist(URL url) throws IOException {
		assert(url != null);
		
		//perform check for http clients
		if("http".equalsIgnoreCase(url.getProtocol()) || "https".equalsIgnoreCase(url.getProtocol())){
			HttpURLConnection huc = (HttpURLConnection) url.openConnection();
			huc.setRequestMethod("HEAD");
			int responseCode = huc.getResponseCode();

			if (responseCode != 404) {
				return true;
			} else {
				return false;
			}
		}
		else if("mailto".equalsIgnoreCase(url.getProtocol())) {
			//TODO check if mailto address is vailid
		}
		else {
			// TODO handle image links
		}
		
		return false;
	}

	/**
	 * Checks if url string ends with an image suffix indicating that it points to an image file
	 * 
	 * @param href url to examine
	 * 
	 * @return true if any suffixes match, false otherwise
	 * 
	 * @pre href != nuill
	 */
	public static boolean isImageUrl(String href) {
		assert(href != null);
		
		return href.endsWith(".jpg") || href.endsWith(".png") || href.endsWith(".gif") || href.endsWith(".bmp") || href.endsWith(".tiff") || href.endsWith(".webp") || href.endsWith(".bpg") || href.endsWith(".heif");
	}
}
