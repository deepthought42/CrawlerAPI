package com.qanairy.models;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.minion.browsing.Browser;
import com.qanairy.persistence.PageState;
import com.qanairy.persistence.PageElement;
import com.qanairy.persistence.PathObject;
import com.qanairy.persistence.ScreenshotSet;

/**
 * A reference to a web page 
 *
 */
public class PageStatePOJO extends PageState {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(PageStatePOJO.class);

    private String key;
    private boolean landable = false;
	private List<ScreenshotSet> browser_screenshots;
	
	@JsonIgnore
	private String src;
	private URL url;
	private Integer total_weight;
	private Integer image_weight;
	
	@JsonIgnore
	private List<PageElement> elements;
	private Map<String, Integer> element_counts;

	private String type;
	
	/**
 	 * Creates a page instance that is meant to contain information about a state of a webpage
 	 * 
	 * @param html
	 * @param url
	 * @param screenshot
	 * @param elements
	 * @throws IOException
	 * 
	 * @pre html != null && html.length() > 0
	 * @pre elements != null
	 */
	public PageStatePOJO(String html, String url, List<ScreenshotSet> browsers_screenshots, List<PageElement> elements) throws IOException {
		assert elements != null;
		assert html != null;
		assert html.length() > 0;
		
		setType("Page");
		this.setSrc(html);
		this.setType("Page");
		this.setUrl(new URL(url.replace("/#","")));
		this.setBrowserScreenshots(browsers_screenshots);
		this.setElements(elements);
		this.setElementCounts(countTags(elements));
		this.setLandable(false);
		this.setImageWeight(0);
		this.setType(PageState.class.getSimpleName());
		setKey(generateKey());

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
	public PageStatePOJO(String key, String html, String url, List<ScreenshotSet> browsers_screenshots, List<PageElement> elements) throws IOException {
		assert elements != null;
		
		setType("Page");
		this.setSrc(html);
		this.setType("Page");
		this.setUrl(new URL(url.replace("/#","")));
		this.setBrowserScreenshots(browsers_screenshots);
		this.setElements(elements);
		this.setElementCounts(countTags(elements));
		this.setLandable(false);
		this.setImageWeight(0);
		this.setType(PageState.class.getSimpleName());
		setKey(generateKey());
	}
	
	/**
 	 * Creates a page instance that is meant to contain information about a state of a webpage
 	 * 
	 * @param html
	 * @param url
	 * @param browsers_screenshots
	 * @param elements
	 * @param isLandable
	 * 
	 * @pre elements != null;
	 * @throws IOException
	 */
	public PageStatePOJO(String html, String url, List<ScreenshotSet> browsers_screenshots, List<PageElement> elements, boolean isLandable) throws IOException {
		assert elements != null;
		setType("Page");
		setSrc(html);
		setUrl(new URL(url.replace("/#","")));
		setBrowserScreenshots(browsers_screenshots);
		setElements(elements);
		setElementCounts(countTags(elements));
		setLandable(isLandable);
		setImageWeight(0);
		setType(PageState.class.getSimpleName());
		setKey(generateKey());
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
	public PageStatePOJO(String key, String html, String url, List<ScreenshotSet> browsers_screenshots, List<PageElement> elements, boolean isLandable) throws IOException {
		assert elements != null;
		setType("Page");
		setSrc(html);
		setUrl(new URL(url.replace("/#","")));
		setBrowserScreenshots(browsers_screenshots);
		setElements(elements);
		setElementCounts(countTags(elements));
		setLandable(isLandable);
		setImageWeight(0);
		setKey(key);
		setType(PageState.class.getSimpleName());
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
	 * Compares two images pixel by pixel.
	 *
	 * @param imgA the first image.
	 * @param imgB the second image.
	 * @return whether the images are both the same or not.
	 */
	public static boolean compareImages(BufferedImage imgA, BufferedImage imgB) {
	  // The images must be the same size.
	  if (imgA.getWidth() == imgB.getWidth() && imgA.getHeight() == imgB.getHeight()) {
	    int width = imgA.getWidth();
	    int height = imgA.getHeight();

	    // Loop over every pixel.
	    for (int y = 0; y < height; y++) {
	      for (int x = 0; x < width; x++) {
	        // Compare the pixels for equality.
	        if (imgA.getRGB(x, y) != imgB.getRGB(x, y)) {
	          return false;
	        }
	      }
	    }
	  } else {
		System.err.println("#########          Pages are not the same size!!!");
	    return false;
	  }

	  return true;
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
        if (!(o instanceof PageState)) return false;
        
        PageState that = (PageState)o;
        
        String thisBrowserScreenshot = this.getBrowserScreenshots().get(0).getFullScreenshot();
        String thatBrowserScreenshot = that.getBrowserScreenshots().get(0).getFullScreenshot();
        
        boolean screenshots_match = false;
        /*for(ScreenshotSet screenshots : this.getBrowserScreenshots()){
        	if(screenshots.getBrowserName().equals(anObject))
        }
        
        for(ScreenshotSet screenshots : this.getBrowserScreenshots()){
        	
        }
        */
        //for(String browser : that.getBrowserScreenshots().keySet()){
		BufferedImage img1;
		BufferedImage img2;
    	
    	if(!thisBrowserScreenshot.equals(thatBrowserScreenshot)){
    		try {
    			img1 = ImageIO.read(new URL(thisBrowserScreenshot));
    			img2 = ImageIO.read(new URL(thatBrowserScreenshot));
    			screenshots_match = compareImages(img1, img2);
    			System.err.println("DO THE SCREENSHOTS MATCH????        ::::     "+screenshots_match);
    	        return screenshots_match;
    		} catch (IOException e1) {
    			e1.printStackTrace();
    			System.err.println("YO THE FULL PAGE SCREENSHOT COMPARISON THINGY ISN'T WORKING!!!!!!  HALP!!!!!!!!!!");
    		}
    	}
    	else{
    		return true;
    	}
        
        
        //System.err.println("Screenshots match? :: "+screenshots_match);
        /*
        System.err.println("PAGE SOURCES MATCH??    ::   "+this.getSrc().equals(that.getSrc()));
        System.err.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        System.err.println("Page 1 length :: "+this.getElements().size());
        System.err.println("Page 2 length :: "+that.getElements().size());
        System.err.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        */
        
        //NOTE ::: THE FOLLOWING COMMENTED CODE CAN BE USED TO TEST PAGE EQUALITY BASED ON PAGE ELEMENTS
        if(this.getElements().size() == that.getElements().size()){
	        Map<String, PageElement> page_elements = new HashMap<String, PageElement>();
	        for(PageElement elem : that.getElements()){
	        	page_elements.put(elem.getXpath(), elem);
	        }
	        
	        for(PageElement elem : this.getElements()){
	        	if(elem.equals(page_elements.get(elem.getXpath()))){
	        		page_elements.remove(elem.getXpath());
	        	}
	        	else{
	        		System.err.println("PAGE ELEMENTS ARE NOT EQUAL");
	        	}
	        }
	        
	        if(page_elements.isEmpty()){
	        	System.err.println("Page elements map is empty. Pages are EQUAL!!!!");
	        	return true;
	        }
        }
    	return false;
    	
  	}
	
	/**
	 * {@inheritDoc} 	
	 */
	@Override
	public String toString(){
		return this.getUrl().toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public int hashCode() {
        int hash = 1;
        hash = hash * 5 + url.hashCode();
        hash = hash * 17 + src.hashCode();
        
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
		List<PageElement> elements = new ArrayList<PageElement>(getElements());
		List<ScreenshotSet> screenshots = new ArrayList<ScreenshotSet>(getBrowserScreenshots());
		
		PageStatePOJO page;
		try {
			page = new PageStatePOJO(getSrc(), getUrl().toString(), screenshots, elements, isLandable());
			page.setElements(this.getElements());
			page.setLandable(this.isLandable());
			page.setSrc(this.getSrc());
			page.setUrl(this.getUrl());
			return page;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	 public String getKey() {
		 return this.key;
	 }

	 public void setKey(String key) {
		 this.key = key;
	 }
	 
	/**
	 * @return the page of the source
	 */
	@JsonIgnore
	public String getSrc() {
		return this.src;
	}
	
	@JsonIgnore
	public void setSrc(String src) {
		if(src != null && src.length() > 0){
			String cleaned_src = Browser.cleanSrc(src);
			this.src = cleaned_src;
		}
		else{
			this.src = src;
		}
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
		
	public boolean isLandable(){
		return this.landable;
	}

	public URL getUrl(){
		return this.url;
	}
	
	public void setUrl(URL url){
		this.url = url;
	}

	public Integer getTotalWeight() {
		return total_weight;
	}

	public void setTotalWeight(Integer total_weight) {
		this.total_weight = total_weight;
	}

	public Map<String, Integer> getElementCounts() {
		return element_counts;
	}

	public void setElementCounts(Map<String, Integer> element_counts) {
		this.element_counts = element_counts;
	}

	public Integer getImageWeight() {
		return image_weight;
	}

	public void setImageWeight(Integer image_weight) {
		this.image_weight = image_weight;
	}

	@JsonProperty("browser_screenshots")
	public List<ScreenshotSet> getBrowserScreenshots() {
		return browser_screenshots;
	}

	public void setBrowserScreenshots(List<ScreenshotSet> browser_screenshots) {
		this.browser_screenshots = browser_screenshots;
	}

	@Override
	public String getType() {
		return this.type;
	}

	@Override
	public void setType(String type) {
		this.type = type;
	}

	@Override
	public void addBrowserScreenshot(ScreenshotSet browser_screenshots) {
		this.browser_screenshots.add(browser_screenshots);
	}

	@Override
	public void addElement(PageElement element) {
		this.elements.add(element);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @pre page != null
	 */
	public String generateKey() {
		return org.apache.commons.codec.digest.DigestUtils.sha256Hex(getSrc());   
	}
}
