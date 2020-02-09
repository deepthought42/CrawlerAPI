package com.qanairy.services;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.grid.common.exception.GridException;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.InvalidSelectorException;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.minion.aws.UploadObjectSingleOperation;
import com.minion.browsing.Browser;
import com.minion.browsing.form.ElementRuleExtractor;
import com.qanairy.helpers.BrowserConnectionHelper;
import com.qanairy.models.Attribute;
import com.qanairy.models.Domain;
import com.qanairy.models.Form;
import com.qanairy.models.Page;
import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.Template;
import com.qanairy.models.Test;
import com.qanairy.models.enums.BrowserEnvironment;
import com.qanairy.models.enums.BrowserType;
import com.qanairy.models.enums.ElementClassification;
import com.qanairy.models.enums.FormStatus;
import com.qanairy.models.enums.FormType;
import com.qanairy.models.enums.TemplateType;
import com.qanairy.utils.ArrayUtils;
import com.qanairy.utils.BrowserUtils;
import com.qanairy.utils.PathUtils;

import us.codecraft.xsoup.Xsoup;

/**
 * A collection of methods for interacting with the {@link Browser} session object
 *
 */
@Component
public class BrowserService {
	private static Logger log = LoggerFactory.getLogger(BrowserService.class);

	@Autowired
	private ElementRuleExtractor extractor;

	@Autowired
	private ElementStateService element_service;
	
	@Autowired
	private PageStateService page_state_service;

	@Autowired
	private FormService form_service;

	@Autowired
	private DomainService domain_service;

	private static String[] valid_xpath_attributes = {"class", "id", "name", "title"};

	/**
	 * retrieves a new browser connection
	 *
	 * @param browser_name name of the browser (ie. firefox, chrome)
	 *
	 * @return new {@link Browser} instance
	 * @throws MalformedURLException
	 *
	 * @pre browser_name != null;
	 * @pre !browser_name.isEmpty();
	 */
	public Browser getConnection(String browser_name, BrowserEnvironment browser_env) throws MalformedURLException {
		assert browser_name != null;
		assert !browser_name.isEmpty();
		return BrowserConnectionHelper.getConnection(BrowserType.create(browser_name), browser_env);
	}

	/**
	 *
	 * @param browser_name
	 * @param page_state
	 * @return
	 * @throws  
	 * @throws IOException
	 * @throws GridException
	 */
	public static boolean checkIfLandable(PageState page_state, Test test) {
		//find last page in path
		PageState last_page = PathUtils.getLastPageState(test.getPathObjects());
		
		return !last_page.getUrl().equals(page_state.getUrl());
	}

	public static boolean isElementLargerThanViewport(Browser browser, Dimension element_size) {
		int height = element_size.getHeight();
		int width = element_size.getWidth();

		return width >= browser.getViewportSize().getWidth()
				 || height >= browser.getViewportSize().getHeight();
	}

	/**
	 * 
	 * @param xpath
	 * @param attributes
	 * @param css_values
	 * @param element
	 * @param classification
	 * @param checksum
	 * @param dimension TODO
	 * @return
	 */
	public static ElementState buildElementState(
			String xpath, 
			Set<Attribute> attributes, 
			Map<String, String> css_values, 
			Element element, 
			ElementClassification classification,
			Point location, 
			Dimension dimension
	) {
		assert xpath != null && !xpath.isEmpty();
		assert attributes != null;
		assert element != null;
		assert classification != null;
		assert location != null;
		assert dimension != null;
		
		ElementState element_state = new ElementState();
		element_state.setXpath(xpath);
		element_state.setAttributes(attributes);
		element_state.setOuterHtml(element.outerHtml());
		element_state.setInnerHtml(element.html());
		element_state.setTemplate(extractTemplate(element.outerHtml(), element.text()));
		element_state.setName(element.tagName());
		element_state.setText(element.text());
		element_state.setIsPartOfForm(isElementPartOfForm(element));
		element_state.setClassification(classification);
		element_state.setCssValues(css_values);
		element_state.setKey(element_state.generateKey());
		element_state.setType("ElementState");
		element_state.setXLocation(location.getX());
		element_state.setYLocation(location.getY());
		element_state.setWidth(dimension.getWidth());
		element_state.setHeight(dimension.getHeight());
		
		return element_state;
	}

	/**
	 *Constructs a page object that contains all child elements that are considered to be potentially expandable.
	 *
	 * @return page {@linkplain PageState}
	 * @throws Exception 
	 * 
	 * @pre browser != null
	 */
	public PageState buildPageState(String user_id, Domain domain, Browser browser) throws Exception{
		assert browser != null;
		
		//retrieve landable page state associated with page with given url
		String browser_url = browser.getDriver().getCurrentUrl();
		String host = new URL(browser_url).getHost();
		String url_without_params = BrowserUtils.sanitizeUrl(browser_url);		

		//DONT MOVE THIS. THIS IS HERE TO MAKE SURE THAT WE GET THE UNALTERED SCREENSHOT OF THE VIEWPORT BEFORE DOING ANYTHING ELSE!!
		BufferedImage viewport_screenshot = browser.getViewportScreenshot();
		String screenshot_checksum = PageState.getFileChecksum(viewport_screenshot);

		BufferedImage full_page_screenshot = browser.getFullPageScreenshot();		
		String full_page_screenshot_checksum = PageState.getFileChecksum(full_page_screenshot);
		//List<PageState> page_states = page_state_service.findByScreenshotChecksum(user_id, domain.getUrl(), screenshot_checksum);
		List<PageState> page_states = page_state_service.findByFullPageScreenshotChecksum(user_id, domain.getUrl(), full_page_screenshot_checksum);

		if(page_states.isEmpty()) {
			log.warn("Retrieving all DOM elements for page  :: "+url_without_params);

			long start_time = System.currentTimeMillis();
			//redo this logic generate all child elements that qualify for expansion. This can mean reducing out any undesirable html tags.
		  	List<ElementState> elements = getDomElementTreeRecursive( browser.getDriver().getPageSource(), browser, user_id, domain );

		  	long end_time = System.currentTimeMillis();
			log.warn("element state time to get all elements ::  "+(end_time-start_time));

			log.warn("DOM elements found :: "+elements.size());

			
			String full_page_screenshot_url = UploadObjectSingleOperation.saveImageToS3(full_page_screenshot, host, full_page_screenshot_checksum, BrowserType.create(browser.getBrowserName()),user_id);
			full_page_screenshot.flush();

			//extract visible elements from list of elementstates provided
			String viewport_screenshot_url = UploadObjectSingleOperation.saveImageToS3(viewport_screenshot, host, screenshot_checksum, BrowserType.create(browser.getBrowserName()), user_id);
			viewport_screenshot.flush();
			
			log.warn("creating new page state object ");
			PageState page_state = new PageState( url_without_params,
					viewport_screenshot_url,
					elements,
					org.apache.commons.codec.digest.DigestUtils.sha256Hex(Browser.cleanSrc(browser.getDriver().getPageSource())),
					browser.getXScrollOffset(),
					browser.getYScrollOffset(),
					browser.getViewportSize().width,
					browser.getViewportSize().height,
					browser.getBrowserName(), 
					new HashSet<Form>(), 
					full_page_screenshot_url, 
					full_page_screenshot_checksum);

			page_state.addScreenshotChecksum(screenshot_checksum);
			page_state.addScreenshotChecksum(full_page_screenshot_checksum);
			page_state.setFullPageWidth(full_page_screenshot.getWidth());
			page_state.setFullPageHeight(full_page_screenshot.getHeight());
			return page_state;
		}
		
		PageState page_state = page_states.get(0);
		page_state.setElements(page_state_service.getElementStates(user_id, page_states.get(0).getKey()));
		log.warn("loaded page elements from db :: " +page_state.getElements().size());
		return page_state;
	}

	/**
	 *Constructs a {@link Page} that contains all references to all elements on the page.
	 *
	 * @return page {@linkplain PageState}
	 * @throws Exception 
	 * 
	 * @pre url != null
	 */
	public Page buildPage(String user_id, String url) throws Exception{
		assert url != null;
		assert user_id != null;
		assert !user_id.isEmpty();
		
		String url_without_params = BrowserUtils.sanitizeUrl(url);
		  	
	  	Page page = new Page(url_without_params);
		log.warn("page :: "+page);
		return page;
	}
	
	private List<ElementState> getDomElementTreeRecursive(String pageSource, Browser browser, String user_id, Domain domain) throws IOException {
		assert domain != null;
		assert user_id != null;
		assert !user_id.isEmpty();
		assert pageSource != null;
		assert !pageSource.isEmpty();
		assert browser != null;
		
		Document html_doc = Jsoup.parse(pageSource);
		Element root = html_doc.getElementsByTag("html").get(0);
		Map<String, Integer> xpath_cnt = new HashMap<>();
		return getElements(root, html_doc, browser, xpath_cnt, user_id, domain, "//html");
	}

	/**
	 * Builds a tree of elements and establishes connections between {@link ElementState} objects in the database
	 * 
	 * @param root
	 * @param html_doc
	 * @param browser
	 * @param xpath_cnt
	 * @param user_id
	 * @param root_xpath TODO
	 * @param root_web_elem TODO
	 * @param domain_url
	 * @return set of {@link ElementState} objects
	 * @throws IOException 
	 * 
	 * @pre root != null
	 * @pre assert html_doc != null
	 * @pre assert browser != null
	 */
	private List<ElementState> getElements(Element root, Document html_doc, Browser browser,
			Map<String, Integer> xpath_cnt, String user_id, Domain domain, String root_xpath) throws IOException {
		assert root != null;
		assert html_doc != null;
		assert browser != null;
		assert root_xpath != null;
		assert !root_xpath.isEmpty();
		
		List<ElementState> elements = new ArrayList<>();
		List<Element> child_elements = new ArrayList<>(root.children());
		for(int idx = 0; idx < child_elements.size(); idx++) {
			Element element = child_elements.get(idx);
			
			String xpath = root_xpath + "/" + element.tagName();
			if(!xpath_cnt.containsKey(xpath)) {
				xpath_cnt.put(xpath, 1);
			}
			else {
				xpath_cnt.put(xpath, xpath_cnt.get(xpath)+1);
			}
			
			xpath = xpath + "["+xpath_cnt.get(xpath)+"]";
			//String xpath = generateXpathUsingJsoup(element, html_doc, element.attributes(), xpath_cnt);
			WebElement web_element = browser.findWebElementByXpath(xpath);

			//Map<String, String> css_values = Browser.loadCssProperties(web_element);
			if(!web_element.isDisplayed()) {
				continue;
			}
			
			Set<Attribute> attributes = generateAttributesUsingJsoup(element);
		
			Dimension element_size = web_element.getSize();
			Point element_location = web_element.getLocation();
			
			if(element.children().size() == 0 ) {
					ElementState element_state = buildElementState(xpath, attributes, new HashMap<>(), element, ElementClassification.CHILD, element_location, element_size);
					ElementState element_record = element_service.findByKey(user_id, element_state.getKey());
					
					if(element_record != null) {
						elements.add(element_record);
					}
					else{
						if(hasWidthAndHeight(element_size) && !doesElementHaveNegativePosition(element_location) && !isElementLargerThanViewport(browser, element_size)) {
							BufferedImage element_screenshot = browser.getElementScreenshot(web_element);
							String checksum = PageState.getFileChecksum(element_screenshot);
							String screenshot_url = UploadObjectSingleOperation.saveImageToS3(element_screenshot, new URL(domain.getProtocol() + "://"+domain.getUrl()).getHost(), element_state.getKey(), BrowserType.create(browser.getBrowserName()), user_id);
							element_state.setScreenshotUrl(screenshot_url);
							element_state.setScreenshotChecksum(checksum);
							element_state = element_service.save(user_id, element_state);
							elements.add(element_state);
						}
						elements.add(element_state);
					}
			}
			else {
				ElementClassification classification = null;
				
				if(isSliderElement(element)) {
					classification = ElementClassification.SLIDER;
				}
				else {
					classification = ElementClassification.ANCESTOR;
				}
				
				//Map<String, String> css_map = loadCssAttriutes();
				
				ElementState element_state = buildElementState(xpath, attributes, new HashMap<>(), element, classification, element_location, element_size);
				ElementState element_record = element_service.findByKey(user_id, element_state.getKey());
				//get all child element states
				List<ElementState> element_states = new ArrayList<>();
				
				if(element_record != null) {
					element_states = element_service.getChildElements(user_id, element_record.getKey());
					element_state = element_record;
				}
				else{
					/*
					if(hasWidthAndHeight(element_size) && !doesElementHaveNegativePosition(element_location) && !isElementLargerThanViewport(browser, element_size)) {
						BufferedImage element_screenshot = browser.getElementScreenshot(web_element);
						String checksum = PageState.getFileChecksum(element_screenshot);
						String screenshot_url = UploadObjectSingleOperation.saveImageToS3(element_screenshot, new URL(domain.getProtocol() + "://"+domain.getUrl()).getHost(), element_state.getKey(), BrowserType.create(browser.getBrowserName()), user_id);
						element_state.setScreenshotUrl(screenshot_url);
						element_state.setScreenshotChecksum(checksum);
					}
					*/
					element_states = getElements(element, html_doc, browser, xpath_cnt, user_id, domain, xpath);
					
					element_state.setChildElements(element_states);
					element_state = element_service.save(user_id, element_state);
					elements.add(element_state);
				}
				
				elements.addAll(element_states);
			}
		}		
		
		return elements;
	}

	/**
	 * Checks if {@link Element} is a part of a slideshow container
	 * 
	 * @param element
	 * @return
	 */
	private static boolean isSliderElement(Element element) {
		
		for(org.jsoup.nodes.Attribute attr : element.attributes()) {
			if(attr.getValue().toLowerCase().contains("slider") || attr.getKey().toLowerCase().contains("slider")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Removes all {@link ElementState}s that have a negative or 0 value for the x or y coordinates
	 *
	 * @param web_elements
	 * @param is_element_state
	 *
	 * @pre web_elements != null
	 *
	 * @return filtered list of {@link ElementState}s
	 */
	public static List<ElementState> filterElementsWithNegativePositions(List<ElementState> web_elements, boolean is_element_state) {
		assert(web_elements != null);

		List<ElementState> elements = new ArrayList<>();

		for(ElementState element : web_elements){
			if(element.getXLocation() >= 0 && element.getYLocation() >= 0){
				elements.add(element);
			}
		}

		return elements;
	}

	public static List<ElementState> filterNotVisibleInViewport(int x_offset, int y_offset, List<ElementState> web_elements, Dimension viewport_size, boolean is_element_state) {
		List<ElementState> elements = new ArrayList<>();

		for(ElementState element : web_elements){
			if(isElementVisibleInPane( x_offset, y_offset, element, viewport_size)){
				elements.add(element);
			}
		}

		return elements;
	}

	public static List<WebElement> fitlerNonDisplayedElements(List<WebElement> web_elements) {
		List<WebElement> filtered_elems = new ArrayList<WebElement>();
		for(WebElement elem : web_elements){
			if(elem.isDisplayed()){
				filtered_elems.add(elem);
			}
		}
		return filtered_elems;
	}

	public static List<WebElement> filterNonChildElements(List<WebElement> web_elements) {
		List<WebElement> filtered_elems = new ArrayList<WebElement>();
		for(WebElement elem : web_elements){
			boolean is_child = getChildElements(elem).isEmpty();
			if(is_child){
				filtered_elems.add(elem);
			}
		}
		return filtered_elems;
	}

	public static List<WebElement> filterElementsWithNegativePositions(List<WebElement> web_elements) {
		List<WebElement> elements = new ArrayList<>();

		for(WebElement element : web_elements){
			Point location = element.getLocation();
			if(location.getX() >= 0 && location.getY() >= 0){
				elements.add(element);
			}
		}

		return elements;
	}

	public static List<WebElement> filterNotVisibleInViewport(long x_offset, long y_offset, List<WebElement> web_elements, Dimension viewport_size) {
		List<WebElement> elements = new ArrayList<>();

		for(WebElement element : web_elements){
			if(isElementVisibleInPane( x_offset, y_offset, element, viewport_size)){
				elements.add(element);
			}
		}

		return elements;
	}

	/**
	 * Filters out html, body, script and link tags
	 *
	 * @param web_elements
	 * @return
	 */
	public static List<WebElement> filterStructureTags(List<WebElement> web_elements) {
		List<WebElement> elements = new ArrayList<>();

		for(WebElement element : web_elements){
			String tag_name = element.getTagName();
			if(isStructureTag(tag_name)){
				continue;
			}
			elements.add(element);
		}
		return elements;
	}
	
	private static boolean isElementPartOfForm(Element element) {
		Element new_element = element;
		while(new_element != null && !new_element.tagName().equals("body") && !new_element.tagName().equals("html")){
			if(new_element.tagName().equals("form")){
				return true;
			}
			new_element = new_element.parent();
		}
		return false;
	}
	
	/**
	 *
	 * @param browser
	 * @param elem
	 * @param page_screenshot
	 * @param xpath
	 * @return
	 * @throws IOException
	 */
	public ElementState buildElementState(Browser browser, WebElement elem, String xpath, Set<Attribute> attributes, Map<String, String> css_props, Point location, Dimension element_size, String screenshot_url, String checksum, boolean is_displayed) throws IOException{
		long start_time = System.currentTimeMillis();

		ElementState page_element = new ElementState(elem.getText(), xpath, elem.getTagName(), attributes, css_props, screenshot_url, checksum,
										location.getX(), location.getY(), element_size.getWidth(), element_size.getHeight(), elem.getAttribute("innerHTML"), ElementClassification.ANCESTOR, elem.isDisplayed());
		page_element.setOuterHtml(elem.getAttribute("outerHTML"));
		page_element.setIsPartOfForm(false);
		page_element.setTemplate(extractTemplate(elem.getAttribute("outerHTML"), elem.getText()));
		log.debug("total time to save element state :: " + (System.currentTimeMillis() - start_time) + "    :  xpath time ::    "+xpath);

		return page_element;
	}

	public static boolean doesElementHaveNegativePosition(Point location) {
		return location.getX() < 0 || location.getY() < 0;
	}

	public static boolean hasWidthAndHeight(Dimension dimension) {
		return dimension.getHeight() > 1 && dimension.getWidth() > 1;
	}

	/**
	 * Filters out html, body, link, title, script, meta, head, iframe, or noscript tags
	 *
	 * @param tag_name
	 *
	 * @pre tag_name != null
	 *
	 * @return true if tag name is html, body, link, title, script, meta, head, iframe, or noscript
	 */
	public static boolean isStructureTag(String tag_name) {
		assert tag_name != null;

		return "html".contentEquals(tag_name) || "body".contentEquals(tag_name)
				|| "link".contentEquals(tag_name) || "script".contentEquals(tag_name)
				|| "head".contentEquals(tag_name) || "noscript".contentEquals(tag_name)
				|| "g".contentEquals(tag_name) || "path".contentEquals(tag_name) || "svg".contentEquals(tag_name) || "polygon".contentEquals(tag_name)
				|| "br".contentEquals(tag_name) || "style".contentEquals(tag_name) || "polyline".contentEquals(tag_name) || "use".contentEquals(tag_name)
				|| "template".contentEquals(tag_name) || "audio".contentEquals(tag_name);
	}

	public static List<WebElement> filterNoWidthOrHeight(List<WebElement> web_elements) {
		List<WebElement> elements = new ArrayList<WebElement>(web_elements.size());
		for(WebElement element : web_elements){
			Dimension dimension = element.getSize();
			if(dimension.getHeight() > 1 && dimension.getWidth() > 1){
				elements.add(element);
			}
		}

		return elements;
	}

	public static List<ElementState> filterNoWidthOrHeight(List<ElementState> web_elements, boolean is_element_state) {
		List<ElementState> elements = new ArrayList<>(web_elements.size());
		for(ElementState element : web_elements){
			if(element.getHeight() > 1 && element.getWidth() > 1){
				elements.add(element);
			}
		}

		return elements;
	}

	public static boolean isElementVisibleInPane(Browser browser, Point location, Dimension size){
		assert browser != null;
		assert location != null;
		assert size != null;

		long y_offset = browser.getYScrollOffset();
		long x_offset = browser.getXScrollOffset();

		int x = location.getX();
		int y = location.getY();

		int height = size.getHeight();
		int width = size.getWidth();

		return x >= x_offset && y >= y_offset && ((x-x_offset)+width) <= (browser.getViewportSize().getWidth())
				&& ((y-y_offset)+height) <= (browser.getViewportSize().getHeight());
	}

	public static boolean isElementVisibleInPane(Browser browser, ElementState elem){
		assert elem != null;
		assert browser != null;

		long x_offset = browser.getXScrollOffset();
		long y_offset = browser.getYScrollOffset();

		long x = elem.getXLocation();
		long y = elem.getYLocation();

		int height = elem.getHeight();
		int width = elem.getWidth();

		return x >= x_offset && y >= y_offset && ((x-x_offset)+width) <= (browser.getViewportSize().getWidth())
				&& ((y-y_offset)+height) <= (browser.getViewportSize().getHeight());
	}

	public static boolean isElementVisibleInPane(long x_offset, long y_offset, WebElement elem, Dimension viewport_size){
		Point location = elem.getLocation();
		int x = location.getX();
		int y = location.getY();

		Dimension dimension = elem.getSize();
		int height = dimension.getHeight();
		int width = dimension.getWidth();

		return x >= x_offset && y >= y_offset && ((x-x_offset)+width) <= (viewport_size.getWidth())
				&& ((y-y_offset)+height) <= (viewport_size.getHeight());
	}

	public static boolean isElementVisibleInPane(int x_offset, int y_offset, ElementState elem, Dimension viewport_size){

		long x = elem.getXLocation();
		long y = elem.getYLocation();

		int height = elem.getHeight();
		int width = elem.getWidth();

		return x >= x_offset && y >= y_offset && ((x-x_offset)+width) <= (viewport_size.getWidth())
				&& ((y-y_offset)+height) <= (viewport_size.getHeight());
	}

	/**
	 * Get immediate child elements for a given element
	 *
	 * @param elem	WebElement to get children for
	 * @return list of WebElements
	 */
	public static List<WebElement> getChildElements(WebElement elem) throws WebDriverException{
		return elem.findElements(By.xpath("./*"));
	}

	/**
	 * Get immediate child elements for a given element
	 *
	 * @param elem	WebElement to get children for
	 * @return list of WebElements
	 */
	public static List<WebElement> getNestedElements(WebElement elem) throws WebDriverException{
		return elem.findElements(By.xpath(".//*"));
	}

	/**
	 * Get immediate parent elements for a given element
	 *
	 * @param elem	{@linkplain WebElement) to get parent of
	 * @return parent {@linkplain WebElement)
	 */
	public WebElement getParentElement(WebElement elem) throws WebDriverException{
		return elem.findElement(By.xpath(".."));
	}

	/**
	 *
	 * @param a_xPathQueryString
	 * @return
	 */
	public static String generateConcatForXPath(String a_xPathQueryString)
	{
	    String returnString = "";
	    String searchString = a_xPathQueryString;

	    int quotePos = searchString.indexOf("\"");
	    if (quotePos == -1)
	    {
	        returnString = "'" + searchString + "'";
	    }
	    else
	    {
	        //returnString = "concat(";
	        while (quotePos != -1)
	        {

	            String substring = searchString.substring(0, quotePos);
	            if(quotePos <= 0 || searchString.length() == 0){
	        		continue;
	        	}
	            returnString += "'" + substring + "', ";
	            String tail_string = searchString.substring(quotePos + 1, searchString.length());
	            if(tail_string.length() == 0){
	            	continue;
	            }
                //must be a double quote
                returnString += "'\"', ";
                searchString = tail_string;
	            quotePos = searchString.indexOf("\"");
	        }
	        returnString += "'" + searchString;
	    }

	    return returnString;
	}

	public static String cleanAttributeValues(String attribute_values_string) {
		String escaped = attribute_values_string.replaceAll("[\\t\\n\\r]+"," ");
		escaped = escaped.trim().replaceAll("\\s+", " ");
		escaped = escaped.replace("\"", "\\\"");
		return escaped.replace("\'", "'");
	}

	/**
	 * generates a unique xpath for this element.
	 *
	 * @return an xpath that identifies this element uniquely
	 */
	public String generateXpath(WebElement element, WebDriver driver, Set<Attribute> attributes){
		List<String> attributeChecks = new ArrayList<>();
		List<String> valid_attributes = Arrays.asList(valid_xpath_attributes);
		String xpath = "/"+element.getTagName();
		for(Attribute attr : attributes){
			if(valid_attributes.contains(attr.getName())){
				String attribute_values = ArrayUtils.joinArray(attr.getVals().toArray(new String[attr.getVals().size()]));
				String trimmed_values = cleanAttributeValues(attribute_values.trim());

				if(trimmed_values.length() > 0 && !trimmed_values.contains("javascript") && !trimmed_values.contains("void()")){
					attributeChecks.add("contains(@" + attr.getName() + ",\"" + trimmed_values.split(" ")[0] + "\")");
				}
			}
		}
		if(attributeChecks.size()>0){
			xpath += "[";
			xpath += attributeChecks.get(0).toString();
			xpath += "]";
		}

	    WebElement parent = element;
	    String parent_tag_name = parent.getTagName();
	    int count = 0;
	    while(!"html".equals(parent_tag_name) && !"body".equals(parent_tag_name) && count < 3){
	    	try{
	    		parent = getParentElement(parent);
	    		if(driver.findElements(By.xpath("//"+parent.getTagName() + xpath)).size() == 1){
	    			return "//"+parent.getTagName() + xpath;
	    		}
	    		else{
		    		xpath = "/" + parent.getTagName() + xpath;
	    		}
	    	}catch(InvalidSelectorException e){
	    		parent = null;
	    		log.warn("Invalid selector exception occurred while generating xpath through parent nodes");
	    		break;
	    	}
	    	count++;
	    }
	    xpath = "/"+xpath;
		return uniqifyXpath(element, xpath, driver);
	}

	/**
	 * generates a unique xpath for this element.
	 *
	 * @return an xpath that identifies this element uniquely
	 */
	public static String generateXpathUsingJsoup(Element element, Document doc, Attributes attributes, Map<String, Integer> xpath_cnt){
		List<String> attributeChecks = new ArrayList<>();
		List<String> valid_attributes = Arrays.asList(valid_xpath_attributes);
		Element element_copy = element.clone();
		String xpath = "/"+element.tagName();
		for(org.jsoup.nodes.Attribute attr : attributes.asList()){
			if(valid_attributes.contains(attr.getKey())){
				String attribute_values = attr.getValue();
				String trimmed_values = cleanAttributeValues(attribute_values.trim());
				//check if attribute is auto generated
				String reduced_values = removeAutoGeneratedValues(trimmed_values);
				if(reduced_values.length() > 0 && !reduced_values.contains("javascript") && !reduced_values.contains("void()")){
					attributeChecks.add("contains(@" + attr.getKey() + ",\"" + reduced_values.split(" ")[0] + "\")");
				}
			}
		}

		if(attributeChecks.size()>0){
			xpath += "[";
			xpath += attributeChecks.get(0).toString();
			xpath += "]";
		}

		Element last_element = element;
		Element parent = null;
		String last_element_tagname = last_element.tagName();
	    int count = 0;
	    while(!"html".equals(last_element_tagname) && !"body".equals(last_element_tagname) && count < 3){
	    	try{
	    		parent = last_element.parent();

	    		if(!isStructureTag(parent.tagName())){
		    		if( Xsoup.compile("//"+parent.tagName() + xpath).evaluate(doc).getElements().isEmpty()){
		    			break;
		    		}
	    			else if( Xsoup.compile("//"+parent.tagName() + xpath).evaluate(doc).getElements().size() == 1){
		    			return "//"+parent.tagName() + xpath;
		    		}
		    		else{
			    		xpath = "/" + parent.tagName() + xpath;
		    		}
		    		last_element = parent;
	    		}
	    		else{
	    			break;
	    		}
	    	}catch(InvalidSelectorException e){
	    		parent = null;
	    		log.warn("Invalid selector exception occurred while generating xpath through parent nodes");
	    		break;
	    	}
	    	count++;
	    }
	    if(!xpath.startsWith("//")){
			xpath = "/"+xpath;
		}

		return uniqifyXpath(element_copy, xpath, doc, xpath_cnt);
	}

	private static String removeAutoGeneratedValues(String trimmed_values) {
		String[] values = trimmed_values.split(" ");
		List<String> reduced_vals = new ArrayList<>();
		for(String val : values){

			//check if value is auto-generated
			if(!isAutoGenerated(val)){
				reduced_vals.add(val);
			}
		}
		return String.join(" ", reduced_vals);
	}

	private static boolean isAutoGenerated(String val) {
		//check if value ends in a number
		return val.length() > 0 && Character.isDigit(val.charAt(val.length()-1));
	}

	/**
	 * generates a unique xpath for this element.
	 *
	 * @return an xpath that identifies this element uniquely
	 */
	public static Set<Attribute> generateAttributesUsingJsoup(Element element){
		Set<Attribute> attribute_list = new HashSet<Attribute>();
		for(org.jsoup.nodes.Attribute attribute : element.attributes() ){
			Attribute qanairy_attribute = new Attribute(attribute.getKey(), Arrays.asList(attribute.getValue().split(" ")));
			attribute_list.add(qanairy_attribute);
		}

		return attribute_list;
	}

	/**
	 * creates a unique xpath based on a given hash of xpaths
	 *
	 * @param driver
	 * @param xpathHash
	 *
	 * @return
	 */
	public static String uniqifyXpath(Element elem, String xpath, Document doc, Map<String, Integer> xpath_cnt){
		try {
			List<Element> elements = Xsoup.compile(xpath).evaluate(doc).getElements();
			if(elements.size() > 1){
				int count = 0;
				if(xpath_cnt.containsKey(xpath)){
					count = xpath_cnt.get(xpath);
				}
				xpath_cnt.put(xpath, ++count);
				String unique_xpath = "("+xpath+")[" + count + "]";
				return unique_xpath;
			}

		}catch(InvalidSelectorException e){
			log.warn(e.getMessage());
		}

		return xpath;
	}

	/**
	 * creates a unique xpath based on a given hash of xpaths
	 *
	 * @param driver
	 * @param xpathHash
	 *
	 * @return
	 */
	public static String uniqifyXpath(WebElement elem, String xpath, WebDriver driver){
		try {
			List<WebElement> elements = driver.findElements(By.xpath(xpath));
			String element_tag_name = elem.getTagName();

			if(elements.size()>1){
				int count = 1;
				for(WebElement element : elements){
					if(element.getTagName().equals(element_tag_name)
							&& element.getLocation().getX() == elem.getLocation().getX()
							&& element.getLocation().getY() == elem.getLocation().getY()){
						return "("+xpath+")[" + count + "]";
					}
					count++;
				}
			}

		}catch(InvalidSelectorException e){
			log.error(e.getMessage());
		}

		return xpath;
	}

	/**
	 * Extracts all forms including the child inputs and associated labels.
	 *
	 * @param elem
	 * @param tag
	 * @param driver
	 * @return
	 * @throws Exception
	 */
	public Set<Form> extractAllForms(String user_id, Domain domain, Browser browser) throws Exception {
		Set<Form> form_list = new HashSet<Form>();
		log.warn("extracting forms from page with url    ::     "+browser.getDriver().getCurrentUrl());
		List<WebElement> form_elements = browser.getDriver().findElements(By.xpath("//form"));

		String host = domain.getHost();
		for(WebElement form_elem : form_elements){
			//BrowserUtils.detectShortAnimation(browser, page.getUrl());
			if(!form_elem.isDisplayed() || doesElementHaveNegativePosition(form_elem.getLocation())) {
				continue;
			}
			
			//if form has 0 or negative height or width then get parent element
			while(form_elem.getSize().getHeight() <= 0 || form_elem.getSize().getWidth() <= 0 ) {
				form_elem = getParentElement(form_elem);
			}
			
			BufferedImage img = browser.getElementScreenshot(form_elem);
			String checksum = PageState.getFileChecksum(img);
			//Map<String, String> css_map = Browser.loadCssProperties(form_elem);
			ElementState form_tag = new ElementState(form_elem.getText(), uniqifyXpath(form_elem, "//form", browser.getDriver()), "form", browser.extractAttributes(form_elem), new HashMap<>(), "", checksum, form_elem.getLocation().getX(), form_elem.getLocation().getY(), form_elem.getSize().getWidth(), form_elem.getSize().getHeight(), form_elem.getAttribute("innerHTML"), ElementClassification.ANCESTOR, form_elem.isDisplayed());
			form_tag = element_service.saveFormElement(user_id, form_tag);
			String screenshot_url = UploadObjectSingleOperation.saveImageToS3(img, host, checksum, BrowserType.create(browser.getBrowserName()), user_id);
			form_tag.setScreenshotUrl(screenshot_url);
			double[] weights = new double[1];
		
			Set<Form> forms = domain_service.getForms(user_id, domain.getUrl());
			Form form = new Form(form_tag, new ArrayList<ElementState>(), findFormSubmitButton(user_id, form_elem, browser),
									"Form #"+(forms.size()+1), weights, FormType.UNKNOWN, new Date(), FormStatus.DISCOVERED );

			List<WebElement> input_elements =  form_elem.findElements(By.xpath(".//input"));
			input_elements = BrowserService.fitlerNonDisplayedElements(input_elements);
			input_elements = BrowserService.filterStructureTags(input_elements);
			input_elements = BrowserService.filterNoWidthOrHeight(input_elements);
			input_elements = BrowserService.filterElementsWithNegativePositions(input_elements);
			input_elements = BrowserService.filterAllButInputs(input_elements);
			form.setFormFields(buildFormFields(user_id, input_elements, browser));

			for(double d: form.getPrediction()){
				log.warn("PREDICTION ::: "+d);
			}

			log.info("weights :: "+ form.getPrediction());
			form.setType(FormType.UNKNOWN);
			form.setDateDiscovered(new Date());
			log.info("form record discovered date :: "+form.getDateDiscovered());

			int form_count = domain_service.getFormCount(user_id, domain.getUrl());
			form.setName("Form #"+(form_count+1));
			log.info("name :: "+form.getName());

			Form form_record = form_service.findByKey(user_id, domain.getUrl(), form.getKey());
			if(form_record != null) {
				continue;
			}
			form_list.add(form);
		}
		return form_list;
	}

	private static List<WebElement> filterAllButInputs(List<WebElement> elements) {
		List<WebElement> filtered_elems = new ArrayList<WebElement>();
		for(WebElement elem : elements){
			if(elem.getTagName().equals("input")){
				filtered_elems.add(elem);
			}
		}
		return filtered_elems;
	}

	private List<ElementState> buildFormFields(String user_id, List<WebElement> input_elements, Browser browser) throws IOException {
		List<ElementState> elements = new ArrayList<>();
		for(WebElement input_elem : input_elements){
			boolean submit_elem_found = false;

			Set<Attribute> attributes = browser.extractAttributes(input_elem);
			for(Attribute attribute : attributes){
				if(attribute.contains("submit")){
					submit_elem_found = true;
					break;
				}
			}

			if(submit_elem_found){
				continue;
			}
			
			ElementState input_tag = new ElementState(input_elem.getText(), generateXpath(input_elem, browser.getDriver(), attributes), input_elem.getTagName(), attributes, new HashMap<>(), "", input_elem.getLocation().getX(), input_elem.getLocation().getY(), input_elem.getSize().getWidth(), input_elem.getSize().getHeight(), input_elem.getAttribute("innerHTML"), "", input_elem.isDisplayed());
			ElementState tag_record = element_service.findByKey(user_id, input_tag.getKey());
			if(tag_record != null && tag_record.getScreenshotUrl() == null) {
				BufferedImage img = browser.getElementScreenshot(input_elem);
				String checksum = PageState.getFileChecksum(img);
				
				//input_tag = element_service.saveFormElement(user_id, input_tag);
				String screenshot = UploadObjectSingleOperation.saveImageToS3(img, (new URL(browser.getDriver().getCurrentUrl())).getHost(), checksum, BrowserType.create(browser.getBrowserName()), user_id);
				input_tag.setScreenshotUrl(screenshot);
				img.flush();
			}
			
			if(input_elem.getLocation().getX() < 0 || input_elem.getLocation().getY() < 0){
				log.warn("element location x or y are negative");
				continue;
			}
			
			input_tag.getRules().addAll(extractor.extractInputRules(input_tag));
			log.warn("rules applied to input tag   ::   "+input_tag.getRules().size());

			elements.add(element_service.saveFormElement(user_id, input_tag));
		}
		
		return elements;
	}

	/**
	 * locates and returns the form submit button
	 * 
	 * @param form_elem
	 * @return
	 * @throws Exception
	 * 
	 * @pre user_id != null
	 * @pre !user_id.isEmpty()
	 * @pre form_elem != null
	 * @pre browser != null;
	 */
	private ElementState findFormSubmitButton(String user_id, WebElement form_elem, Browser browser) throws Exception {
		assert user_id != null;
		assert !user_id.isEmpty();
		assert form_elem != null;
		assert browser != null;
		
		WebElement submit_element = null;

		boolean submit_elem_found = false;
		List<WebElement> form_elements = getNestedElements(form_elem);
		Set<Attribute> attributes = null;

		for(WebElement elem : form_elements){
			attributes = browser.extractAttributes(elem);
			for(Attribute attribute : attributes){
				if(attribute.contains("submit")){
					submit_elem_found = true;
					break;
				}
			}

			if(submit_elem_found){
				submit_element = elem;
				break;
			}
		}

		if(submit_element == null){
			return null;
		}
		BufferedImage img = browser.getElementScreenshot(form_elem);
		String checksum = PageState.getFileChecksum(img);
		
		//Map<String, String> css_map = Browser.loadCssProperties(submit_element);
		ElementState elem = new ElementState(submit_element.getText(), generateXpath(submit_element, browser.getDriver(), attributes), submit_element.getTagName(), attributes, new HashMap<>(), "", submit_element.getLocation().getX(), submit_element.getLocation().getY(), submit_element.getSize().getWidth(), submit_element.getSize().getHeight(), submit_element.getAttribute("innerHTML"), checksum, submit_element.isDisplayed());
		String screenshot_url = UploadObjectSingleOperation.saveImageToS3(img, (new URL(browser.getDriver().getCurrentUrl())).getHost(), checksum, BrowserType.create(browser.getBrowserName()), user_id);
		elem.setScreenshotUrl(screenshot_url);
		elem = element_service.saveFormElement(user_id, elem);

		return elem;
	}

	public Map<String, Template> findTemplates(List<ElementState> element_list){
		//create a map for the various duplicate elements
		log.warn("parent only list size :: " + element_list.size());

		Map<String, Template> element_templates = new HashMap<>();
		List<ElementState> parents_only_element_list = new ArrayList<>();
		for(ElementState element : element_list) {
			if(!element.isLeaf()) {
				parents_only_element_list.add(element);
			}
		}

		log.warn("parent only list size :: " + parents_only_element_list.size());		
		//iterate over all elements in list
		
		Map<String, Boolean> identified_templates = new HashMap<String, Boolean>();
		for(int idx1 = 0; idx1 < parents_only_element_list.size()-1; idx1++){
			ElementState element1 = parents_only_element_list.get(idx1);
			log.warn("****************************************************************");
			boolean at_least_one_match = false;
			if(identified_templates.containsKey(element1.getKey()) ) {
				continue;
			}
			//for each element iterate over all elements in list
			for(int idx2 = idx1+1; idx2 < parents_only_element_list.size(); idx2++){
				ElementState element2 = parents_only_element_list.get(idx2);
				if(identified_templates.containsKey(element2.getKey()) || !element1.getName().equals(element2.getName())){
					continue;
				}
				//get largest string length
				int max_length = element1.getTemplate().length();
				if(element2.getTemplate().length() > max_length){
					max_length = element2.getTemplate().length();
				}
				
				if(max_length == 0) {
					log.warn("max length of 0 between both templates");
					continue;
				}
				
				if(element1.getTemplate().equals(element2.getTemplate())){
					log.warn("templates match !!");
					String template_str = element2.getTemplate();
					if(!element_templates.containsKey(template_str)){
						element_templates.put(template_str, new Template(TemplateType.UNKNOWN, template_str));
					}
					element_templates.get(template_str).getElements().add(element2);
					identified_templates.put(element2.getKey(), Boolean.TRUE);
					at_least_one_match = true;
					continue;
				}

				log.warn("getting levenshtein distance...");
				//double distance = StringUtils.getJaroWinklerDistance(element_list.get(idx1).getTemplate(), element_list.get(idx2).getTemplate());
				//calculate distance between loop1 value and loop2 value
				double distance = StringUtils.getLevenshteinDistance(element1.getTemplate(), element2.getTemplate());
				//if value is within threshold then add loop2 value to map for loop1 value xpath
				double avg_string_size = ((element1.getTemplate().length() + element2.getTemplate().length())/2.0);
				double similarity = distance / avg_string_size;
				//double sigmoid = new Sigmoid(0,1).value(similarity);

				//calculate distance of children if within 20%
				if(distance == 0.0 || similarity < 0.025){
					log.warn("Distance ;  Similarity :: "+distance + "  ;  "+similarity);
					String template_str = element1.getTemplate();
					if(!element_templates.containsKey(template_str)){
						element_templates.put(template_str, new Template(TemplateType.UNKNOWN, template_str));
					}
					element_templates.get(template_str).getElements().add(element2);
					identified_templates.put(element2.getKey(), Boolean.TRUE);

					at_least_one_match = true;
				}
			}
			if(at_least_one_match){
				String template_str = element1.getTemplate();
				element_templates.get(template_str).getElements().add(element1);
				identified_templates.put(element1.getKey(), Boolean.TRUE);
			}
			log.warn("****************************************************************");

		}

		return element_templates;
	}

	/**
	 * Checks if Attributes contains keywords indicative of a slider 
	 * @param attributes
	 * 
	 * @return true if any of keywords present, otherwise false
	 * 
	 * @pre attributes != null
	 * @pre !attributes.isEmpty()
	 */
	public static boolean doesAttributesContainSliderKeywords(Set<Attribute> attributes) {
		assert attributes != null;
		assert !attributes.isEmpty();
		for(Attribute attr : attributes) {
			List<String> attr_vals = attr.getVals();
			for(String val : attr_vals) {
				if(val.contains("slide")) {
					return true;
				}
			}
		}
		return false;
	}

	@Deprecated
	public Map<String, List<ElementState>> findRepeatedElements(List<ElementState> element_list){
		//create a map for the various duplicate elements
		Map<Integer, ElementState> reviewed_element_map = new HashMap<>();
		Map<String, List<ElementState>> element_templates = new HashMap<>();

		//iterate over all elements in list
		for(int idx1 = 0; idx1 < element_list.size()-1; idx1++){
			if(reviewed_element_map.containsKey(idx1)){
				continue;
			}
			boolean at_least_one_match = false;
			//for each element iterate over all elements in list
			for(int idx2 = idx1+1; idx2 < element_list.size(); idx2++){
				if(reviewed_element_map.containsKey(idx2)){
					continue;
				}
				if(element_list.get(idx1).getTemplate().equals(element_list.get(idx2).getTemplate())){
					String template = element_list.get(idx1).getTemplate();
					if(!element_templates.containsKey(template)){
						element_templates.put(template, new ArrayList<>());
					}
					element_templates.get(template).add(element_list.get(idx2));
				}

				//double distance = StringUtils.getJaroWinklerDistance(element_list.get(idx1).getTemplate(), element_list.get(idx2).getTemplate());
				//calculate distance between loop1 value and loop2 value
				double distance = StringUtils.getLevenshteinDistance(element_list.get(idx1).getTemplate(), element_list.get(idx2).getTemplate());
				//if value is within threshold then add loop2 value to map for loop1 value xpath
				double avg_string_size = ((element_list.get(idx1).getTemplate().length() + element_list.get(idx2).getTemplate().length())/2.0);
				double similarity = distance / avg_string_size;
				//double sigmoid = new Sigmoid(0,1).value(similarity);

				//calculate distance of children if within 20%
				if(distance == 0.0 || similarity < 0.05){
					String template = element_list.get(idx1).getTemplate();
					if(!element_templates.containsKey(template)){
						element_templates.put(template, new ArrayList<>());
					}
					element_templates.get(template).add(element_list.get(idx2));
					reviewed_element_map.put(idx2, element_list.get(idx2));
					at_least_one_match = true;
				}
			}
			if(at_least_one_match){
				String template = element_list.get(idx1).getTemplate();
				element_templates.get(template).add(element_list.get(idx1));			}
		}

		return element_templates;
	}

	/**
	 * Extracts template for element by usin outter tml and removing inner text
	 * @param element {@link Element}
	 *
	 * @return templated version of element html
	 */
	public static String extractTemplate(Element element){
		String template = element.outerHtml();
		String inner_text = element.text();
		String[] text_atoms = inner_text.split(" ");

		template = template.replaceAll("<", " <");
		template = template.replaceAll(">", "> ");
		for(String word : text_atoms){
			word = word.replaceAll("[()]", "");
			word = word.replace("\"", " ");
			word = word.replace("[", "");
			word = word.replaceAll("]", "");
			template = template.replaceAll("\\d"+word+"\\s", "  ");
			template = template.replaceAll(">"+word+"<", "> <");
		}

		//remove all id attributes
		template = template.replaceAll("\\bid=\".*\"", "");
		template = template.replaceAll("\\bhref=\".*\"", "");
		template = template.replaceAll("\\bsrc=\".*\"", "");
		template = template.replaceAll("\\s", "");

		return template;
	}
	
	/**
	 * Extracts template for element by usin outter tml and removing inner text
	 * @param element {@link Element}
	 *
	 * @return templated version of element html
	 */
	public static String extractTemplate(String outerHtml, String innerText){
		String template = outerHtml;
		String[] text_atoms = innerText.split(" ");

		template = template.replaceAll("<", " <");
		template = template.replaceAll(">", "> ");
		for(String word : text_atoms){
			word = word.replaceAll("\\{.*?\\}", "");

			word = word.replaceAll("[()]", "");
			word = word.replace("\"", " ");
			word = word.replace("[", "");
			word = word.replaceAll("]", "");
			template = template.replaceAll("\\d"+word+"\\s", "  ");
			template = template.replaceAll(">"+word+"<", "> <");
		}

		//remove all id attributes
		template = template.replaceAll("\\bid=\".*\"", "");
		template = template.replaceAll("\\bhref=\".*\"", "");
		template = template.replaceAll("\\bsrc=\".*\"", "");
		template = template.replaceAll("\\s", "");

		return template;
	}

	public Map<String, Template> reduceTemplatesToParents(Map<String, Template> list_elements_list) {
		Map<String, Template> element_map = new HashMap<>();
		List<Template> template_list = new ArrayList<>(list_elements_list.values());
		//check if element is a child of another element in the list. if yes then don't add it to the list
		for(int idx1=0; idx1 < template_list.size(); idx1++){
			boolean is_child = false;
			for(int idx2=0; idx2 < template_list.size(); idx2++){
				if(idx1 != idx2 && template_list.get(idx2).getTemplate().contains(template_list.get(idx1).getTemplate())){
					is_child = true;
					break;
				}
			}

			if(!is_child){
				element_map.put(template_list.get(idx1).getTemplate(), template_list.get(idx1));
			}
		}

		//remove duplicates
		log.warn("total elements left after reduction :: " + element_map.values().size());
		return element_map;
	}

	public Map<String, Template> reduceTemplateElementsToUnique(Map<String, Template> template_elements) {

		Map<String, ElementState> element_map = new HashMap<>();
		for(Template template : template_elements.values()){
			for(ElementState element: template.getElements()){
				element_map.put(element.getOuterHtml(), element);
			}
			template.setElements(new ArrayList<>(element_map.values()));
		}
		return template_elements;
	}

	/**
	 *
	 * Atom - A leaf element or an element that contains only 1 leaf element regardless of depth
	 * Molecule - Contains at least 2 atoms and cannot contain any molecules
	 * Organism - Contains at least 2 molecules or at least 1 molecule and 1 atom or at least 1 organism, Must not be an immediate child of body
	 * Template - An Immediate child of the body tag or the descendant such that the element is the first to have sibling elements
	 *
	 * @param template
	 * @return
	 */
	public TemplateType classifyTemplate(String template){
		Document html_doc = Jsoup.parseBodyFragment(template);
		Element root_element = html_doc.body();

		return classifyUsingChildren(root_element);
	}

	private TemplateType classifyUsingChildren(Element root_element) {
		assert root_element != null;

		int atom_cnt = 0;
		int molecule_cnt = 0;
		int organism_cnt = 0;
		int template_cnt = 0;
		if(root_element.children() == null || root_element.children().isEmpty()){
			return TemplateType.ATOM;
		}

		//categorize each eleemnt
		for(Element element : root_element.children()){
			TemplateType type = classifyUsingChildren(element);
			if(type == TemplateType.ATOM){
				atom_cnt++;
			}
			else if(type == TemplateType.MOLECULE){
				molecule_cnt++;
			}
			else if(type == TemplateType.ORGANISM){
				organism_cnt++;
			}
			else if(type == TemplateType.TEMPLATE){
				template_cnt++;
			}
		}

		if(atom_cnt == 1){
			return TemplateType.ATOM;
		}
		else if(atom_cnt > 1 && molecule_cnt == 0 && organism_cnt == 0 && template_cnt == 0){
			return TemplateType.MOLECULE;
		}
		else if((molecule_cnt == 1 && atom_cnt > 0 || molecule_cnt > 1 || organism_cnt > 0) && template_cnt == 0){
			return TemplateType.ORGANISM;
		}
		else if(isTopLevelElement()){
			return TemplateType.TEMPLATE;
		}
		return TemplateType.UNKNOWN;

	}

	private boolean isTopLevelElement() {
		// TODO Auto-generated method stub
		return false;
	}
	
	public static boolean testContainsElement(List<String> keys) {
		for(String key : keys) {
			if(key.contains("elementstate")) {
				return true;
			}
		}
		
		return false;
	}
}
