package com.qanairy.models;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.minion.browsing.Browser;

/**
 * A reference to a web page 
 *
 */
@NodeEntity
public class PageState implements Persistable, PathObject {
	private static Logger log = LoggerFactory.getLogger(PageState.class);
	
	@GeneratedValue
    @Id
	private Long id;
	
    private String key;
    private boolean landable;
    
    @JsonIgnore
	private String src;
	private String url;
	private int total_weight;
	private int image_weight;
	private String type;
	
	@Relationship(type = "HAS_SCREENSHOT")
	private Set<ScreenshotSet> browser_screenshots = new HashSet<>();
	
	@Relationship(type = "HAS_ELEMENT")
	private Set<PageElement> elements = new HashSet<>();
	
	@Relationship(type = "HAS_FORM")
	private Set<Form> forms = new HashSet<>();
	
	public PageState(){}

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
	 * @pre browser_screenshots != null;
	 */
	public PageState(String html, String url , Set<PageElement> elements) throws IOException {
		assert elements != null;
		assert html != null;
		assert html.length() > 0;
		assert browser_screenshots  != null;

		setType(PageState.class.getSimpleName());
		setSrc("");
		setUrl(url.replace("/#",""));
		setElements(elements);
		setLandable(false);
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
	 * @pre html != null && html.length() > 0
	 * @pre elements != null
	 * @pre browser_screenshots != null;
	 */
	public PageState(String html, String url, Set<ScreenshotSet> browser_screenshots , Set<PageElement> elements) throws IOException {
		assert elements != null;
		assert html != null;
		assert html.length() > 0;
		assert browser_screenshots  != null;

		setType(PageState.class.getSimpleName());
		setSrc("");
		setUrl(url.replace("/#",""));
		setBrowserScreenshots(browser_screenshots );
		setElements(elements);
		setLandable(false);
		setImageWeight(0);
		setType(PageState.class.getSimpleName());
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
	 * @pre browser_screenshots != null;
	 * 
	 * @throws IOException
	 */
	public PageState(String html, String url, Set<ScreenshotSet> browser_screenshots, Set<PageElement> elements, boolean isLandable) throws IOException {
		assert elements != null;
		assert browser_screenshots != null;
	
		setType(PageState.class.getSimpleName());
		setSrc("");
		setUrl(url.replace("/#",""));
		setBrowserScreenshots(browser_screenshots);
		setElements(elements);
		setLandable(isLandable);
		setImageWeight(0);
		setType(PageState.class.getSimpleName());
		setKey(generateKey());
	}
	
	
	/**
	 * Gets counts for all tags based on {@link PageElement}s passed
	 * 
	 * @param page_elements list of {@link PageElement}s
	 * 
	 * @return Hash of counts for all tag names in list of {@PageElement}s passed
	 */
	public Map<String, Integer> countTags(Set<PageElement> tags){
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
        
        boolean pages_match = this.getKey().equals(that.getKey());
		/*try {
			System.err.println("This browser screenshot :: "+this.getBrowserScreenshots().size());
			System.err.println("NEXT BROSEWR SCREENSHOT :: "+this.getBrowserScreenshots().iterator().next());
			System.err.println("Viewport screenshot :: "+this.getBrowserScreenshots().iterator().next().getViewportScreenshot());
			String thisBrowserScreenshot = this.getBrowserScreenshots().iterator().next().getViewportScreenshot();
	        String thatBrowserScreenshot = that.getBrowserScreenshots().iterator().next().getViewportScreenshot();	        
	        
	        System.err.println("Checking image location for equality :: "+thisBrowserScreenshot.equals(thatBrowserScreenshot));
			BufferedImage img1;
			BufferedImage img2;
			
			img1 = ImageIO.read(new URL(thisBrowserScreenshot));
			img2 = ImageIO.read(new URL(thatBrowserScreenshot));
			pages_match = compareImages(img1, img2);
			System.err.println("DO THE SCREENSHOTS MATCH FOR PAGE EQUALITY????        ::::     "+pages_match);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		*/
        //System.err.println("Screenshots match? :: "+screenshots_match);
        
        /*System.err.println("PAGE SOURCES MATCH??    ::   "+this.getSrc().equals(that.getSrc()));
        System.err.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        System.err.println("Page 1 length :: "+this.getElements().size());
        System.err.println("Page 2 length :: "+that.getElements().size());
        System.err.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        */
        
        if(!pages_match ){
	        Map<String, PageElement> page_elements = new HashMap<String, PageElement>();
	        for(PageElement elem : that.getElements()){
	        	page_elements.put(elem.getXpath(), elem);
	        }
	        
	        for(PageElement elem : this.getElements()){
        		page_elements.remove(elem.getXpath());
	        }

	        System.err.println("#####################################################################################");
	        System.err.println("PAGE ELEMENT DIFF :: "+page_elements.size());
	        if(page_elements.isEmpty()){
	        	pages_match = true;
	        }
	        else{
	        	System.err.println("TOTAL ELEMENTS FOR Both :: " + (this.getElements().size()+that.getElements().size())/2);
	        	System.err.println("difference percentage :: "+(page_elements.size()/((this.getElements().size()+that.getElements().size())/2)));
	        	System.err.println("###################################################################################");
	        	
	        	if( (page_elements.size()/((this.getElements().size()+that.getElements().size())/2)) > 0.7){
		        	pages_match = false;	
	        	}
	        	else{
	        		pages_match = true;
	        	}
	        }
        }
    	return pages_match;
    	
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
		Set<PageElement> elements = new HashSet<PageElement>(getElements());
		Set<ScreenshotSet> screenshots = new HashSet<ScreenshotSet>(getBrowserScreenshots());
		
		PageState page;
		try {
			page = new PageState(getSrc(), getUrl().toString(), screenshots, elements, isLandable());
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
	public Set<PageElement> getElements(){
		return this.elements;
	}
	
	@JsonIgnore
	public void setElements(Set<PageElement> elements){
		this.elements = elements;
	}
	
	public void setLandable(boolean isLandable){
		this.landable = isLandable;
	}
		
	public boolean isLandable(){
		return this.landable;
	}

	public String getUrl(){
		return this.url;
	}
	
	public void setUrl(String url){
		this.url = url;
	}

	public Integer getTotalWeight() {
		return total_weight;
	}

	public void setTotalWeight(Integer total_weight) {
		this.total_weight = total_weight;
	}

	public Integer getImageWeight() {
		return image_weight;
	}

	public void setImageWeight(Integer image_weight) {
		this.image_weight = image_weight;
	}

	public Set<ScreenshotSet> getBrowserScreenshots() {
		return browser_screenshots;
	}

	public void setBrowserScreenshots(Set<ScreenshotSet> browser_screenshots) {
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

	public void addBrowserScreenshot(ScreenshotSet browser_screenshots) {
		this.browser_screenshots.add(browser_screenshots);
	}

	public void addElement(PageElement element) {
		this.elements.add(element);
	}

	public static String getFileChecksum(MessageDigest digest, String url) throws IOException
	{
		InputStream is = new URL(url).openStream(); 

	    //Create byte array to read data in chunks
	    byte[] byteArray = new byte[1024];
	    int bytesCount = 0;
	      
	    //Read file data and update in message digest
	    while ((bytesCount = is.read(byteArray)) != -1) {
	        digest.update(byteArray, 0, bytesCount);
	    };
	     
	    //close the stream; We don't need it now.
	    is.close();
	     
	    //Get the hash's bytes
	    byte[] bytes = digest.digest();
	     
	    //This bytes[] has bytes in decimal format;
	    //Convert it to hexadecimal format
	    StringBuilder sb = new StringBuilder();
	    for(int i=0; i< bytes.length ;i++)
	    {
	        sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
	    }
	     
	   //return complete hash
	   return sb.toString();
	}
	
	public static String getFileChecksum(BufferedImage bufferedImage) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		boolean foundWriter = ImageIO.write(bufferedImage, "png", baos);
		assert foundWriter; // Not sure about this... with jpg it may work but other formats ?
	    //Get file input stream for reading the file content
	    
	    try {
			MessageDigest sha = MessageDigest.getInstance("SHA-256");
			byte[] thedigest = sha.digest(baos.toByteArray());
	        return DatatypeConverter.printHexBinary(thedigest);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	    return "";
	}
		
	/**
	 * {@inheritDoc}
	 * 
	 * @pre page != null
	 */
	public String generateKey() {
		try{
			return "pagestate::"+getFileChecksum(MessageDigest.getInstance("SHA-512"), this.getBrowserScreenshots().iterator().next().getViewportScreenshot());
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return "";
		//return "";
		/*String key = "";
		for(PageElement element : getElements()){
			key += element.getKey();
		}
		return org.apache.commons.codec.digest.DigestUtils.sha512Hex(key);
		*/
		
	}

	public void addForm(Form form) {
		for(Form temp_form: this.forms){
			if(temp_form.getKey().equals(form.getKey())){
				return;
			}
		}
		this.forms.add(form);
	}
	
	public Set<Form> getForms(){
		return this.forms;
	}
}
