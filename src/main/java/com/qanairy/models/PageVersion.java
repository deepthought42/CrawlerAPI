package com.qanairy.models;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.apache.commons.codec.binary.Hex;
import org.neo4j.ogm.annotation.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.qanairy.services.BrowserService;

/**
 * A reference to a web page
 *
 */
public class PageVersion extends LookseeObject {
	private static Logger log = LoggerFactory.getLogger(PageVersion.class);

	private String url;
	private String path;
	private String src;
	
	private String body;
	private String title;
	private Set<String> script_urls;
	private Set<String> stylesheet_urls;
	private Set<String> metadata;	
	private Set<String> favicon_url;
	
	@Relationship(type = "HAS")
	private List<PageState> page_states;


	public PageVersion() {
		super();
		setPageStates(new ArrayList<>());
	}
	

	/**
	 * Constructor 
	 * @param src
	 * @param title
	 * @param url
	 * @param path
	 */
	public PageVersion(String src, String title, String url, String path)
	{
		super();
		assert url != null;
		assert src != null;
		assert title != null;
		assert path != null;

		setPageStates(new ArrayList<>());
		setUrl(url);
		setBody( BrowserService.extractBody(src));
		setMetadata( BrowserService.extractMetadata(src) );
		setStylesheetUrls( BrowserService.extractStylesheets(src));
		setScriptUrls( BrowserService.extractScriptUrls(src));
		setFaviconUrl(BrowserService.extractIconLinks(src));
		setSrc( src );
		setTitle(title);
		setPath(path);
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
		if (!(o instanceof PageVersion))
			return false;

		PageVersion that = (PageVersion) o;
		
		return this.getKey().equals(that.getKey());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PageVersion clone() {
		PageVersion page = new PageVersion(getSrc(), getTitle(), getUrl(), getPath());
		return page;
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
		return "pagestate::" + org.apache.commons.codec.digest.DigestUtils.sha256Hex(BrowserService.extractTemplate(this.getBody()));
	}

	public String getSrc() {
		return src;
	}

	public void setSrc(String src) {
		this.src = src;
	}

	public String getTitle() {
		return title;
	}

	public void setPageStates(List<PageState> page_states) {
		this.page_states = page_states;
	}

	public List<PageState> getPageStates(){
		return this.page_states;
	}
	
	public boolean addPageState(PageState page_state_record) {
		return this.page_states.add(page_state_record);
	}
	
	public void setTitle(String title) {
		this.title = title;
	}


	public String getUrl() {
		return url;
	}


	public void setUrl(String url) {
		this.url = url;
	}


	public String getPath() {
		return path;
	}


	public void setPath(String path) {
		this.path = path;
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


	public Set<String>  getMetadata() {
		return metadata;
	}


	public void setMetadata(Set<String>  metadata) {
		this.metadata = metadata;
	}


	public String getBody() {
		return body;
	}


	public void setBody(String body) {
		this.body = body;
	}


	public Set<String> getFaviconUrl() {
		return favicon_url;
	}


	public void setFaviconUrl(Set<String> favicon_url) {
		this.favicon_url = favicon_url;
	}



}
