package com.qanairy.services;

import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.openqa.selenium.NoSuchElementException;
import java.util.Set;

import javax.imageio.ImageIO;

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
import com.minion.browsing.BrowserConnectionFactory;
import com.minion.browsing.Crawler;
import com.minion.browsing.form.ElementRuleExtractor;
import com.minion.util.ArrayUtility;
import com.qanairy.models.Attribute;
import com.qanairy.models.Form;
import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Screenshot;
import com.qanairy.models.Template;
import com.qanairy.models.Test;
import com.qanairy.models.enums.BrowserEnvironment;
import com.qanairy.models.enums.BrowserType;
import com.qanairy.models.enums.FormStatus;
import com.qanairy.models.enums.FormType;
import com.qanairy.models.enums.TemplateType;
import com.qanairy.utils.BrowserUtils;

import us.codecraft.xsoup.Xsoup;

/**
 *
 *
 */
@Component
public class BrowserService {
	private static Logger log = LoggerFactory.getLogger(BrowserService.class);

	@Autowired
	private PageStateService page_state_service;

	@Autowired
	private ElementStateService element_state_service;

	@Autowired
	private ElementRuleExtractor extractor;

	@Autowired
	private BrowserService browser_service;

	@Autowired
	private FormService form_service;

	@Autowired
	private DomainService domain_service;

	@Autowired
	private Crawler crawler;

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
		return BrowserConnectionFactory.getConnection(BrowserType.create(browser_name), browser_env);
	}

	/**
	 *
	 * @param browser_name
	 * @param page_state
	 * @return
	 * @throws IOException
	 * @throws GridException
	 */
	public static boolean checkIfLandable(String browser, PageState page_state, Test test){
		//find last page in path
		PageState last_page = test.findLastPage();
		return !last_page.getUrl().equals(page_state.getUrl());
	}

	public List<PageState> buildPageStates(String url, BrowserType browser_type, String host, List<PathObject> path_objects, List<String> path_keys)
			throws MalformedURLException, IOException, Exception{
		List<PageState> page_states = new ArrayList<>();
		Browser browser = null;
		Map<String, Template> template_elements = new HashMap<>();

		// BUILD ALL PAGE STATES
		boolean err = true;
		do{
			err=false;
			try{
				log.warn("getting browser connection");
				browser = BrowserConnectionFactory.getConnection(browser_type, BrowserEnvironment.DISCOVERY);
				browser.navigateTo(url);
				crawler.crawlPathWithoutBuildingResult(path_keys, path_objects, browser, host);
				BrowserUtils.getLoadingAnimation(browser, host);
				
				String source = browser.getDriver().getPageSource();
				List<ElementState> all_elements_list = BrowserService.getAllElementsUsingJSoup(source);
				template_elements = browser_service.findTemplates(all_elements_list);
				template_elements = browser_service.reduceTemplatesToParents(template_elements);
				template_elements = browser_service.reduceTemplateElementsToUnique(template_elements);

				PageState page_state = buildPage(browser);

				page_state.setTemplates(new ArrayList<>(template_elements.values()));

				page_states.add(page_state);
			}
			catch(NullPointerException e){
				err=true;
				e.printStackTrace();
			}
			catch(WebDriverException e){
				err=true;
				log.debug("WebDriverException occurred while buildin page states");
			}
			catch(Exception e){
				log.warn("Exception occurred while building page states :: " + e.getMessage());
				err=true;
			}
			finally {
				if(browser != null) {
					browser.close();
				}
			}
		}while(err);

		return page_states;
	}

	private boolean isElementLargerThanViewport(Browser browser, Dimension element_size) {
		int height = element_size.getHeight();
		int width = element_size.getWidth();

		return width >= browser.getViewportSize().getWidth()
				 || height >= browser.getViewportSize().getHeight();
	}

	public static List<String> getXpathsUsingJSoup(String pageSource) {
		Map<String, Integer> xpath_cnt_map = new HashMap<>();
		List<String> elements = new ArrayList<>();
		Document html_doc = Jsoup.parse(pageSource);
		List<Element> web_elements = Xsoup.compile("//body//*").evaluate(html_doc).getElements();
		for(Element element: web_elements){
			int child_node_cnt = element.children().size();
			String xpath = generateXpathUsingJsoup(element, html_doc, element.attributes(), xpath_cnt_map);
			if(child_node_cnt == 0 && !isStructureTag(element.tagName()) && !doesElementBelongToScriptTag(element) && !"iframe".equals(element.tagName())){
				elements.add(xpath);
			}
		}

		return elements;
	}

	public static List<ElementState> getChildElementsUsingJSoup(String pageSource) {
		Map<String, Integer> xpath_cnt_map = new HashMap<>();
		List<ElementState> elements = new ArrayList<>();
		Document html_doc = Jsoup.parse(pageSource);
		List<Element> web_elements = Xsoup.compile("//body//*").evaluate(html_doc).getElements();
		for(Element element: web_elements){
			int child_node_cnt = element.children().size();

			if(child_node_cnt == 0 && !isStructureTag(element.tagName()) && !doesElementBelongToScriptTag(element) && !"iframe".equals(element.tagName())){
				String xpath = generateXpathUsingJsoup(element, html_doc, element.attributes(), xpath_cnt_map);
				Set<Attribute> attributes = generateAttributesUsingJsoup(element);
				ElementState element_state = buildElementState(xpath, attributes, element);
				elements.add(element_state);
			}
		}

		return elements;
	}

	public static List<ElementState> getAllElementsUsingJSoup(String pageSource) {
		Map<String, Integer> xpath_cnt_map = new HashMap<>();
		List<ElementState> elements = new ArrayList<>();
		Document html_doc = Jsoup.parse(pageSource);
		List<Element> web_elements = Xsoup.compile("//body//*").evaluate(html_doc).getElements();
		for(Element element: web_elements){
			if(!isStructureTag(element.tagName()) && !doesElementBelongToScriptTag(element) && !"iframe".equals(element.tagName())){
				String xpath = generateXpathUsingJsoup(element, html_doc, element.attributes(), xpath_cnt_map);
				Set<Attribute> attributes = generateAttributesUsingJsoup(element);
				ElementState element_state = buildElementState(xpath, attributes, element);
				elements.add(element_state);
			}
		}

		return elements;
	}

	public static ElementState buildElementState(String xpath, Set<Attribute> attributes, Element element) {
		ElementState element_state = new ElementState();
		element_state.setXpath(xpath);
		element_state.setAttributes(attributes);
		element_state.setOuterHtml(element.outerHtml());
		element_state.setInnerHtml(element.html());
		element_state.setTemplate(extractTemplate(element));
		element_state.setName(element.tagName());
		element_state.setText(element.text());
		return element_state;
	}

	/**
	 * Checks all parent elements up until and excluding the body tag for any script tags.
	 *
	 * @param element {@Element element
	 *
	 * @return true if a parent element within the dom is a structure tag
	 */
	private static boolean doesElementBelongToScriptTag(Element element) {
		Element new_elem = element;
		while(new_elem != null && !new_elem.tagName().equals("body")){
			if(isStructureTag(new_elem.tagName())){
				return true;
			}
			new_elem = new_elem.parent();
		}
		return false;
	}

	/**
	 *
	 * @return
	 * @throws GridException
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 *
	 * @pre browser != null
	 * @post page_state != null
	 */
	public PageState buildPage(Browser browser) throws GridException, IOException, NoSuchAlgorithmException{
		assert browser != null;
		List<ElementState> element_list = BrowserService.getChildElementsUsingJSoup(browser.getDriver().getPageSource());

		List<ElementState> visible_elements = browser_service.getVisibleElements(browser, element_list);
		log.warn("building page with # of elements :: " + visible_elements.size());

		String browser_url = browser.getDriver().getCurrentUrl();
		String url_without_params = BrowserUtils.sanitizeUrl(browser_url);
		
		BufferedImage viewport_screenshot = browser.getViewportScreenshot();
		String screenshot_checksum = PageState.getFileChecksum(viewport_screenshot);

		BufferedImage full_page_screenshot = browser.getFullPageScreenshot();
		String full_page_screenshot_checksum = PageState.getFileChecksum(full_page_screenshot);
		PageState page_state_record2 = page_state_service.findByScreenshotChecksum(full_page_screenshot_checksum);

		if(page_state_record2 != null){
			viewport_screenshot.flush();
			page_state_record2.setElements(page_state_service.getElementStates(page_state_record2.getKey()));
			page_state_record2.setScreenshots(page_state_service.getScreenshots(page_state_record2.getKey()));
			return page_state_record2;
		}
		else{
			//extract visible elements from list of elementstates provided
			String viewport_screenshot_url = UploadObjectSingleOperation.saveImageToS3(viewport_screenshot, new URL(url_without_params).getHost(), screenshot_checksum, browser.getBrowserName()+"-viewport");
			String full_page_screenshot_url = UploadObjectSingleOperation.saveImageToS3(full_page_screenshot, new URL(url_without_params).getHost(), full_page_screenshot_checksum, browser.getBrowserName()+"-full");

			List<ElementState> elements_with_screenshots = new ArrayList<>();
			for(ElementState element_state : visible_elements){
				ElementState elem = retrieveAndUploadBrowserScreenshot(browser, element_state, full_page_screenshot, new URL(url_without_params).getHost(), browser.getXScrollOffset(), browser.getYScrollOffset());
				elements_with_screenshots.add(elem);
			}

			PageState page_state = new PageState( url_without_params,
					viewport_screenshot_url,
					elements_with_screenshots,
					org.apache.commons.codec.digest.DigestUtils.sha256Hex(Browser.cleanSrc(browser.getDriver().getPageSource())),
					browser.getXScrollOffset(),
					browser.getYScrollOffset(),
					browser.getViewportSize().width,
					browser.getViewportSize().height,
					browser.getBrowserName());
			page_state.addScreenshotChecksum(screenshot_checksum);
			page_state.addScreenshotChecksum(full_page_screenshot_checksum);
			page_state.setFullPageScreenshotUrl(full_page_screenshot_url);
			page_state.setFullPageChecksum(full_page_screenshot_checksum);
			Screenshot screenshot = new Screenshot(full_page_screenshot_url, browser.getBrowserName(), full_page_screenshot_checksum, browser.getViewportSize().getWidth(), browser.getViewportSize().getHeight());
			page_state.addScreenshot(screenshot);

			full_page_screenshot.flush();
			viewport_screenshot.flush();
			return page_state;
		}
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
			if("html".equals(tag_name) || "body".equals(tag_name)
					|| "link".equals(tag_name) || "script".equals(tag_name)
					|| "title".equals(tag_name) || "meta".equals(tag_name)
					|| "head".equals(tag_name) ){
				continue;
			}
			elements.add(element);
		}
		return elements;
	}

	/**
	 *
	 * @return
	 * @throws GridException
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 * @throws URISyntaxException
	 *
	 * @pre browser != null
	 * @post page_state != null
	 */
	/*
	@Deprecated
	public PageState buildPage(Browser browser) throws GridException, IOException, NoSuchAlgorithmException{
		assert browser != null;

		String browser_url = browser.getDriver().getCurrentUrl();
		URL page_url = new URL(browser_url);
		String url_without_params = BrowserUtils.sanitizeUrl(browser_url);

		BufferedImage viewport_screenshot = browser.getViewportScreenshot();
		String screenshot_checksum = PageState.getFileChecksum(viewport_screenshot);
		
		BufferedImage full_page_screenshot = browser.getFullPageScreenshot();
		String full_page_screenshot_checksum = PageState.getFileChecksum(full_page_screenshot);
		PageState page_state_record2 = page_state_service.findByScreenshotChecksum(full_page_screenshot_checksum);

		//PageState page_state_record2 = page_state_service.findByScreenshotChecksum(screenshot_checksum);
		if(page_state_record2 == null){
			page_state_record2 = page_state_service.findByAnimationImageChecksum(screenshot_checksum);
		}

		if(page_state_record2 != null){
			viewport_screenshot.flush();
			page_state_record2.setElements(page_state_service.getElementStates(page_state_record2.getKey()));
			page_state_record2.setScreenshots(page_state_service.getScreenshots(page_state_record2.getKey()));
			return page_state_record2;
		}
		else{
			//Animation animation = BrowserUtils.getAnimation(browser, page_url.getHost());
			List<ElementState> visible_elements = getVisibleElements(browser, "", page_url.toString(), viewport_screenshot);
			String viewport_screenshot_url = UploadObjectSingleOperation.saveImageToS3(viewport_screenshot, page_url.getHost(), screenshot_checksum, browser.getBrowserName()+"-viewport");

			PageState page_state = new PageState( url_without_params,
					viewport_screenshot_url,
					visible_elements,
					org.apache.commons.codec.digest.DigestUtils.sha256Hex(Browser.cleanSrc(browser.getDriver().getPageSource())),
					browser.getXScrollOffset(),
					browser.getYScrollOffset(),
					browser.getViewportSize().width,
					browser.getViewportSize().height,
					browser.getBrowserName());

			Screenshot screenshot = new Screenshot(viewport_screenshot_url, browser.getBrowserName(), screenshot_checksum, browser.getViewportSize().getWidth(), browser.getViewportSize().getHeight());
			page_state.addScreenshot(screenshot);

			PageState page_state_record = page_state_service.findByKey(page_state.getKey());
			if(page_state_record != null){
				page_state = page_state_record;
				page_state.addScreenshotChecksum(screenshot_checksum);
				//page_state = page_state_service.save(page_state);
			}

			viewport_screenshot.flush();
			return page_state;
		}
	}
*/

	/**
	 * Retreives all elements on a given page that are visible. In this instance we take
	 *  visible to mean that it is not currently set to {@css display: none} and that it
	 *  is visible within the confines of the screen. If an element is not hidden but is also
	 *  outside of the bounds of the screen it is assumed hidden
	 *
	 * @param driver
	 * @return list of webelements that are currently visible on the page
	 * @throws IOException
	 * @throws GridException
	 *
	 * @pre visible_element_map != null
	 */
	public List<ElementState> getVisibleElements(Browser browser, List<ElementState> elements)
															 throws WebDriverException, GridException, IOException{
		assert elements != null;
		long start_time = System.currentTimeMillis();
		List<ElementState> visible_elements = new ArrayList<>();
		boolean err = false;

		do{
			err = false;
			try{
				for(ElementState element_state : elements){
					try{
						WebElement element = browser.findWebElementByXpath(element_state.getXpath());
						Point location = element.getLocation();
						Dimension element_size = element.getSize();
						if(element.isDisplayed() && hasWidthAndHeight(element_size) && !isElementLargerThanViewport(browser, element_size)){
							Map<String, String> css_props = Browser.loadCssProperties(element);

							ElementState new_element_state = buildElementState(browser, element, element_state.getXpath(), element_state.getAttributes(), css_props, location, element_size, element_state);
							visible_elements.add(new_element_state);
						}
					}catch(NoSuchElementException e){
						log.warn("Unable to find element :: "+e.getMessage());
					}
				}
			}catch(WebDriverException e){
				log.warn("Exception occurred while getting visible elements ::   " + e.getMessage());
				err = true;
				if(!e.getMessage().contains("no_such_element")){
					throw e;
				}
			}
		}while(err);

		log.warn("total time get all visible elements :: " + (System.currentTimeMillis() - start_time));
		return visible_elements;
	}

	/**
	 * Retreives all elements on a given page that are visible. In this instance we take
	 *  visible to mean that it is not currently set to {@css display: none} and that it
	 *  is visible within the confines of the screen. If an element is not hidden but is also
	 *  outside of the bounds of the screen it is assumed hidden
	 *
	 * @param driver
	 * @return list of webelements that are currently visible on the page
	 * @throws IOException
	 * @throws GridException
	 */
	public List<ElementState> getVisibleElementsWithinViewport(Browser browser, BufferedImage page_screenshot, Map<Integer, ElementState> visible_element_map, List<ElementState> elements, boolean element_pre_build)
															 throws WebDriverException, GridException, IOException{
		List<ElementState> visible_elements = new ArrayList<>();

		boolean err = false;
		do{
			err = false;
			try{
				int start_idx = 0;
				int visible_map_size = visible_element_map.size();

				if(visible_map_size > 0){
					start_idx = visible_element_map.keySet().size()-1;
				}

				List<ElementState> element_sublist = elements.subList(start_idx, elements.size());
				for(ElementState element_state : element_sublist){
					if(element_state == null) {
						continue;
					}
					WebElement element = browser.findWebElementByXpath(element_state.getXpath());
					Dimension element_size = element.getSize();
					if(element.isDisplayed() && hasWidthAndHeight(element_size) && isElementVisibleInPane(browser, element.getLocation(), element_size)){
						Map<String, String> css_props = Browser.loadCssProperties(element);

						ElementState new_element_state = buildElementState(browser, element, element_state.getXpath(), element_state.getAttributes(), css_props, element.getLocation(), element.getSize(), element_state);
						if(new_element_state != null){
							visible_elements.add(new_element_state);
							visible_element_map.put(visible_map_size, new_element_state);
							visible_map_size++;
						}
					}
				}
			}
			catch(NoSuchElementException e){
				log.warn("No such element exception");
			}
			catch(WebDriverException e){
				e.printStackTrace();
				if(!e.getMessage().contains("no_such_element")){
					throw e;
				}
				else{
					err = true;
				}
			}
		}while(err);

		return visible_elements;
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
	@Deprecated
	public ElementState buildElementState(Browser browser, WebElement elem, BufferedImage page_screenshot) throws IOException{
		Map<String, Integer> xpath_map = new HashMap<String, Integer>();

		String element_tag_name = elem.getTagName();
		Point location = elem.getLocation();
		Dimension element_size = elem.getSize();

		boolean is_visible_in_pane = isElementVisibleInPane(browser, elem.getLocation(), elem.getSize());
		boolean is_structure_tag = isStructureTag(element_tag_name);
		boolean has_width_and_height = hasWidthAndHeight(element_size);

		if(!elem.isDisplayed() || !is_visible_in_pane || is_structure_tag || !has_width_and_height){
			return null;
		}

		String screenshot = null;
		ElementState page_element_record = null;
		ElementState page_element = null;
		BufferedImage img = browser.getElementScreenshot(elem);
		String checksum = PageState.getFileChecksum(img);
		page_element_record = element_state_service.findByScreenshotChecksum(checksum);

		if(page_element_record != null){
			page_element = page_element_record;
		}
		else{
			Map<String, String> css_props = Browser.loadCssProperties(elem);
			Set<Attribute> attributes = browser.extractAttributes(elem);

			page_element = new ElementState(elem.getText(), null, element_tag_name, attributes, css_props, null, checksum, location.getX(), location.getY(), element_size.getWidth(), element_size.getHeight(), elem.getAttribute("innerHTML") );

			boolean err = false;
			int count = 0;
			do{
				try{
					screenshot = UploadObjectSingleOperation.saveImageToS3(img, (new URL(browser.getDriver().getCurrentUrl())).getHost(), checksum, browser.getBrowserName()+"-element");
				}catch(IOException e){}
				count++;
			}while(err && count < 100);

			//TODO: refactor xpath to generation to be faster. Generating xpath can take over 1.6s
			String element_xpath = generateXpath(elem, "", xpath_map, browser.getDriver(), attributes);

			page_element.setScreenshot(screenshot);
			page_element.setScreenshotChecksum(checksum);
			page_element.setXpath(element_xpath);
			//page_element = element_state_service.save(page_element);
		}
		img.flush();

		return page_element;
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
	public ElementState buildElementState(Browser browser, WebElement elem, String xpath, Set<Attribute> attributes, Map<String, String> css_props, Point location, Dimension element_size, ElementState element_state) throws IOException{
		long start_time = System.currentTimeMillis();

		String checksum = "";
		ElementState page_element = new ElementState(element_state.getText(), xpath, element_state.getName(), attributes, css_props, null, checksum,
										location.getX(), location.getY(), element_size.getWidth(), element_size.getHeight(), element_state.getInnerHtml());

		//element_state_service.save(page_element);
		log.debug("total time to save element state :: " + (System.currentTimeMillis() - start_time) + "    :  xpath time ::    "+xpath);

		return page_element;
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
	public ElementState buildElementState(Browser browser, WebElement elem, String xpath, Set<Attribute> attributes, Map<String, String> css_props, Point location, Dimension element_size) throws IOException{
		long start_time = System.currentTimeMillis();

		String checksum = "";
		ElementState page_element = new ElementState(elem.getText(), xpath, elem.getTagName(), attributes, css_props, null, checksum,
										location.getX(), location.getY(), element_size.getWidth(), element_size.getHeight(), elem.getAttribute("innerHTML") );

		//element_state_service.save(page_element);
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

		return "html".equals(tag_name) || "body".equals(tag_name)
				|| "link".equals(tag_name) || "script".equals(tag_name)
				|| "title".equals(tag_name) || "meta".equals(tag_name)
				|| "head".equals(tag_name) || "noscript".equals(tag_name)
				|| "g".equals(tag_name) || "path".equals(tag_name) || "svg".equals(tag_name) || "polygon".equals(tag_name)
				|| "br".equals(tag_name) || "style".equals(tag_name) || "polyline".equals(tag_name) || "use".equals(tag_name)
				|| "template".equals(tag_name) || "audio".equals(tag_name);
	}

	/**
	 * Filters out html, body, link, title, script, meta, head, iframe, or noscript tags
	 *
	 * @param tag_name
	 *
	 * @pre tag_name != null
	 *
	 * @return true if tag name is noscript, g, path, svg, polygon
	 */
	public static boolean isInternalStructTag(String tag_name) {
		assert tag_name != null;

		return "noscript".equals(tag_name)
				|| "g".equals(tag_name) || "path".equals(tag_name) || "svg".equals(tag_name) || "polygon".equals(tag_name);
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
	public String generateXpath(WebElement element, String xpath, Map<String, Integer> xpathHash, WebDriver driver, Set<Attribute> attributes){
		List<String> attributeChecks = new ArrayList<>();
		List<String> valid_attributes = Arrays.asList(valid_xpath_attributes);
		xpath += "//"+element.getTagName();
		for(Attribute attr : attributes){
			if(valid_attributes.contains(attr.getName())){
				String attribute_values = ArrayUtility.joinArray(attr.getVals().toArray(new String[attr.getVals().size()]));
				String trimmed_values = cleanAttributeValues(attribute_values.trim());

				if(trimmed_values.length() > 0 && !trimmed_values.contains("javascript") && !trimmed_values.contains("void()")){
					attributeChecks.add("contains(@" + attr.getName() + ",\"" + trimmed_values.split(" ")[0] + "\")");
				}
			}
		}
		if(attributeChecks.size()>0){
			xpath += "[";
			for(int i = 0; i < attributeChecks.size(); i++){
				xpath += attributeChecks.get(i).toString();
				if(i < attributeChecks.size()-1){
					xpath += " and ";
				}
			}
			xpath += "]";
		}

	    WebElement parent = element;
	    int count = 0;
	    while(!"html".equals(parent.getTagName()) && !"body".equals(parent.getTagName()) && parent != null && count < 3){
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
		return uniqifyXpath(element, xpathHash, xpath, driver);
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
		String xpath = "//"+element.tagName();
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
			for(int i = 0; i < attributeChecks.size(); i++){
				xpath += attributeChecks.get(i).toString();
				if(i < attributeChecks.size()-1){
					xpath += " and ";
				}
			}
			xpath += "]";
		}

		Element last_element = element;
		Element parent = null;
	    int count = 0;
	    while(!"html".equals(last_element.tagName()) && !"body".equals(last_element.tagName()) && count < 3){
	    	try{
	    		parent = last_element.parent();

	    		if(!isStructureTag(parent.tagName()) && !"iframe".equals(element.tagName())){
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
			List<Element> elements = Xsoup.compile(xpath).evaluate(doc).getElements(); //driver.findElements(By.xpath(xpath));
			if(elements.size() > 1){
				int count = 0;
				if(xpath_cnt.containsKey(xpath)){
					count = xpath_cnt.get(xpath);
				}
				xpath_cnt.put(xpath, ++count);

				return "("+xpath+")[" + count + "]";
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
	public static String uniqifyXpath(WebElement elem, Map<String, Integer> xpathHash, String xpath, WebDriver driver){
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
	public List<Form> extractAllForms(PageState page, Browser browser) throws Exception{
		Map<String, Integer> xpath_map = new HashMap<String, Integer>();
		List<Form> form_list = new ArrayList<Form>();
		log.warn("extracting forms from page with url    ::     "+browser.getDriver().getCurrentUrl());
		List<WebElement> form_elements = browser.getDriver().findElements(By.xpath("//form"));
		log.warn("form elements found using xpath //form    :: "+form_elements.size());
		String host = new URL(page.getUrl()).getHost();
		for(WebElement form_elem : form_elements){
			log.warn("scrolling to form element");
			browser.scrollToElement(form_elem);
			BrowserUtils.detectShortAnimation(browser, page.getUrl());

			String screenshot_url = retrieveAndUploadBrowserScreenshot(browser, form_elem, host);
			ElementState form_tag = new ElementState(form_elem.getText(), uniqifyXpath(form_elem, xpath_map, "//form", browser.getDriver()), "form", browser.extractAttributes(form_elem), Browser.loadCssProperties(form_elem), screenshot_url, form_elem.getLocation().getX(), form_elem.getLocation().getY(), form_elem.getSize().getWidth(), form_elem.getSize().getHeight(), form_elem.getAttribute("innerHTML"), PageState.getFileChecksum(ImageIO.read(new URL(screenshot_url))) );

			double[] weights = new double[1];
			weights[0] = 0.3;

			Set<Form> forms = domain_service.getForms(host);
			Form form = new Form(form_tag, new ArrayList<ElementState>(), findFormSubmitButton(form_elem, browser),
									"Form #"+(forms.size()+1), weights, FormType.UNKNOWN, new Date(), FormStatus.DISCOVERED );

			List<WebElement> input_elements =  form_elem.findElements(By.xpath(form_tag.getXpath() +"//input"));

			input_elements = BrowserService.fitlerNonDisplayedElements(input_elements);
			input_elements = BrowserService.filterStructureTags(input_elements);
			input_elements = BrowserService.filterNotVisibleInViewport(browser.getXScrollOffset(), browser.getYScrollOffset(), input_elements, browser.getViewportSize());
			input_elements = BrowserService.filterNoWidthOrHeight(input_elements);
			input_elements = BrowserService.filterElementsWithNegativePositions(input_elements);

			for(WebElement input_elem : input_elements){
				Set<Attribute> attributes = browser.extractAttributes(input_elem);
				String form_element_url = retrieveAndUploadBrowserScreenshot(browser, form_elem, host);
				ElementState input_tag = new ElementState(input_elem.getText(), generateXpath(input_elem, "", xpath_map, browser.getDriver(), attributes), input_elem.getTagName(), attributes, Browser.loadCssProperties(input_elem), form_element_url, input_elem.getLocation().getX(), input_elem.getLocation().getY(), input_elem.getSize().getWidth(), input_elem.getSize().getHeight(), input_elem.getAttribute("innerHTML"), PageState.getFileChecksum(ImageIO.read(new URL(screenshot_url))) );

				if(input_tag == null || input_tag.getScreenshot()== null || input_tag.getScreenshot().isEmpty()){
					browser.scrollToElement(input_elem);
					BufferedImage viewport = browser.getViewportScreenshot();

					if(input_elem.getLocation().getX() < 0 || input_elem.getLocation().getY() < 0){
						continue;
					}
					BufferedImage img = browser.getElementScreenshot(input_elem);
					String checksum = PageState.getFileChecksum(img);
					viewport.flush();
					String screenshot= null;
					try {
						screenshot = UploadObjectSingleOperation.saveImageToS3(img, (new URL(browser.getDriver().getCurrentUrl())).getHost(), checksum, input_tag.getKey());
					} catch (Exception e) {
						log.warn("Error retrieving screenshot -- "+e.getLocalizedMessage());
					}
					img.flush();
					input_tag.setScreenshot(screenshot);
					input_tag.setScreenshotChecksum(checksum);
					input_tag.getRules().addAll(extractor.extractInputRules(input_tag));
				}

				List<ElementState> group_inputs = constructGrouping(input_elem, browser);

				//combo_input.getElements().addAll(labels);
				form.addFormFields(group_inputs);
			}

			for(double d: form.getPrediction()){
				log.info("PREDICTION ::: "+d);
			}

			log.info("weights :: "+ form.getPrediction());
			form.setType(FormType.UNKNOWN);

			form.setDateDiscovered(new Date());
			log.info("form record discovered date :: "+form.getDateDiscovered());

			form.setName("Form #1");
			log.info("name :: "+form.getName());

			Form form_record = form_service.findByKey(form.getKey());
			if(form_record != null) {
				continue;
			}
			form_list.add(form);
		}
		return form_list;
	}


	/**
	 * Finds all other inputs that are grouped with this one by observing each parent of a {@link WebElement} until it
	 *   finds a parent which has inputs with a different type than the provided {@link WebElement}
	 *
	 * @param page_elem
	 * @param driver
	 * @return
	 * @throws Exception
	 */
	public List<ElementState> constructGrouping(WebElement page_elem, Browser browser) throws Exception{

		List<ElementState> child_inputs = new ArrayList<ElementState>();

		String input_type = page_elem.getAttribute("type");
		WebElement parent = null;
		boolean allChildrenMatch = true;
		do{
			try{
				parent = page_elem.findElement(By.xpath(".."));

				List<WebElement> children = parent.findElements(By.xpath(".//input"));
				if(children.size() >= child_inputs.size() && !parent.getTagName().equals("form")){
					//lists are different, so check out all the element
					for(WebElement child : children){
						if(!child.getAttribute("type").equals(input_type)){
							allChildrenMatch = false;
						}
						/*else{
							form_xpath_list.add(Browser.generateXpath(child, "", new HashMap<String, Integer>(), driver));
						}*/
					}
				}
				else{
					allChildrenMatch = false;
				}
			}catch(InvalidSelectorException e){
				parent = null;
				log.error("Invalid selector exception occurred " + e.getMessage());
				break;
			}

			if(allChildrenMatch){
				//create list with new elements
				List<WebElement> children = parent.findElements(By.xpath(".//input"));
				children = BrowserService.filterNotVisibleInViewport(browser.getXScrollOffset(), browser.getYScrollOffset(), children, browser.getViewportSize());
				children = BrowserService.fitlerNonDisplayedElements(children);
				children = BrowserService.filterNonChildElements(children);
				children = BrowserService.filterNoWidthOrHeight(children);
				children = BrowserService.filterElementsWithNegativePositions(children);

				child_inputs = new ArrayList<ElementState>();

				for(WebElement child : children){
					Set<Attribute> attributes = browser.extractAttributes(child);
					String screenshot_url = retrieveAndUploadBrowserScreenshot(browser, child, (new URL(browser.getDriver().getCurrentUrl())).getHost());

					ElementState elem = new ElementState(child.getText(), generateXpath(child, "", new HashMap<String, Integer>(), browser.getDriver(), attributes), child.getTagName(), attributes, Browser.loadCssProperties(child), screenshot_url, child.getLocation().getX(), child.getLocation().getY(), child.getSize().getWidth(), child.getSize().getHeight(), child.getAttribute("innerHTML"), PageState.getFileChecksum(ImageIO.read(new URL(screenshot_url))));

					//elem = element_state_service.save(elem);

					//FormField input_field = new FormField(elem);

					child_inputs.add(elem);
				}

				page_elem = parent;
			}
			else{
				if(child_inputs.size() == 0){
					Set<Attribute> attributes = browser.extractAttributes(page_elem);
					String screenshot_url = retrieveAndUploadBrowserScreenshot(browser, page_elem, (new URL(browser.getDriver().getCurrentUrl())).getHost());

					ElementState input_tag = new ElementState(page_elem.getText(), generateXpath(page_elem, "", new HashMap<String,Integer>(), browser.getDriver(), attributes), page_elem.getTagName(), attributes, Browser.loadCssProperties(page_elem), screenshot_url, page_elem.getLocation().getX(), page_elem.getLocation().getY(), page_elem.getSize().getWidth(), page_elem.getSize().getHeight(), page_elem.getAttribute("innerHTML"), PageState.getFileChecksum(ImageIO.read(new URL(screenshot_url))) );

					/*
					ElementState elem_record = element_state_service.findByKey(input_tag.getKey());
					if(elem_record != null){
						input_tag=elem_record;
					}
					else{
						input_tag = element_state_service.save(input_tag);
					}
					*/
					child_inputs.add(input_tag);
				}
			}
		}while(allChildrenMatch);

		return child_inputs;
	}

	/**
	 * locates and returns the form submit button
	 * @param form_elem
	 * @return
	 * @throws Exception
	 */
	private ElementState findFormSubmitButton(WebElement form_elem, Browser browser) throws Exception {
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
		
		String screenshot_url = retrieveAndUploadBrowserScreenshot(browser, submit_element, (new URL(browser.getDriver().getCurrentUrl())).getHost());
		ElementState elem = new ElementState(submit_element.getText(), generateXpath(submit_element, "", new HashMap<String, Integer>(), browser.getDriver(), attributes), submit_element.getTagName(), attributes, Browser.loadCssProperties(submit_element), screenshot_url, submit_element.getLocation().getX(), submit_element.getLocation().getY(), submit_element.getSize().getWidth(), submit_element.getSize().getHeight(), submit_element.getAttribute("innerHTML"), PageState.getFileChecksum(ImageIO.read(new URL(screenshot_url))) );


		return elem;
	}

	/**
	 *
	 * @param driver
	 * @param elem
	 * @param page_img
	 * @return
	 * @throws Exception
	 */
	public String retrieveAndUploadBrowserScreenshot(Browser browser, WebElement elem, String host) throws IOException {
		BufferedImage img = null;
		String checksum = "";
		String screenshot_url = "";
		boolean err = false;
		int count = 0;

		do{
			err = false;
			try{
				BufferedImage viewport_screenshot = browser.getViewportScreenshot();
				img = browser.getElementScreenshot(elem);
				checksum = PageState.getFileChecksum(img);
				screenshot_url = UploadObjectSingleOperation.saveImageToS3(img, host, checksum, browser.getBrowserName()+"-element");
			}
			catch(RasterFormatException e){
				err = true;
				log.warn("Raster Format Exception (retrieveAndUploadBrowserScreenshot): "+e.getMessage());
			} catch (GridException e) {
				err = true;
				log.warn("Grid Exception occurred while retrieving and uploading "+e.getMessage());
			} catch (IOException e) {
				err = true;
				log.warn("IOException occurred while retrieving and uploading "+e.getMessage());
			}
			finally{
				if(img != null){
					img.flush();
				}
			}
			count++;
		}while(err && count < 50);

		return screenshot_url;
	}

	/**
	 *
	 * @param driver
	 * @param elem
	 * @param page_img
	 * @return
	 * @throws Exception
	 */
	public ElementState retrieveAndUploadBrowserScreenshot(Browser browser, ElementState elem, BufferedImage page_img, String host, long x_offset, long y_offset) throws IOException{
		BufferedImage img = null;
		String checksum = "";
		String screenshot_url = "";
		boolean err = false;
		int count = 0;

		ElementState elem_copy = elem.clone();
		do{
			err = false;
			try{
				img = Browser.getElementScreenshot(elem_copy, page_img, browser);
				checksum = PageState.getFileChecksum(img);
				screenshot_url = UploadObjectSingleOperation.saveImageToS3(img, host, checksum, browser.getBrowserName()+"-element");
				elem_copy.setScreenshot(screenshot_url);
				elem_copy.setScreenshotChecksum(checksum);
			}
			catch(RasterFormatException e){
				err = true;
				log.warn("Raster Format Exception (retrieveAndUploadBrowserScreenshot): "+e.getMessage());
			} catch (GridException e) {
				err = true;
				log.warn("Grid Exception occurred while retrieving and uploading "+e.getMessage());
			} catch (IOException e) {
				err = true;
				log.warn("IOException occurred while retrieving and uploading "+e.getMessage());
			}
			finally{
				if(img != null){
					img.flush();
				}
			}
			count++;
		}while(err && count < 10);

		return elem_copy;
	}


	public Map<String, Template> findTemplates(List<ElementState> element_list){
		//create a map for the various duplicate elements
		Map<Integer, ElementState> reviewed_element_map = new HashMap<>();
		Map<String, Template> element_templates = new HashMap<>();

		//iterate over all elements in list
		for(int idx1 = 0; idx1 < element_list.size()-1; idx1++){
			if(reviewed_element_map.containsKey(idx1)){
				continue;
			}
			boolean at_least_one_match = false;
			//for each element iterate over all elements in list
			for(int idx2 = 0; idx2 < element_list.size(); idx2++){
				if(idx1 == idx2){
					continue;
				}
				//get largest string length
				int max_length = element_list.get(idx1).getTemplate().length();
				if(element_list.get(idx2).getTemplate().length() > max_length){
					max_length = element_list.get(idx2).getTemplate().length();
				}
				double length_similarity = Math.abs(element_list.get(idx1).getTemplate().length() - element_list.get(idx2).getTemplate().length()) / max_length;
				if(length_similarity > 0.05){
					continue;
				}
				if(element_list.get(idx1).getTemplate().equals(element_list.get(idx2).getTemplate())){
					String template_str = element_list.get(idx2).getTemplate();
					if(!element_templates.containsKey(template_str)){
						element_templates.put(template_str, new Template(TemplateType.UNKNOWN, template_str));
					}
					element_templates.get(template_str).getElements().add(element_list.get(idx2));
					continue;
				}

				//double distance = StringUtils.getJaroWinklerDistance(element_list.get(idx1).getTemplate(), element_list.get(idx2).getTemplate());
				//calculate distance between loop1 value and loop2 value
				double distance = StringUtils.getLevenshteinDistance(element_list.get(idx1).getTemplate(), element_list.get(idx2).getTemplate());
				//if value is within threshold then add loop2 value to map for loop1 value xpath
				double avg_string_size = ((element_list.get(idx1).getTemplate().length() + element_list.get(idx2).getTemplate().length())/2.0);
				double similarity = distance / avg_string_size;
				//double sigmoid = new Sigmoid(0,1).value(similarity);

				//calculate distance of children if within 20%
				if(distance == 0.0 || similarity < 0.025){
					log.debug("Distance ;  Similarity :: "+distance + "  ;  "+similarity);
					String template_str = element_list.get(idx1).getTemplate();
					if(!element_templates.containsKey(template_str)){
						element_templates.put(template_str, new Template(TemplateType.UNKNOWN, template_str));
					}
					element_templates.get(template_str).getElements().add(element_list.get(idx2));
					at_least_one_match = true;
				}
			}
			if(at_least_one_match){
				String template_str = element_list.get(idx1).getTemplate();
				element_templates.get(template_str).getElements().add(element_list.get(idx1));
			}
		}

		return element_templates;
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


	@Deprecated
	public Map<String, Template> reduceRepeatedElementsListToOnlyParents(List<ElementState> list_elements_list) {
		Map<String, Template> element_map = new HashMap<>();

		//check if element is a child of another element in the list. if yes then don't add it to the list
		for(int idx1=0; idx1 < list_elements_list.size(); idx1++){
			boolean is_child = false;
			for(int idx2=0; idx2 < list_elements_list.size(); idx2++){
				if(idx1 != idx2 && list_elements_list.get(idx2).getTemplate().contains(list_elements_list.get(idx1).getTemplate())
						&& !list_elements_list.get(idx2).getTemplate().equals(list_elements_list.get(idx1).getTemplate())){
					is_child = true;
					break;
				}
			}

			if(!is_child){
				element_map.get(list_elements_list.get(idx1).getTemplate()).getElements().add(list_elements_list.get(idx1));
			}
		}

		//remove duplicates
		log.warn("total elements left after reduction :: " + element_map.values().size());
		return element_map;
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
}
