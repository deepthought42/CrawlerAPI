package com.qanairy.models;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.UnhandledAlertException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.auth0.jwt.internal.com.fasterxml.jackson.annotation.JsonIgnore;
import com.minion.browsing.Browser;
/**
 * A reference to a web page 
 *
 */
public class Page extends PathObject {
	private static Logger log = LogManager.getLogger(Page.class);

    private String key;
    private boolean landable = false;
	private String screenshot = null; 
	
	@JsonIgnore
	private String src = "";
	private URL url;
	private int total_weight;
	private int image_weight;
	
	@JsonIgnore
	private List<PageElement> elements;
	private Map<String, Integer> element_counts = new HashMap<String, Integer>();
	
	/**
	 * instantiate an empty page instance
	 */
	public Page(){
		this.setType(Page.class.getSimpleName());
	}

	/**
 	 * Creates a page instance that is meant to contain information about a state of a webpage
 	 * 
	 * @param html
	 * @param url
	 * @param screenshot
	 * @param elements
	 * @throws IOException
	 * 
	 * @pre elements != null
	 */
	public Page(String html, String url, String screenshot_url, List<PageElement> elements) throws IOException {
		assert elements != null;
		
		super.setType("Page");
		
		log.info("setting source");
		this.setSrc(html);
		this.setType("Page");
		this.url = new URL(url.replace("/#",""));
		this.screenshot = screenshot_url;
		this.elements = elements;
		this.element_counts = countTags(this.elements);
		this.setKey(null);
		log.info("Page object created");
	}
	
	/**
 	 * Creates a page instance that is meant to contain information about a state of a webpage
 	 * 
	 * @param html
	 * @param url
	 * @param screenshot
	 * @param elements
	 * @throws IOException
	 * 
	 * @pre elements != null;
	 */
	public Page(String html, String url, String screenshot, List<PageElement> elements, boolean isLandable) throws IOException {
		assert elements != null;
		super.setType("Page");

		log.info("setting source");
		this.setSrc(html);
		this.setUrl(new URL(url.replace("/#","")));
		this.setScreenshot(screenshot);
		this.setElements(elements);
		this.setElementCounts(countTags(this.elements));
		this.setLandable(isLandable);
		this.setKey(null);
		
		log.info("Page object created");
	}
	
	/**
	 * Gets counts for all tags based on {@link PageElement}s passed
	 * 
	 * @param page_elements list of {@link PageElement}s
	 * 
	 * @return Hash of counts for all tag names in list of {@PageElement}s passed
	 */
	public Map<String, Integer> countTags(List<PageElement> tags){
		Map<String, Integer> elem_cnts = new HashMap<String, Integer>();
		for(PageElement tag : tags){
			if(elem_cnts.containsKey(tag.getName())){
				int cnt = elem_cnts.get(tag.getName());
				cnt += 1;
				elem_cnts.put(tag.getName(), cnt);
			}
			else{
				elem_cnts.put(tag.getName(), 1);
			}
		}
		return elem_cnts;
	}
	
	/**
	 * Checks if the page is able to be accessed directly as a landing page
	 * 
	 * @return
	 * @throws java.util.NoSuchElementException
	 * @throws UnhandledAlertException
	 * @throws IOException 
	 */
	public boolean checkIfLandable() throws java.util.NoSuchElementException, UnhandledAlertException, IOException{
		log.info("Checking if page is landable");
		
		Browser browser = new Browser(this.getUrl().toString(), "chrome");
		browser.getDriver().get(this.getUrl().toString());
		String src = Browser.cleanSrc(browser.getDriver().getPageSource());
		boolean landable = false;
		if(this.equals(browser.getPage())){
			log.info("Pages match in check for landability");
			landable = true;
		}
		browser.close();
		return landable;
	}
		
	/**
	 * Checks if Pages are equal
	 * @param page the {@link Page} object to compare current page to
	 * 
	 * @pre page != null
	 * @return boolean value
	 * 
	 * @NOTE :: TODO :: add in ability to differentiate screenshots
	 */
	@Override
	public boolean equals(Object o){
		if (this == o) return true;
        if (!(o instanceof Page)) return false;
        
        Page that = (Page)o;
    	//return (this.getSrc().equals(that.getSrc()) || this.getSrc().length() == that.getSrc().length() || this.screenshot.equals(that.screenshot));
		return (this.url.equals(that.url) 
				&& this.getSrc().equals(that.getSrc())
				&& this.screenshot.equals(that.screenshot));
				
	}
	
	/**
	 * {@inheritDoc} 	
	 */
	@Override
	public String toString(){
		return this.getSrc();
	}
	
	

	/**
	 * {@inheritDoc}
	 */
	@Override
    public int hashCode() {
        int hash = 1;
        hash = hash * 5 + url.hashCode();
        hash = hash * 17 + src.hashCode();
        if(this.screenshot != null){
        	hash = hash * 31 + screenshot.hashCode();
        }
        
        if(elements != null){
	        for(PageElement element : elements){
	        	hash = hash * 13 + element.hashCode();
	        }
        }
        return hash;
    }
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public PathObject clone() {
		Page page = new Page();
		
		page.setElements(this.getElements());
		page.setKey(this.getKey());
		page.setLandable(this.isLandable());
		page.setScreenshot(this.getScreenshot());
		page.setSrc(this.getSrc());
		page.setUrl(this.getUrl());

		return page;
	}
	
	 public String getKey() {
		 return key;
	 }

	 public void setKey(String key) {
		 this.key = key;
	 }
	 
	/**
	 * 
	 * 
	 * @return the page of the source
	 */
	@JsonIgnore
	public String getSrc() {
		return this.src;
	}
	
	@JsonIgnore
	public void setSrc(String src) {
		this.src = Browser.cleanSrc(src);
	}
	
	@JsonIgnore
	public List<PageElement> getElements(){
		return this.elements;
	}
	
	@JsonIgnore
	public void setElements(List<PageElement> elements){
		this.elements = elements;
	}
	
	public void setLandable(boolean isLandable){
		this.landable = isLandable;
	}
	
	public String getScreenshot(){
		return this.screenshot;
	}
	
	public void setScreenshot(String url){
		this.screenshot = url;
	}
		
	public boolean isLandable(){
		return this.landable;
	}

	public URL getUrl(){
		return this.url;
	}
	
	public void setUrl(URL url){
		this.url = url;
	}

	public int getTotalWeight() {
		return total_weight;
	}

	public void setTotalWeight(int total_weight) {
		this.total_weight = total_weight;
	}

	public Map<String, Integer> getElementCounts() {
		return element_counts;
	}

	public void setElementCounts(Map<String, Integer> element_counts) {
		this.element_counts = element_counts;
	}

	public int getImageWeight() {
		return image_weight;
	}

	public void setImageWeight(int image_weight) {
		this.image_weight = image_weight;
	}
}
