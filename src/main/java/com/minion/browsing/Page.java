package com.minion.browsing;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.openqa.selenium.UnhandledAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minion.aws.UploadObjectSingleOperation;
import com.minion.persistence.DataAccessObject;
import com.minion.persistence.IPage;
import com.minion.persistence.OrientConnectionFactory;

/**
 * A reference to a web page 
 * 
 * @author Brandon Kindred
 *
 */
public class Page extends PathObject<IPage> {
    private static final Logger log = LoggerFactory.getLogger(Page.class);

    private String key;
    private boolean landable = false;
	private String screenshot = null; 
	private String src = "";
	private URL url;
	private int total_weight;
	private int image_weight;
	
	private List<PageElement> elements;
	private Map<String, Integer> element_counts = new HashMap<String, Integer>();
	
	public Page(){}

	/**
	 * Creates a page instance that is meant to contain the information found using the driver passed
	 * 
	 * @param driver
	 * @param valid
	 * @throws IOException
	 * @throws URISyntaxException 
	 */
	public Page(String html, String url, File screenshot, List<PageElement> elements) throws IOException {
		log.info("setting source");
		this.setSrc(html);

		log.info("Page URL :: "+url);
		this.url = new URL(url.replace("/#",""));
		
		log.info("GETTING SCREENSHOT");
		this.screenshot = UploadObjectSingleOperation.saveImageToS3(screenshot, this.url.getHost(), this.url.getPath().toString());
		
		System.err.println("IMAGE SAVED TO S3 at : " +this.screenshot);
		this.elements = elements;
		//this.element_counts = countTags(this.elements);
		
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
		
		Browser browser = new Browser(this.getUrl().toString());
		browser.getDriver().get(this.getUrl().toString());
		//String src = Browser.cleanSrc(browser.getDriver().getPageSource());
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
        //log.info(this.elements.size() + " :: "+ that.elements.size());
        log.info("Do screenshots match? : "+this.screenshot.equals(that.getScreenshot()));
        log.info("sources match? : " +(this.getSrc().length() == that.getSrc().length()));
        log.info("Source 1: " +this.getSrc());
        log.info("Source 2: " +that.getSrc());
    	log.info("PAGE URLs ARE EQUAL? :: "+this.url+" == "+that.url +" :: ");
    	log.info("urls equal?" + this.url.equals(that.url));

    	log.info("PAGE SRCs ARE EQUAL? :: "+this.getSrc().equals(that.getSrc()));
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
	public String generateKey() {
		return this.src.hashCode() + "::";
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public IPage create() {
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		
		IPage page = this.convertToRecord(orient_connection);
		orient_connection.save();
		
		return page;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IPage update() {
		OrientConnectionFactory connection = new OrientConnectionFactory();
		IPage page = this.convertToRecord(connection);
		connection.save();
		
		return page;
	}
	
	public static Page convertFromRecord(IPage result) {
		Page page = new Page();
		page.setType(Page.class.getSimpleName());
		page.setScreenshot(result.getScreenshot());
		page.setKey(result.getKey());
		page.setLandable(result.isLandable());
		page.setSrc(result.getSrc());
		page.setElementCounts(result.getElementCounts());
		
		try {
			page.setUrl(new URL(result.getUrl()));
		} catch (MalformedURLException e) {
			page.setUrl(null);
			e.printStackTrace();
		}

		return page;
	}
	
	/**
	 * Converts Page to IPage for persistence
	 * 
	 * @param page
	 */
	@Override
	public IPage convertToRecord(OrientConnectionFactory connection){
		this.setKey(this.generateKey());
		@SuppressWarnings("unchecked")
		Iterable<IPage> pages = (Iterable<IPage>) DataAccessObject.findByKey(this.getKey(), connection, IPage.class);
		
		int cnt = 0;
		Iterator<IPage> iter = pages.iterator();
		IPage page = null;
		while(iter.hasNext()){
			iter.next();
			cnt++;
		}
		log.info("# of existing records with key "+this.getKey() + " :: "+cnt);
		
		if(cnt == 0){
			page = connection.getTransaction().addVertex("class:"+IPage.class.getCanonicalName()+","+UUID.randomUUID(), IPage.class);
			page.setLandable(this.isLandable());
			page.setScreenshot(this.getScreenshot());
			page.setSrc(this.getSrc());
			page.setUrl(this.getUrl().toString());
			page.setType(this.getClass().getName());
			page.setKey(this.getKey());
			page.setElementCounts(this.element_counts);
		}
		else{
			page = pages.iterator().next();
		}

		return page;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PathObject<?> clone() {
		Page page = new Page();
		
		page.setElements(this.getElements());
		page.setKey(this.generateKey());
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
	public String getSrc() {
		return this.src;
	}
	
	public void setSrc(String src) {
		this.src = Browser.cleanSrc(src);
	}
	
	public List<PageElement> getElements(){
		return this.elements;
	}
	
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
