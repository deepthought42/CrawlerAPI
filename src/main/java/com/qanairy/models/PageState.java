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
import java.util.HashMap;
import java.util.HashSet;
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
import com.qanairy.services.BrowserService;

/**
 * A reference to a web page
 *
 */
public class PageState extends LookseeObject {
	private static Logger log = LoggerFactory.getLogger(PageState.class);

	
	//Deprecating this value because it should be coming from Page
	@Deprecated
	private String url;
	private String src;
	private String src_checksum;
	private boolean login_required;

	private LocalDateTime last_landability_check;
	@Deprecated
	private String screenshot_url;
	private String full_page_screenshot_url;
	private String full_page_checksum;
	private String browser;
	private boolean landable;
	private long scrollXOffset;
	private long scrollYOffset;
	private int viewport_width;
	private int viewport_height;
	private long full_page_width;
	private long full_page_height;
	private String title;
	
	@Deprecated
	private List<String> screenshot_checksums;
	@Deprecated
	private List<String> animated_image_urls;
	@Deprecated
	private List<String> animated_image_checksums;

	@Relationship(type = "HAS")
	private List<ElementState> elements;

	@Deprecated
	@Relationship(type = "HAS")
	private Set<Form> forms;

	public PageState() {
		super();
		setForms(new HashSet<>());
		setElements(new ArrayList<>());
		setScreenshotChecksum(new ArrayList<String>());
		setAnimatedImageUrls(new ArrayList<>());
		setAnimatedImageChecksums(new ArrayList<>());
	}
	
	/**
	 * Creates a page instance that is meant to contain information about a
	 * state of a webpage
	 *
	 * @param url
	 * @param elements
	 * @param full_page_screenshot_url TODO
	 * @param full_page_checksum TODO
	 * @param title TODO
	 * @param screenshot
	 * @throws MalformedURLException
	 * @throws IOException
	 *
	 * @pre elements != null
	 * @pre screenshot_url != null;
	 */
	public PageState(String url, String screenshot_url, List<ElementState> elements, String src, long scroll_x_offset, long scroll_y_offset,
			int viewport_width, int viewport_height, String browser_name, Set<Form> forms, String full_page_screenshot_url, String full_page_checksum, String title)
					throws MalformedURLException, IOException
	{
		super();
		assert elements != null;
		assert screenshot_url != null;

		setUrl(url);
		setScreenshotUrl(screenshot_url);
		setViewportWidth(viewport_width);
		setViewportHeight(viewport_height);
		setBrowser(browser_name);
		setLastLandabilityCheck(LocalDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneId.systemDefault()));
		setElements(elements);
		setLandable(false);
		setSrc(src);
		setSrcChecksum(	org.apache.commons.codec.digest.DigestUtils.sha256Hex(BrowserService.generalizeSrc(getSrc())) );
		setScreenshotChecksum(new ArrayList<String>());
		setScrollXOffset(scroll_x_offset);
		setScrollYOffset(scroll_y_offset);
		setAnimatedImageUrls(new ArrayList<String>());
		setAnimatedImageChecksums(new ArrayList<>());
	    setLoginRequired(false);
		setForms(forms);
		setFullPageScreenshotUrl(full_page_screenshot_url);
		setFullPageChecksum(full_page_checksum);
		setTitle(title);
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
	public PageState(String url, String screenshot_url, List<ElementState> elements, boolean isLandable,
			String src, long scroll_x_offset, long scroll_y_offset, int viewport_width, int viewport_height,
			String browser_name, Set<Form> forms, String full_page_screenshot_url, String full_page_checksum, String title) throws IOException, NoSuchAlgorithmException {
		super();
		assert elements != null;
		assert screenshot_url != null;
		assert full_page_checksum != null;
		assert !full_page_checksum.isEmpty();
		assert full_page_screenshot_url != null;
		assert !full_page_screenshot_url.isEmpty();
		
		setUrl(url);
		setScreenshotUrl(screenshot_url);
		setLastLandabilityCheck(LocalDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneId.systemDefault()));
		setElements(elements);
		setLandable(isLandable);
		setBrowser(browser_name);
		setScrollXOffset(scroll_x_offset);
		setScrollYOffset(scroll_y_offset);
		setViewportWidth(viewport_width);
		setViewportHeight(viewport_height);
		setScreenshotChecksum(new ArrayList<String>());
		setSrc(Browser.cleanSrc(src));
		setSrcChecksum(	org.apache.commons.codec.digest.DigestUtils.sha256Hex(BrowserService.generalizeSrc(getSrc())) );
		setAnimatedImageUrls(new ArrayList<String>());
		setAnimatedImageChecksums(new ArrayList<>());
		setLoginRequired(false);
		setForms(forms);
		setTitle(title);
		setFullPageScreenshotUrl(full_page_screenshot_url);
		setFullPageChecksum(full_page_checksum);
		setKey(generateKey());
	}

	/**
	 * Gets counts for all tags based on {@link ElementState}s passed
	 *
	 * @param page_elements
	 *            list of {@link ElementState}s
	 *
	 * @return Hash of counts for all tag names in list of {@ElementState}s
	 *         passed
	 */
	public Map<String, Integer> countTags(Set<ElementState> tags) {
		Map<String, Integer> elem_cnts = new HashMap<String, Integer>();
		for (ElementState tag : tags) {
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
	public String toString() {
		return this.getUrl();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PageState clone() {
		List<ElementState> elements = new ArrayList<ElementState>(getElements());

		PageState page = null;
		try {
			page = new PageState(getUrl(), getScreenshotUrl(), elements, isLandable(), getSrc(), getScrollXOffset(), getScrollYOffset(), getViewportWidth(), getViewportHeight(), getBrowser(), getForms(), getFullPageScreenshotUrl(), getFullPageChecksum(), null);
			page.setScreenshotChecksum(getScreenshotChecksums());
			page.setAnimatedImageUrls(this.getAnimatedImageUrls());
			page.setAnimatedImageChecksums(this.getAnimatedImageChecksums());
		} catch (NoSuchAlgorithmException | IOException e) {
			log.info("Error cloning page : " + this.getKey() + ";  "+e.getMessage());
		}
		return page;
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
		String src_template = BrowserService.extractTemplate(getSrc());
		return "pagestate::" + org.apache.commons.codec.digest.DigestUtils.sha256Hex(getUrl()+src_template);
	}

	@Deprecated
	public void addForm(Form form) {
		for (Form temp_form : this.forms) {
			if (temp_form.getKey().equals(form.getKey())) {
				return;
			}
		}
		this.forms.add(form);
	}

	@Deprecated
	public Set<Form> getForms() {
		return this.forms;
	}

	@Deprecated
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

	public String getScreenshotUrl() {
		return screenshot_url;
	}

	public void setScreenshotUrl(String screenshot_url) throws MalformedURLException, IOException {
		this.screenshot_url = screenshot_url;
		//addScreenshotChecksum(getFileChecksum(ImageIO.read(new URL(screenshot_url))));
	}

	public String getBrowser() {
		return browser;
	}

	public void setBrowser(String browser) {
		this.browser = browser;
	}

	@Deprecated
	public List<String> getScreenshotChecksums() {
		if(screenshot_checksums == null){
			return new ArrayList<String>();
		}
		return screenshot_checksums;
	}

	@Deprecated
	public void setScreenshotChecksum(List<String> screenshot_checksums) {
		this.screenshot_checksums = screenshot_checksums;
	}

	@Deprecated
	public boolean addScreenshotChecksum(String checksum){
		if(this.screenshot_checksums == null){
			this.screenshot_checksums = new ArrayList<String>();
		}
		boolean exists = false;
		for(String screenshot_checksum : getScreenshotChecksums()){
			if(checksum.equals(screenshot_checksum)){
				exists = true;
				break;
			}
		}
		if(!exists){
			return this.screenshot_checksums.add(checksum);
		}
		return false;
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

	public List<String> getAnimatedImageUrls() {
		return animated_image_urls;
	}

	public void setAnimatedImageUrls(List<String> animated_image_urls) {
		this.animated_image_urls = animated_image_urls;
	}

	public List<String> getAnimatedImageChecksums() {
		return animated_image_checksums;
	}

	public void setAnimatedImageChecksums(List<String> animated_image_checksums) {
		this.animated_image_checksums = animated_image_checksums;
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
	public String getFullPageChecksum() {
		return full_page_checksum;
	}
	public void setFullPageChecksum(String full_page_checksum) {
		this.full_page_checksum = full_page_checksum;
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

	public String getSrcChecksum() {
		return src_checksum;
	}

	public void setSrcChecksum(String src_checksum) {
		this.src_checksum = src_checksum;
	}

	public void addElements(List<ElementState> elements) {
		//check for duplicates before adding
		for(ElementState element : elements) {
			if(!this.elements.contains(element)) {				
				this.elements.add(element);
			}
		}
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
}
