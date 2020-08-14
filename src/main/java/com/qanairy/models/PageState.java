package com.qanairy.models;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.apache.commons.codec.binary.Hex;
import org.neo4j.ogm.annotation.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.minion.browsing.Browser;

/**
 * A reference to a web page
 *
 */
public class PageState extends LookseeObject {
	private static Logger log = LoggerFactory.getLogger(PageState.class);

	
	//Deprecating this value because it should be coming from Page
	private String src;
	private String url;
	private boolean login_required;

	private LocalDateTime last_landability_check;

	private String viewport_screenshot_url;
	private String full_page_screenshot_url;
	private String browser;
	private boolean landable;
	private long scrollXOffset;
	private long scrollYOffset;
	private int viewport_width;
	private int viewport_height;
	private long full_page_width;
	private long full_page_height;

	@Relationship(type = "HAS")
	private List<ElementState> elements;


	public PageState() {
		super();
		setElements(new ArrayList<>());
	}
	
	/**
	 * Creates a page instance that is meant to contain information about a
	 * state of a webpage
	 * @param elements
	 * @param full_page_screenshot_url TODO
	 * @param url TODO
	 * @param url
	 * @param full_page_checksum TODO
	 * @param title TODO
	 * @param screenshot
	 *
	 * @throws MalformedURLException
	 * @throws IOException
	 *
	 * @pre elements != null
	 * @pre screenshot_url != null;
	 */
	public PageState(String screenshot_url, List<ElementState> elements, String src, long scroll_x_offset, long scroll_y_offset,
			int viewport_width, int viewport_height, String browser_name, String full_page_screenshot_url, String url)
	{
		super();
		assert screenshot_url != null;
		assert elements != null;
		assert src != null;
		assert browser_name != null;
		assert full_page_screenshot_url != null;
		
		setViewportScreenshotUrl(screenshot_url);
		setViewportWidth(viewport_width);
		setViewportHeight(viewport_height);
		setBrowser(browser_name);
		setLastLandabilityCheck(LocalDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneId.systemDefault()));
		setElements(elements);
		setLandable(false);
		setSrc(src);
		setScrollXOffset(scroll_x_offset);
		setScrollYOffset(scroll_y_offset);
	    setLoginRequired(false);
		setFullPageScreenshotUrl(full_page_screenshot_url);
		setUrl(url);
		setKey(generateKey());
	}
	

	/**
	 * Creates a page instance that is meant to contain information about a
	 * state of a webpage
	 * @param url
	 * @param elements
	 * @param isLandable
	 * @param title TODO
	 * @param html
	 * @param browsers_screenshots
	 *
	 * @pre elements != null;
	 * @pre screenshot_url != null;
	 *
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
	public PageState(String screenshot_url, List<ElementState> elements, boolean isLandable,
			String src, long scroll_x_offset, long scroll_y_offset, int viewport_width, int viewport_height,
			String browser_name, String full_page_screenshot_url) {
		super();
		assert elements != null;
		assert screenshot_url != null;
		assert full_page_screenshot_url != null;
		assert !full_page_screenshot_url.isEmpty();
		
		setViewportScreenshotUrl(screenshot_url);
		setLastLandabilityCheck(LocalDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneId.systemDefault()));
		setElements(elements);
		setLandable(isLandable);
		setBrowser(browser_name);
		setScrollXOffset(scroll_x_offset);
		setScrollYOffset(scroll_y_offset);
		setViewportWidth(viewport_width);
		setViewportHeight(viewport_height);
		setSrc(Browser.cleanSrc(src));
		setLoginRequired(false);
		setFullPageScreenshotUrl(full_page_screenshot_url);
		setKey(generateKey());
	}

	/**
	 * Gets counts for all tags based on {@link Element}s passed
	 *
	 * @param page_elements
	 *            list of {@link Element}s
	 *
	 * @return Hash of counts for all tag names in list of {@ElementState}s
	 *         passed
	 */
	public Map<String, Integer> countTags(Set<Element> tags) {
		Map<String, Integer> elem_cnts = new HashMap<String, Integer>();
		for (Element tag : tags) {
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
		
		return this.getKey().equals(that.getKey());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PageState clone() {
		List<ElementState> elements = new ArrayList<ElementState>(getElements());

		return new PageState(getViewportScreenshotUrl(), elements, isLandable(), getSrc(), getScrollXOffset(), getScrollYOffset(), getViewportWidth(), getViewportHeight(), getBrowser(), getFullPageScreenshotUrl());
	}

	@JsonIgnore
	public List<ElementState> getElements() {
		return this.elements;
	}

	@JsonIgnore
	public void setElements(List<ElementState> elements) {
		this.elements = elements;
	}

	public void setLandable(boolean isLandable) {
		this.landable = isLandable;
	}

	public boolean isLandable() {
		return this.landable;
	}

	public void addElement(ElementState element) {
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

	/**
	 * 
	 * @param buff_img
	 * @return
	 * @throws IOException
	 */
	public static String getFileChecksum(BufferedImage buff_img) throws IOException {
		assert buff_img != null;
		
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

	/**
	 * {@inheritDoc}
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 *
	 * @pre page != null
	 */
	public String generateKey() {
		List<ElementState> properties = new ArrayList<>(this.getElements());
		Collections.sort(properties);
		String key = "";
		for(ElementState element : properties) {
			key += element.getKey();
		}
		return "pagestate::" + org.apache.commons.codec.digest.DigestUtils.sha256Hex(key);
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

	public long getScrollXOffset() {
		return scrollXOffset;
	}

	public void setScrollXOffset(long scrollXOffset) {
		this.scrollXOffset = scrollXOffset;
	}

	public long getScrollYOffset() {
		return scrollYOffset;
	}

	public void setScrollYOffset(long scrollYOffset) {
		this.scrollYOffset = scrollYOffset;
	}

	public String getViewportScreenshotUrl() {
		return viewport_screenshot_url;
	}

	public void setViewportScreenshotUrl(String viewport_screenshot_url) {
		this.viewport_screenshot_url = viewport_screenshot_url;
	}

	public String getBrowser() {
		return browser;
	}

	public void setBrowser(String browser) {
		this.browser = browser;
	}


	public int getViewportWidth() {
		return viewport_width;
	}

	public void setViewportWidth(int viewport_width) {
		this.viewport_width = viewport_width;
	}

	public int getViewportHeight() {
		return viewport_height;
	}

	public void setViewportHeight(int viewport_height) {
		this.viewport_height = viewport_height;
	}

	public boolean isLoginRequired() {
		return login_required;
	}

	public void setLoginRequired(boolean login_required) {
		this.login_required = login_required;
	}
	
	public String getFullPageScreenshotUrl() {
		return full_page_screenshot_url;
	}

	public void setFullPageScreenshotUrl(String full_page_screenshot_url) {
		this.full_page_screenshot_url = full_page_screenshot_url;
	}

	public long getFullPageWidth() {
		return full_page_width;
	}
	
	public void setFullPageWidth(long full_page_width) {
		this.full_page_width = full_page_width;
	}
	
	public long getFullPageHeight() {
		return full_page_height;
	}

	public void setFullPageHeight(long full_page_height) {
		this.full_page_height = full_page_height;
	}

	public void addElements(List<ElementState> elements) {
		//check for duplicates before adding
		for(ElementState element : elements) {
			if(!this.elements.contains(element)) {				
				this.elements.add(element);
			}
		}
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
}
