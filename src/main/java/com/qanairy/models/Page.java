package com.qanairy.models;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.slf4j.Logger;import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.minion.browsing.Browser;

/**
 * A reference to a web page 
 *
 */
public class Page extends PathObject {
	private static Logger log = LoggerFactory.getLogger(Page.class);

    private String key;
    private boolean landable = false;
	private Map<String, String> browser_screenshots;
	
	@JsonIgnore
	private String src;
	private URL url;
	private Integer total_weight;
	private Integer image_weight;
	
	@JsonIgnore
	private List<PageElement> elements;
	private Map<String, Integer> element_counts;
	
	/**
	 * instantiate an empty page instance
	 */
	public Page(){
		this.setSrc(null);
		this.setType(Page.class.getSimpleName());
		this.setImageWeight(0);
		this.element_counts = new HashMap<String, Integer>();
		this.setBrowserScreenshots(new HashMap<String, String>());
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
	public Page(String html, String url, Map<String, String> browsers_screenshots, List<PageElement> elements) throws IOException {
		assert elements != null;
		
		super.setType("Page");
		this.setSrc(html);
		this.setType("Page");
		this.setUrl(new URL(url.replace("/#","")));
		this.setBrowserScreenshots(browsers_screenshots);
		this.setElements(elements);
		this.setElementCounts(countTags(elements));
		this.setLandable(false);
		this.setImageWeight(0);
		this.setKey(null);
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
	public Page(String key, String html, String url, Map<String, String> browsers_screenshots, List<PageElement> elements) throws IOException {
		assert elements != null;
		
		super.setType("Page");
		this.setSrc(html);
		this.setType("Page");
		this.setUrl(new URL(url.replace("/#","")));
		this.setBrowserScreenshots(browsers_screenshots);
		this.setElements(elements);
		this.setElementCounts(countTags(elements));
		this.setLandable(false);
		this.setImageWeight(0);
		this.setKey(key);
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
	public Page(String html, String url, Map<String, String> browsers_screenshots, List<PageElement> elements, boolean isLandable) throws IOException {
		assert elements != null;
		super.setType("Page");
		this.setSrc(html);
		this.setUrl(new URL(url.replace("/#","")));
		this.setBrowserScreenshots(browsers_screenshots);
		this.setElements(elements);
		this.setElementCounts(countTags(elements));
		this.setLandable(isLandable);
		this.setImageWeight(0);
		this.setKey(null);
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
	public Page(String key, String html, String url, Map<String, String> browsers_screenshots, List<PageElement> elements, boolean isLandable) throws IOException {
		assert elements != null;
		super.setType("Page");
		this.setSrc(html);
		this.setUrl(new URL(url.replace("/#","")));
		this.setBrowserScreenshots(browsers_screenshots);
		this.setElements(elements);
		this.setElementCounts(countTags(elements));
		this.setLandable(isLandable);
		this.setImageWeight(0);
		this.setKey(key);
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
	 */
	public boolean checkIfLandable(String browser_name){		
		boolean landable = false;

		try{
			Browser browser = new Browser(this.getUrl().toString(), browser_name);
			if(this.equals(browser.getPage())){
				landable = true;
			}

			browser.close();
		}catch(Exception e){
			log.error("ERROR VISITING PAGE AT :: "+this.getUrl().getHost()+" ::: "+this.getUrl().toString(), e.getMessage());
		}
		
		return landable;
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
        if (!(o instanceof Page)) return false;
        
        Page that = (Page)o;
        
        boolean screenshots_match = true;
        for(String browser : that.getBrowserScreenshots().keySet()){
			BufferedImage img1;
			BufferedImage img2;
        	
			try {
				img1 = ImageIO.read(new URL(this.getBrowserScreenshots().get(browser)));
				img2 = ImageIO.read(new URL(that.getBrowserScreenshots().get(browser)));
			} catch (IOException e1) {
				e1.printStackTrace();
				return false;
			}

			screenshots_match = compareImages(img1, img2);
        }
        return screenshots_match; 
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
		Page page = new Page();
		
		page.setElements(this.getElements());
		page.setKey(this.getKey());
		page.setLandable(this.isLandable());
		page.setBrowserScreenshots(this.getBrowserScreenshots());
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
	public Map<String, String> getBrowserScreenshots() {
		return browser_screenshots;
	}

	public void setBrowserScreenshots(Map<String, String> browser_screenshots) {
		this.browser_screenshots = browser_screenshots;
	}
	
	public void setBrowserScreenshot(String browser, String screenshot_url) {
		this.browser_screenshots.put(browser, screenshot_url);
	}
}
