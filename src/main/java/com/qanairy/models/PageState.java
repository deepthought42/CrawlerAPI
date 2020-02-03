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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.commons.codec.binary.Hex;
import org.neo4j.ogm.annotation.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A reference to a web page state. State is considered to be the distinct combination of {@link ElementState}s
 *
 */
public class PageState extends Page implements Persistable, PathObject {
	private static Logger log = LoggerFactory.getLogger(PageState.class);

	private String src;
	private boolean landable;
	private boolean login_required;
	private LocalDateTime last_landability_check;
	private String screenshot_url;
	private String full_page_screenshot_url;
	private String full_page_checksum;
	private String browser;

	private int total_weight;
	private int image_weight;
	private long scrollXOffset;
	private long scrollYOffset;
	private int viewport_width;
	private int viewport_height;
	private long full_page_width;
	private long full_page_height;
	private String type;
	private List<String> screenshot_checksums;
	private List<String> animated_image_urls;
	private List<String> animated_image_checksums;
	

	@Relationship(type = "HAS")
	private List<Screenshot> screenshots;

	@Relationship(type = "HAS")
	private List<ElementState> elements;

	@Relationship(type = "HAS")
	private Set<Form> forms;

	public PageState() {
		setForms(new HashSet<>());
		setScreenshots(new ArrayList<>());
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
	 * @param screenshot
	 * @param elements
	 * @throws MalformedURLException
	 * @throws IOException
	 *
	 * @pre elements != null
	 * @pre screenshot_url != null;
	 */
	public PageState(String url, String screenshot_url, List<ElementState> elements, String src, long scroll_x_offset, long scroll_y_offset,
			int viewport_width, int viewport_height, String browser_name, Set<Form> forms) throws MalformedURLException, IOException{
		assert elements != null;
		assert screenshot_url != null;

		setType(PageState.class.getSimpleName());
		setUrl(url);
		setScreenshotUrl(screenshot_url);
		setViewportWidth(viewport_width);
		setViewportHeight(viewport_height);
		setBrowser(browser_name);
		setLastLandabilityCheck(LocalDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneId.systemDefault()));
		setElements(elements);
		setLandable(false);
		setImageWeight(0);
		setSrc(src);
		setScreenshotChecksum(new ArrayList<String>());
		setScrollXOffset(scroll_x_offset);
		setScrollYOffset(scroll_y_offset);
		setScreenshots(new ArrayList<Screenshot>());
		setAnimatedImageUrls(new ArrayList<String>());
		setAnimatedImageChecksums(new ArrayList<>());
	    setLoginRequired(false);
		setForms(forms);
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
	 * @pre screenshot_url != null;
	 *
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
	public PageState(String url, String screenshot_url, List<ElementState> elements, boolean isLandable,
			String src, long scroll_x_offset, long scroll_y_offset, int viewport_width, int viewport_height,
			String browser_name, Set<Form> forms) throws IOException, NoSuchAlgorithmException {
		assert elements != null;
		assert screenshot_url != null;

		setType(PageState.class.getSimpleName());
		setUrl(url);
		setScreenshotUrl(screenshot_url);
		setLastLandabilityCheck(LocalDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneId.systemDefault()));
		setElements(elements);
		setLandable(isLandable);
		setImageWeight(0);
		setBrowser(browser_name);
		setScrollXOffset(scroll_x_offset);
		setScrollYOffset(scroll_y_offset);
		setViewportWidth(viewport_width);
		setViewportHeight(viewport_height);
		setScreenshotChecksum(new ArrayList<String>());
		setSrc(src);
		setScreenshots(new ArrayList<Screenshot>());
		setAnimatedImageUrls(new ArrayList<String>());
		setAnimatedImageChecksums(new ArrayList<>());
		setLoginRequired(false);
		setForms(forms);
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
	public PathObject clone() {
		List<ElementState> elements = new ArrayList<ElementState>(getElements());

		PageState page = null;
		try {
			page = new PageState(getUrl(), getScreenshotUrl(), elements, isLandable(), getSrc(), getScrollXOffset(), getScrollYOffset(), getViewportWidth(), getViewportHeight(), getBrowser(), getForms());
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

	@Override
	public String getType() {
		return this.type;
	}

	@Override
	public void setType(String type) {
		this.type = type;
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
		String key = getUrl();
		List<ElementState> elements = getElements().stream().collect(Collectors.toList());
		Collections.sort(elements, (o1, o2) -> o1.getKey().compareTo(o2.getKey()));
		for(ElementState element : elements){
			key += element.getKey();
		}
		
		List<Form> forms = getForms().stream().collect(Collectors.toList());
		Collections.sort(forms, (o1, o2) -> o1.getKey().compareTo(o2.getKey()));
		for(Form form : forms) {
			key += form.getKey();
		}
		
		return "pagestate::" + org.apache.commons.codec.digest.DigestUtils.sha256Hex(key);
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
		addScreenshotChecksum(getFileChecksum(ImageIO.read(new URL(screenshot_url))));
	}

	public String getBrowser() {
		return browser;
	}

	public void setBrowser(String browser) {
		this.browser = browser;
	}

	public List<String> getScreenshotChecksums() {
		if(screenshot_checksums == null){
			return new ArrayList<String>();
		}
		return screenshot_checksums;
	}

	public void setScreenshotChecksum(List<String> screenshot_checksums) {
		this.screenshot_checksums = screenshot_checksums;
	}

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

	public List<Screenshot> getScreenshots() {
		return screenshots;
	}

	public void setScreenshots(List<Screenshot> screenshots) {
		this.screenshots = screenshots;
	}

	public void addScreenshot(Screenshot screenshot){
		boolean exists = false;

		if(this.screenshots == null){
			this.screenshots = new ArrayList<>();
		}
		for(Screenshot screenshot_record : this.screenshots){
			if(screenshot_record.getKey().equals(screenshot.getKey())){
				exists = true;
			}
		}
		if(!exists){
			this.screenshots.add(screenshot);
		}
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
}
