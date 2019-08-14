package com.qanairy.services;

import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.imageio.ImageIO;

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
import com.qanairy.models.Test;
import com.qanairy.models.enums.BrowserEnvironment;
import com.qanairy.models.enums.FormStatus;
import com.qanairy.models.enums.FormType;
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
	private Crawler crawler;
	
	private static String[] valid_xpath_attributes = {"class", "id", "name", "title", "src", "href"};

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
		return BrowserConnectionFactory.getConnection(browser_name, browser_env);
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

	public List<PageState> buildPageStates(String url, String browser_name, String host, List<PathObject> path_objects, List<String> path_keys) 
			throws MalformedURLException, IOException, Exception{
		List<PageState> page_states = new ArrayList<>();
		boolean error_occurred = false;
		Map<String, ElementState> element_hash = new HashMap<String, ElementState>();
		Map<String, ElementState> element_xpaths = new HashMap<>();
		boolean elements_built_successfully = false;

		Browser browser = null;
		boolean is_browser_closed = true;
		Map<String, ElementState> visible_element_map = new HashMap<>();
		List<ElementState> visible_elements = new ArrayList<>();
		
		do{
			try{
				error_occurred = false;
				if(is_browser_closed){
					browser = BrowserConnectionFactory.getConnection(browser_name, BrowserEnvironment.DISCOVERY);
					browser.navigateTo(url);
					is_browser_closed = false;
					crawler.crawlPathWithoutBuildingResult(path_keys, path_objects, browser, host);
					BrowserUtils.getLoadingAnimation(browser, host);
				}
					
				if(!elements_built_successfully){
					//element_xpath_list = getXpathsUsingJSoup(browser.getDriver().getPageSource());
					List<ElementState> element_list = BrowserService.getElementsUsingJSoup(browser.getDriver().getPageSource());

					visible_elements = getVisibleElements(browser, visible_element_map, element_list);
				}
			}catch(NullPointerException e){
				log.warn("Error happened while browser service attempted to build page states  :: "+e.getMessage());
				error_occurred = true;
				is_browser_closed = true;
			} catch (GridException e) {
				log.warn("Grid exception encountered while trying to build page states"+e.getMessage());
				error_occurred = true;
				is_browser_closed = true;
			}
			catch (NoSuchElementException e){
				log.error("Unable to locate element while performing build page states   ::    "+ e.getMessage());
				error_occurred = true;
			}
			catch (WebDriverException e) {
				//TODO: HANDLE EXCEPTION THAT OCCURS BECAUSE THE PAGE ELEMENT IS NOT ON THE PAGE
				log.debug("WebDriver exception encountered while trying to crawl exporatory path"+e.getMessage());
				error_occurred = true;
				is_browser_closed = true;
			} catch(Exception e){
				log.warn("Exception occurred in getting page states. \n"+e.getMessage());
				error_occurred = true;
				is_browser_closed = true;
			}
			finally{
				if(browser != null && is_browser_closed){
					browser.close();
				}
			}
		}while(error_occurred);
		
		log.warn("####  returning elements list : "+visible_elements.size()+ "   :    "+url);

		for(ElementState elem : visible_elements){
			element_xpaths.put(elem.getXpath(), elem);
		}

		// BUILD ALL PAGE STATES
		elements_built_successfully = true;
		int iter_idx=0;
		boolean err = true;
		
		while(element_xpaths.size() > 0){
			log.warn("building page with remaining element xpaths :: " +element_xpaths.size());
			try{
				List<ElementState> remaining_elements = new ArrayList<ElementState>(element_xpaths.values());
				Collections.sort(remaining_elements);
				if(err){
					log.warn("getting browser connection");
					browser = BrowserConnectionFactory.getConnection(browser_name, BrowserEnvironment.DISCOVERY);
					browser.navigateTo(url);
					crawler.crawlPathWithoutBuildingResult(path_keys, path_objects, browser, host);
					BrowserUtils.getLoadingAnimation(browser, host);
				}
				err = false;

				if(!isElementVisibleInPane(browser, remaining_elements.get(0)) || iter_idx > 1){
					element_hash.put(remaining_elements.get(0).getXpath(), remaining_elements.get(0));

					browser.scrollTo(remaining_elements.get(0).getXLocation(), remaining_elements.get(0).getYLocation());
					BrowserUtils.detectShortAnimation(browser, url);
					iter_idx=0;
				}

				log.warn("getting current url from browser");
				String browser_url = browser.getDriver().getCurrentUrl();
				String url_without_params = BrowserUtils.sanitizeUrl(browser_url);
				log.warn("getting visible elements within viewport " + visible_elements.size());
				List<ElementState> all_visible_elements = getVisibleElementsWithinViewport(browser, browser.getViewportScreenshot(), new HashMap<Integer, ElementState>(), visible_elements, true);
				log.warn("building page with # of elements :: " +all_visible_elements.size());
				PageState page_state = buildPage(browser, all_visible_elements, url_without_params);
				for(ElementState element : page_state.getElements()){
					element_hash.put(element.getXpath(), element);
				}
				element_xpaths = BrowserService.filterElementStatesFromList(element_xpaths, element_hash.keySet());

				if(remaining_elements.size() != element_xpaths.size() && page_state.getElements().size() > 0){
					page_states.add(page_state);
				}

				iter_idx++;
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
		}

		element_xpaths = new HashMap<String, ElementState>();
		
		error_occurred = false;
		return page_states;
	}
	
	private boolean isElementLargerThanViewport(Browser browser, WebElement elementState) {
		int height = elementState.getSize().getHeight();
		int width = elementState.getSize().getWidth();

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
			if(child_node_cnt == 0 && !isStructureTag(element.tagName()) && !doesElementBelongToScriptTag(element)){
				elements.add(xpath);
			}
		}

		return elements;
	}

	public static List<ElementState> getElementsUsingJSoup(String pageSource) {
		Map<String, Integer> xpath_cnt_map = new HashMap<>();
		List<ElementState> elements = new ArrayList<>();
		Document html_doc = Jsoup.parse(pageSource);
		List<Element> web_elements = Xsoup.compile("//body//*").evaluate(html_doc).getElements();
		for(Element element: web_elements){
			int child_node_cnt = element.children().size();			
			
			if(child_node_cnt == 0 && !isStructureTag(element.tagName()) && !doesElementBelongToScriptTag(element)){
				String xpath = generateXpathUsingJsoup(element, html_doc, element.attributes(), xpath_cnt_map);
				Set<Attribute> attributes = generateAttributesUsingJsoup(element);
				ElementState element_state = new ElementState();
				element_state.setXpath(xpath);
				element_state.setAttributes(attributes);
				elements.add(element_state);
			}
		}

		return elements;
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
	public PageState buildPage(Browser browser, List<ElementState> all_elements, String url) throws GridException, IOException, NoSuchAlgorithmException{
		assert browser != null;
		
		BufferedImage viewport_screenshot = browser.getViewportScreenshot();
		String screenshot_checksum = PageState.getFileChecksum(viewport_screenshot);
		PageState page_state_record2 = page_state_service.findByScreenshotChecksum(screenshot_checksum);

		if(page_state_record2 != null){
			viewport_screenshot.flush();
			page_state_record2.setElements(page_state_service.getElementStates(page_state_record2.getKey()));
			page_state_record2.setScreenshots(page_state_service.getScreenshots(page_state_record2.getKey()));
			return page_state_record2;
		}
		else{
			//extract visible elements from list of elementstates provided
			List<ElementState> visible_elements = new ArrayList<>();
			for(ElementState element : all_elements){
				if(isElementVisibleInPane(browser, element)){
					ElementState new_element_state = element.clone();
					WebElement new_element = browser.findWebElementByXpath(element.getXpath());
					Point location = new_element.getLocation();
					Dimension size = new_element.getSize();
					
					new_element_state.setXLocation(location.getX());
					new_element_state.setYLocation(location.getY());
					new_element_state.setWidth(size.getWidth());
					new_element_state.setHeight(size.getHeight());
					new_element_state.setKey( new_element_state.generateKey());
					visible_elements.add(new_element_state);
				}
			}

			String viewport_screenshot_url = UploadObjectSingleOperation.saveImageToS3(viewport_screenshot, new URL(url).getHost(), screenshot_checksum, browser.getBrowserName()+"-viewport");
		
			List<ElementState> elements_with_screenshots = new ArrayList<>();
			for(ElementState element_state : visible_elements){
				//BufferedImage page_screenshot = ImageIO.read(new URL(page_state.getScreenshotUrl()));
				ElementState elem = retrieveAndUploadBrowserScreenshot(browser, element_state, viewport_screenshot, new URL(url).getHost(), browser.getXScrollOffset(), browser.getYScrollOffset());
				elements_with_screenshots.add(elem);
			}
			
			PageState page_state = new PageState( url,
					viewport_screenshot_url,
					elements_with_screenshots,
					org.apache.commons.codec.digest.DigestUtils.sha256Hex(Browser.cleanSrc(browser.getDriver().getPageSource())),
					browser.getXScrollOffset(),
					browser.getYScrollOffset(),
					browser.getViewportSize().width,
					browser.getViewportSize().height,
					browser.getBrowserName());
			page_state.addScreenshotChecksum(screenshot_checksum);
			Screenshot screenshot = new Screenshot(viewport_screenshot_url, browser.getBrowserName(), screenshot_checksum, browser.getViewportSize().getWidth(), browser.getViewportSize().getHeight());			
			page_state.addScreenshot(screenshot);
			
			viewport_screenshot.flush();
			return page_state;
		}
	}
	
	private static Map<String, ElementState> filterElementStatesFromList(Map<String, ElementState> elements,
			Collection<String> values) {
		HashMap<String, ElementState> elements_hash = new HashMap<>(elements);

		for(String xpath : values){
			if(xpath != null){
				elements_hash.remove(xpath);
			}
		}
		return elements_hash;
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

	public static List<WebElement> filterNotVisibleInViewport(int x_offset, int y_offset, List<WebElement> web_elements, Dimension viewport_size) {
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
	@Deprecated
	public PageState buildPage(Browser browser) throws GridException, IOException, NoSuchAlgorithmException{
		assert browser != null;
		
		String browser_url = browser.getDriver().getCurrentUrl();
		URL page_url = new URL(browser_url);
		String url_without_params = BrowserUtils.sanitizeUrl(browser_url);
		
		BufferedImage viewport_screenshot = browser.getViewportScreenshot();
		String screenshot_checksum = PageState.getFileChecksum(viewport_screenshot);
		PageState page_state_record2 = page_state_service.findByScreenshotChecksum(screenshot_checksum);
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
	@Deprecated
	public List<ElementState> getVisibleElements(Browser browser, String xpath, String host, BufferedImage page_screenshot)
															 throws WebDriverException, GridException, IOException{

		
		List<WebElement> web_elements = browser.getDriver().findElements(By.xpath("//body//*[not(*)]"));

		web_elements = BrowserService.filterNotVisibleInViewport(browser.getXScrollOffset(), browser.getYScrollOffset(), web_elements, browser.getViewportSize());
		web_elements = BrowserService.filterStructureTags(web_elements);
		web_elements = BrowserService.fitlerNonDisplayedElements(web_elements);
		//web_elements = BrowserService.filterNonChildElements(web_elements);
		web_elements = BrowserService.filterNoWidthOrHeight(web_elements);
		web_elements = BrowserService.filterElementsWithNegativePositions(web_elements);
		
		List<ElementState> elementList = new ArrayList<ElementState>(web_elements.size());
	
		for(WebElement elem : web_elements){
			if(elem.isDisplayed()){
				try{
					ElementState element_state = buildElementState(browser, elem, page_screenshot);
					if(element_state != null){
						elementList.add(element_state);
					}
				}catch(Exception e){}
			}
		}

		return elementList;
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
	 * 
	 * @pre visible_element_map != null
	 */
	public List<ElementState> getVisibleElements(Browser browser, Map<String, ElementState> visible_element_map, List<ElementState> elements)
															 throws WebDriverException, GridException, IOException{		
		assert visible_element_map != null;
		long start_time = System.currentTimeMillis();

		boolean err = false;
		do{
			err = false;
			try{
				int visible_map_size = visible_element_map.keySet().size();
				int start_idx = 0;
				if(visible_map_size > 1){
					start_idx = visible_map_size-1;
				}
				
				List<ElementState> element_sublist = elements.subList(start_idx, elements.size());
				for(ElementState element_state : element_sublist){
					WebElement element = browser.findWebElementByXpath(element_state.getXpath());
					if(element.isDisplayed() && hasWidthAndHeight(element.getSize()) && !isElementLargerThanViewport(browser, element)){
						ElementState new_element_state = buildElementState(browser, element, element_state.getXpath(), element_state.getAttributes());
						visible_element_map.put(element_state.getXpath().trim(), new_element_state);
					}
					else{
						visible_element_map.put(element_state.getXpath().trim(), null);
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

		List<ElementState> visible_elements = new ArrayList<>();
		for(ElementState element : visible_element_map.values()){
			if(element != null){
				visible_elements.add(element);
			}
		}
		
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
					WebElement element = browser.findWebElementByXpath(element_state.getXpath());
					if(element.isDisplayed() && hasWidthAndHeight(element.getSize()) && isElementVisibleInPane(browser, element.getLocation(), element.getSize())){
						ElementState new_element_state = buildElementState(browser, element, page_screenshot, element_state.getXpath(), element_state.getAttributes());
						if(new_element_state != null){
							visible_elements.add(new_element_state);
							visible_element_map.put(visible_map_size, new_element_state);
							visible_map_size++;
						}
					}
				}
			}catch(WebDriverException e){
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
		BufferedImage img = Browser.getElementScreenshot(elem, page_screenshot, browser);
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
	 * 
	 * @return
	 * 
	 * @throws IOException
	 */
	public ElementState buildElementState(Browser browser, WebElement elem, BufferedImage page_screenshot, String xpath, Set<Attribute> attributes) throws IOException{
		String element_tag_name = elem.getTagName();
		Point location = elem.getLocation();
		Dimension element_size = elem.getSize();

		//boolean negative_position = doesElementHaveNegativePosition(location);
		boolean is_structure_tag = isStructureTag(element_tag_name);
		boolean has_width_and_height = hasWidthAndHeight(element_size);
		
		if(!elem.isDisplayed() || is_structure_tag || !has_width_and_height){
			return null;
		}
		
		String checksum = "";
		String screenshot = null;
		ElementState page_element = null;
		BufferedImage img = Browser.getElementScreenshot(elem, page_screenshot, browser);
		checksum = PageState.getFileChecksum(img);
		ElementState page_element_record = element_state_service.findByScreenshotChecksum(checksum);

		if(page_element_record != null){
			page_element = page_element_record;
		}
		else{
			Map<String, String> css_props = Browser.loadCssProperties(elem);
			page_element = new ElementState(elem.getText(), xpath, element_tag_name, attributes, css_props, null, checksum, location.getX(), location.getY(), element_size.getWidth(), element_size.getHeight(), elem.getAttribute("innerHTML") );
			
			boolean err = false;
			int count = 0;
			do{
				err = false;
				try{
					screenshot = UploadObjectSingleOperation.saveImageToS3(img, (new URL(browser.getDriver().getCurrentUrl())).getHost(), checksum, browser.getBrowserName()+"-element");
				}catch(IOException e){
					err = true;
				}
				count++;
			}while(err && count < 100);
					
			page_element.setScreenshot(screenshot);
			page_element.setScreenshotChecksum(checksum);
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
	public ElementState buildElementState(Browser browser, WebElement elem, String xpath, Set<Attribute> attributes) throws IOException{
		long start_time = System.currentTimeMillis();

		String checksum = "";
		ElementState page_element = null;
		String element_tag_name = elem.getTagName();
		Point location = elem.getLocation();
		Dimension element_size = elem.getSize();
		boolean has_negative_position = doesElementHaveNegativePosition(location);
		boolean is_structure_tag = isStructureTag(element_tag_name);
		boolean has_width_and_height = hasWidthAndHeight(element_size);
		boolean is_child = getChildElements(elem).isEmpty();
		
		if(!elem.isDisplayed() || has_negative_position || is_structure_tag || !has_width_and_height || !is_child){
			return null;
		}
		
		Map<String, String> css_props = Browser.loadCssProperties(elem);
		page_element = new ElementState(elem.getText(), xpath, elem.getTagName(), attributes, css_props, null, checksum, 
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
				|| "template".equals(tag_name) || "audio".equals(tag_name) || "iframe".equals(tag_name);
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
		int y_offset = browser.getYScrollOffset();
		int x_offset = browser.getXScrollOffset();

		int x = location.getX();
		int y = location.getY();

		int height = size.getHeight();
		int width = size.getWidth();

		return x >= x_offset && y >= y_offset && ((x-x_offset)+width) < (browser.getViewportSize().getWidth())
				&& ((y-y_offset)+height) < (browser.getViewportSize().getHeight());
	}

	public static boolean isElementVisibleInPane(Browser browser, ElementState elem){
		int x_offset = browser.getXScrollOffset();
		int y_offset = browser.getYScrollOffset();

		int x = elem.getXLocation();
		int y = elem.getYLocation();

		int height = elem.getHeight();
		int width = elem.getWidth();

		return x >= x_offset && y >= y_offset && ((x-x_offset)+width) < (browser.getViewportSize().getWidth())
				&& ((y-y_offset)+height) < (browser.getViewportSize().getHeight());
	}

	public static boolean isElementVisibleInPane(int x_offset, int y_offset, WebElement elem, Dimension viewport_size){
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

		int x = elem.getXLocation();
		int y = elem.getYLocation();

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

		List<WebElement> form_elements = browser.getDriver().findElements(By.xpath("//form"));

		BufferedImage page_screenshot = ImageIO.read(new URL(page.getScreenshotUrl()));
		String host = new URL(page.getUrl()).getHost();
		for(WebElement form_elem : form_elements){
			browser.scrollToElement(form_elem);
			BrowserUtils.detectShortAnimation(browser, page.getUrl());

			String screenshot_url = retrieveAndUploadBrowserScreenshot(browser, form_elem, page_screenshot, host);
			ElementState form_tag = new ElementState(form_elem.getText(), uniqifyXpath(form_elem, xpath_map, "//form", browser.getDriver()), "form", browser.extractAttributes(form_elem), Browser.loadCssProperties(form_elem), screenshot_url, form_elem.getLocation().getX(), form_elem.getLocation().getY(), form_elem.getSize().getWidth(), form_elem.getSize().getHeight(), form_elem.getAttribute("innerHTML"), PageState.getFileChecksum(ImageIO.read(new URL(screenshot_url))) );

			double[] weights = new double[1];
			weights[0] = 0.3;

			Form form = new Form(form_tag, new ArrayList<ElementState>(), findFormSubmitButton(form_elem, browser),
									"Form #1", weights, FormType.values(), FormType.UNKNOWN, new Date(), FormStatus.DISCOVERED );

			List<WebElement> input_elements =  form_elem.findElements(By.xpath(form_tag.getXpath() +"//input"));

			input_elements = BrowserService.fitlerNonDisplayedElements(input_elements);
			input_elements = BrowserService.filterStructureTags(input_elements);
			input_elements = BrowserService.filterNotVisibleInViewport(browser.getXScrollOffset(), browser.getYScrollOffset(), input_elements, browser.getViewportSize());
			input_elements = BrowserService.filterNoWidthOrHeight(input_elements);
			input_elements = BrowserService.filterElementsWithNegativePositions(input_elements);
			for(WebElement input_elem : input_elements){
				Set<Attribute> attributes = browser.extractAttributes(input_elem);

				String form_element_url = retrieveAndUploadBrowserScreenshot(browser, form_elem, page_screenshot, host);
				ElementState input_tag = new ElementState(input_elem.getText(), generateXpath(input_elem, "", xpath_map, browser.getDriver(), attributes), input_elem.getTagName(), attributes, Browser.loadCssProperties(input_elem), form_element_url, input_elem.getLocation().getX(), input_elem.getLocation().getY(), input_elem.getSize().getWidth(), input_elem.getSize().getHeight(), input_elem.getAttribute("innerHTML"), PageState.getFileChecksum(ImageIO.read(new URL(screenshot_url))) );

				if(input_tag == null || input_tag.getScreenshot()== null || input_tag.getScreenshot().isEmpty()){

					browser.scrollToElement(input_elem);
					//Crawler.performAction(new Action("click"), input_tag, browser.getDriver());
					BufferedImage viewport = browser.getViewportScreenshot();

					if(input_elem.getLocation().getX() < 0 || input_elem.getLocation().getY() < 0){
						continue;
					}
					BufferedImage img = Browser.getElementScreenshot(input_elem, viewport, browser);
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

			for(FormType type : form.getTypeOptions()){
				log.info(" FORM TYPE          :::::     "+type);
			}
			log.info("weights :: "+ form.getPrediction());
			form.setType(FormType.UNKNOWN);

			form.setDateDiscovered(new Date());
			log.info("form record discovered date :: "+form.getDateDiscovered());

			form.setName("Form #1");
			log.info("name :: "+form.getName());

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
					String screenshot_url = retrieveAndUploadBrowserScreenshot(browser, child);

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
					String screenshot_url = retrieveAndUploadBrowserScreenshot(browser, page_elem);

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

		String screenshot_url = retrieveAndUploadBrowserScreenshot(browser, submit_element);
		ElementState elem = new ElementState(submit_element.getText(), generateXpath(submit_element, "", new HashMap<String, Integer>(), browser.getDriver(), attributes), submit_element.getTagName(), attributes, Browser.loadCssProperties(submit_element), screenshot_url, submit_element.getLocation().getX(), submit_element.getLocation().getY(), submit_element.getSize().getWidth(), submit_element.getSize().getHeight(), submit_element.getAttribute("innerHTML"), PageState.getFileChecksum(ImageIO.read(new URL(screenshot_url))) );

	
		return elem;
	}

	/**
	 *
	 * @param driver
	 * @param elem
	 * @return
	 * @throws Exception
	 */
	public String retrieveAndUploadBrowserScreenshot(Browser browser, WebElement elem) throws IOException{
		BufferedImage img = null;
		String checksum = "";
		String screenshot_url = "";
		try{

			img = Browser.getElementScreenshot(elem, browser.getViewportScreenshot(), browser);
			checksum = PageState.getFileChecksum(img);
			screenshot_url = UploadObjectSingleOperation.saveImageToS3(img, (new URL(browser.getDriver().getCurrentUrl())).getHost(), checksum, browser.getBrowserName()+"-element");
		}
		catch(RasterFormatException e){
			log.warn("Raster Format Exception (retrieveAndUploadBrowserScreenshot)  2: "+e.getMessage());
		} catch (GridException e) {
			log.warn("Grid Exception occurred while retrieving and uploading "+e.getMessage());
		}
		finally{
			if(img != null){
				img.flush();
			}
		}
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
	public String retrieveAndUploadBrowserScreenshot(Browser browser, WebElement elem, BufferedImage page_img, String host) throws IOException {
		BufferedImage img = null;
		String checksum = "";
		String screenshot_url = "";
		boolean err = false;
		int count = 0;
		
		do{
			err = false;
			try{
				img = Browser.getElementScreenshot(elem, browser.getViewportScreenshot(), browser);
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
	public ElementState retrieveAndUploadBrowserScreenshot(Browser browser, ElementState elem, BufferedImage page_img, String host, int x_offset, int y_offset) throws IOException{
		BufferedImage img = null;
		String checksum = "";
		String screenshot_url = "";
		boolean err = false;
		int count = 0;
		
		ElementState elem_copy = elem.clone();
		do{
			err = false;
			try{
				img = Browser.getElementScreenshot(elem_copy, page_img, x_offset, y_offset);
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
}
