package com.looksee.models;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.looksee.models.enums.BrowserType;
import com.looksee.services.BrowserService;

/**
 * A reference to a web page
 *
 */
@NodeEntity
public class PageState extends LookseeObject {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(PageState.class);

	private String src;
	private String url;
	private boolean login_required;
	private boolean is_secure;

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
	private String page_name;
	
	private String title;
	private Set<String> script_urls;
	private Set<String> stylesheet_urls;
	private Set<String> metadata;	
	private Set<String> favicon_url;
	private Set<String> keywords;
	
	
	@Relationship(type = "HAS")
	private List<ElementState> elements;


	public PageState() {
		super();
		setElements(new ArrayList<>());
		setKeywords(new HashSet<>());
	}
	
	/**
	 * 	 Constructor
	 * 
	 * @param screenshot_url
	 * @param elements
	 * @param src
	 * @param isLandable
	 * @param scroll_x_offset
	 * @param scroll_y_offset
	 * @param viewport_width
	 * @param viewport_height
	 * @param browser
	 * @param full_page_screenshot_url
	 * @param full_page_width TODO
	 * @param full_page_height TODO
	 * @param url
	 * @param title TODO
	 * @param is_secure TODO
	 * @throws MalformedURLException 
	 */
	public PageState(String screenshot_url, 
			List<ElementState> elements, 
			String src, 
			boolean isLandable, 
			long scroll_x_offset, 
			long scroll_y_offset,
			int viewport_width, 
			int viewport_height, 
			BrowserType browser, 
			String full_page_screenshot_url,
			long full_page_width, 
			long full_page_height, 
			String url, 
			String title, 
			boolean is_secure
	) {
		assert screenshot_url != null;
		assert elements != null;
		assert src != null;
		assert browser != null;
		assert full_page_screenshot_url != null;
		assert url != null;
		assert !url.isEmpty();
		
		setViewportScreenshotUrl(screenshot_url);
		setViewportWidth(viewport_width);
		setViewportHeight(viewport_height);
		setBrowser(browser);
		setLastLandabilityCheck(LocalDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneId.systemDefault()));
		setElements(elements);
		setLandable(isLandable);
		setSrc(src);
		setScrollXOffset(scroll_x_offset);
		setScrollYOffset(scroll_y_offset);
	    setLoginRequired(false);
		setFullPageScreenshotUrl(full_page_screenshot_url);
		setFullPageWidth(full_page_width);
		setFullPageHeight(full_page_height);
		setUrl(url);
		setTitle(title);
		setIsSecure(is_secure);
		
		setPageName( generatePageName(getUrl()) );
		setMetadata( BrowserService.extractMetadata(src) );
		setStylesheetUrls( BrowserService.extractStylesheets(src));
		setScriptUrls( BrowserService.extractScriptUrls(src));
		setFaviconUrl(BrowserService.extractIconLinks(src));

		setKeywords(new HashSet<>());
		
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
	 *            the {@link PageVersion} object to compare current page to
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
		return new PageState(getViewportScreenshotUrl(), 
							 elements, 
							 getSrc(), 
							 isLandable(), 
							 getScrollXOffset(), 
							 getScrollYOffset(), 
							 getViewportWidth(), 
							 getViewportHeight(), 
							 getBrowser(), 
							 getFullPageScreenshotUrl(), 
							 getFullPageWidth(), 
							 getFullPageHeight(), 
							 getUrl(),
							 getTitle(),
							 isSecure() );
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

	/**
	 * Generates page name using path
	 * 
	 * @return
	 * @throws MalformedURLException
	 */
	public String generatePageName(String url) {
		String name = "";

		try {
			String path = new URL(url).getPath().trim();
			path = path.replace("/", " ");
			path = path.trim();
			if("/".equals(path) || path.isEmpty()){
				path = "home";
			}
			name += path;
			
			return name.trim();
		} catch(MalformedURLException e){}
		
		return url;
	}
	
	/**
	 * {@inheritDoc}
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 *
	 * @pre page != null
	 */
	public String generateKey() {
		/*
		List<ElementState> elements = new ArrayList<>(this.getElements());
		Collections.sort(elements);
		String key = "";
		for(ElementState element : elements) {
			key += element.getKey();
		}
		*/
		return "pagestate" + org.apache.commons.codec.digest.DigestUtils.sha256Hex( this.getUrl() + BrowserService.generalizeSrc(BrowserService.extractBody(this.getSrc()) ));
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

	public BrowserType getBrowser() {
		return BrowserType.create(browser);
	}

	public void setBrowser(BrowserType browser) {
		this.browser = browser.toString();
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

	public String getPageName() {
		return page_name;
	}

	public void setPageName(String page_name) {
		this.page_name = page_name;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Set<String> getScriptUrls() {
		return script_urls;
	}

	public void setScriptUrls(Set<String> script_urls) {
		this.script_urls = script_urls;
	}

	public Set<String> getStylesheetUrls() {
		return stylesheet_urls;
	}

	public void setStylesheetUrls(Set<String> stylesheet_urls) {
		this.stylesheet_urls = stylesheet_urls;
	}

	public Set<String> getMetadata() {
		return metadata;
	}

	public void setMetadata(Set<String> metadata) {
		this.metadata = metadata;
	}

	public Set<String> getFaviconUrl() {
		return favicon_url;
	}

	public void setFaviconUrl(Set<String> favicon_url) {
		this.favicon_url = favicon_url;
	}

	public boolean isSecure() {
		return is_secure;
	}

	public void setIsSecure(boolean is_secure) {
		this.is_secure = is_secure;
	}

	public Set<String> getKeywords() {
		return keywords;
	}

	public void setKeywords(Set<String> keywords) {
		this.keywords = keywords;
	}
}
