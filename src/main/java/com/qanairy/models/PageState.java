package com.qanairy.models;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.apache.commons.codec.binary.Hex;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.openqa.selenium.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonIgnore;

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

	private String src;
	private String key;
	private boolean landable;
	private LocalDateTime last_landability_check;

	private String url;
	private int total_weight;
	private int image_weight;
	private int scrollXOffset;
	private int scrollYOffset;
	private String type;

	@Relationship(type = "HAS_SCREENSHOT")
	private Set<ScreenshotSet> browser_screenshots;

	@Relationship(type = "HAS_ELEMENT")
	private Set<PageElementState> elements;

	@Relationship(type = "HAS_FORM")
	private Set<Form> forms;

	public PageState() {
		setForms(new HashSet<Form>());
	}
	
	/**
	 * Creates a page instance that is meant to contain information about a
	 * state of a webpage
	 * 
	 * @param url
	 * @param screenshot
	 * @param elements
	 * @throws IOException
	 * 
	 * @pre elements != null
	 * @pre browser_screenshots != null;
	 */
	public PageState(String url, Set<ScreenshotSet> browser_screenshots, Set<PageElementState> elements, String src, int scroll_x_offset, int scroll_y_offset){
		assert elements != null;
		assert browser_screenshots != null;

		setType(PageState.class.getSimpleName());
		setUrl(url.replace("/#", ""));
		setBrowserScreenshots(browser_screenshots);
		setLastLandabilityCheck(LocalDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneId.systemDefault()));
		setElements(elements);
		setLandable(false);
		setImageWeight(0);
		setSrc(src);
		setForms(new HashSet<Form>());
		setScrollXOffset(scroll_x_offset);
		setScrollYOffset(scroll_y_offset);
		setKey(generateKey());
	}

	/**
	 * Creates a page instance that is meant to contain information about a
	 * state of a webpage
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
	 * @throws NoSuchAlgorithmException 
	 */
	public PageState(String url, Set<ScreenshotSet> browser_screenshots, Set<PageElementState> elements, boolean isLandable,
			String src, int scroll_x_offset, int scroll_y_offset) throws IOException, NoSuchAlgorithmException {
		assert elements != null;
		assert browser_screenshots != null;

		setType(PageState.class.getSimpleName());
		setUrl(url.replace("/#", ""));
		setBrowserScreenshots(browser_screenshots);
		setLastLandabilityCheck(LocalDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneId.systemDefault()));
		setElements(elements);
		setLandable(isLandable);
		setImageWeight(0);

		setScrollXOffset(scroll_x_offset);
		setScrollYOffset(scroll_y_offset);
		setSrc(src);
		setForms(new HashSet<Form>());
		setKey(generateKey());
	}

	/**
	 * Gets counts for all tags based on {@link PageElementState}s passed
	 * 
	 * @param page_elements
	 *            list of {@link PageElementState}s
	 * 
	 * @return Hash of counts for all tag names in list of {@PageElementState}s
	 *         passed
	 */
	public Map<String, Integer> countTags(Set<PageElementState> tags) {
		Map<String, Integer> elem_cnts = new HashMap<String, Integer>();
		for (PageElementState tag : tags) {
			if (elem_cnts.containsKey(tag.getName())) {
				int cnt = elem_cnts.get(tag.getName());
				cnt += 1;
				elem_cnts.put(tag.getName(), cnt);
			} else {
				elem_cnts.put(tag.getName(), 1);
			}
		}
		return elem_cnts;
	}

	/**
	 * Compares two images pixel by pixel.
	 *
	 * @param imgA
	 *            the first image.
	 * @param imgB
	 *            the second image.
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
	 * 
	 * @param page
	 *            the {@link Page} object to compare current page to
	 * 
	 * @pre page != null
	 * @return boolean value
	 * 
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof PageState))
			return false;

		PageState that = (PageState) o;

		//boolean pages_match = false;
		boolean pages_match = this.getKey().equals(that.getKey());
		/*
		if(!pages_match){
			pages_match = this.getUrl().equals(that.getUrl()) && Browser.cleanSrc(this.getSrc()).equals(Browser.cleanSrc(that.getSrc()));
		}
		*/
		
		if(!pages_match){
			//check if elements match
			Map<String, PageElementState> element_map = new HashMap<String,PageElementState>();
			for(PageElementState elem : this.getElements()){
				element_map.put(elem.getKey(), elem);
			}
			
			for(PageElementState elem: that.getElements()){
				element_map.remove(elem.getKey());
			}
	
			if(element_map.keySet().isEmpty()){
				pages_match = true;
			}
		}
		//boolean sources_match = this.getSrc().equals(that.getSrc());

		return pages_match;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return this.getUrl().toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PathObject clone() {
		Set<PageElementState> elements = new HashSet<PageElementState>(getElements());
		Set<ScreenshotSet> screenshots = new HashSet<ScreenshotSet>(getBrowserScreenshots());

		PageState page = null;
		try {
			page = new PageState(getUrl().toString(), screenshots, elements, isLandable(), getSrc(), getScrollXOffset(), getScrollYOffset());
		} catch (NoSuchAlgorithmException | IOException e) {
			log.info("Error cloning page : " + page.getKey() + ";  "+e.getMessage());
		}
		return page;
	}

	public String getKey() {
		return this.key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	@JsonIgnore
	public Set<PageElementState> getElements() {
		return this.elements;
	}

	@JsonIgnore
	public void setElements(Set<PageElementState> elements) {
		this.elements = elements;
	}

	public void setLandable(boolean isLandable) {
		this.landable = isLandable;
	}

	public boolean isLandable() {
		return this.landable;
	}

	public String getUrl() {
		return this.url;
	}

	public void setUrl(String url) {
		/*
		 * int param_idx = url.indexOf('?'); if(param_idx >= 0){ url =
		 * url.substring(0, param_idx); }
		 */
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

	public void addElement(PageElementState element) {
		this.elements.add(element);
	}

	public String getFileChecksum(MessageDigest digest, String url) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		BufferedImage buff_img = ImageIO.read(new URL(url));
		
		boolean foundWriter = ImageIO.write(buff_img, "png", baos);
		assert foundWriter; // Not sure about this... with jpg it may work but
							// other formats ?
		
		// Get file input stream for reading the file content
		byte[] data = baos.toByteArray();
		digest.update(data);
		byte[] thedigest = digest.digest(data);
		return Hex.encodeHexString(thedigest);
	}

	public static String getFileChecksum(BufferedImage buff_img) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		boolean foundWriter = ImageIO.write(buff_img, "png", baos);
		assert foundWriter; // Not sure about this... with jpg it may work but
							// other formats ?
		// Get file input stream for reading the file content
		byte[] data = baos.toByteArray();
		try {
			MessageDigest sha = MessageDigest.getInstance("SHA-256");
			sha.update(data);
			byte[] thedigest = sha.digest(data);
			return Hex.encodeHexString(thedigest);
		} catch (NoSuchAlgorithmException e) {
			log.error("Error generating checksum of buffered image");
		}
		return "";

	}


	public static BufferedImage resize(BufferedImage img, int newW, int newH) { 
	    Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
	    BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);

	    Graphics2D g2d = dimg.createGraphics();
	    g2d.drawImage(tmp, 0, 0, null);
	    g2d.dispose();

	    return dimg;
	}
	
	/**
	 * {@inheritDoc}
	 * @throws IOException 
	 * @throws NoSuchAlgorithmException 
	 * 
	 * @pre page != null
	 */
	public String generateKey() {
		Set<ScreenshotSet> screenshots = this.getBrowserScreenshots();
		String screenshot = screenshots.iterator().next().getViewportScreenshot();
		int param_index = this.getUrl().indexOf("?");
		String url_without_params = this.getUrl();
		if(param_index >= 0){
			url_without_params = url_without_params.substring(0, param_index);
		}

		try{
			return "pagestate::" + org.apache.commons.codec.digest.DigestUtils.sha256Hex(url_without_params+ getFileChecksum(ImageIO.read(new URL(screenshot))));
		}
		catch(IOException e){
			log.error("Couldn't read file at "+screenshot+" ::  "+e.getLocalizedMessage());
		}
		
		return "";
	}

	public void addForm(Form form) {
		for (Form temp_form : this.forms) {
			if (temp_form.getKey().equals(form.getKey())) {
				return;
			}
		}
		this.forms.add(form);
	}

	public Set<Form> getForms() {
		return this.forms;
	}
	
	public void setForms(Set<Form> form_set){
		this.forms = form_set;
	}

	public LocalDateTime getLastLandabilityCheck() {
		return last_landability_check;
	}

	public void setLastLandabilityCheck(LocalDateTime last_landability_check_timestamp) {
		this.last_landability_check = last_landability_check_timestamp;
	}

	public String getSrc() {
		return src;
	}

	public void setSrc(String src) {
		this.src = src;
	}

	public int getScrollXOffset() {
		return scrollXOffset;
	}

	public void setScrollXOffset(int scrollXOffset) {
		this.scrollXOffset = scrollXOffset;
	}
	
	public int getScrollYOffset() {
		return scrollYOffset;
	}

	public void setScrollYOffset(int scrollYOffset) {
		this.scrollYOffset = scrollYOffset;
	}
}
