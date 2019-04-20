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
import java.util.NoSuchElementException;
import java.util.Set;

import javax.imageio.ImageIO;

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
import com.qanairy.models.Action;
import com.qanairy.models.Attribute;
import com.qanairy.models.Form;
import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.enums.BrowserEnvironment;
import com.qanairy.models.enums.FormStatus;
import com.qanairy.models.enums.FormType;
import com.qanairy.models.rules.Rule;

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
	private ElementStateService page_element_service;
	
	@Autowired
	private ElementRuleExtractor extractor;
	
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
	public boolean checkIfLandable(String browser, PageState page_state){
		boolean isLandable = false;
		boolean page_visited_successfully = false;
		int cnt  = 0;
		do{
			page_visited_successfully = false;
			Browser landable_browser = null;
			try{
				landable_browser = BrowserConnectionFactory.getConnection(browser, BrowserEnvironment.DISCOVERY);
				landable_browser.navigateTo(page_state.getUrl());
				
				PageState landable_page_state = buildPage(landable_browser);
				log.warn("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
				log.warn("page state key, checksum    : " + page_state.getKey()+"   ,   "+page_state.getScreenshotChecksum() +" :: "+page_state.getUrl());
				log.warn("page state key, checksum    : " + landable_page_state.getKey()+"   ,   "+landable_page_state.getScreenshotChecksum()+" :: "+page_state.getUrl());
				log.warn("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
				if(page_state.equals(landable_page_state)){
					isLandable = true;
				}
				else{
					isLandable = false;
				}

				page_visited_successfully = true;
			}
			catch(GridException e){
				log.warn(e.getMessage());
			}
			catch(Exception e){
				log.info("ERROR CHECKING LANDABILITY OF PAGE AT ::: "+ e.getMessage());
			}
			finally {
				if(landable_browser != null){
					landable_browser.close();
				}
			}
			cnt++;
		}while(!page_visited_successfully && cnt < Integer.MAX_VALUE);
		
		log.warn("is page state landable  ?? :: "+isLandable);
		return isLandable;
	}
	
	public List<PageState> buildPageStates(String url, String browser_name){
		List<PageState> page_states = new ArrayList<>();
		boolean error_occurred = false;		
		Map<String, ElementState> seen_element_state = new HashMap<String, ElementState>();
		
		Browser browser = null;
		do{
			try{
				browser = BrowserConnectionFactory.getConnection(browser_name, BrowserEnvironment.DISCOVERY);
				browser.navigateTo(url);
				//get current viewport screenshot
				String browser_url = browser.getDriver().getCurrentUrl();
				URL page_url = new URL(browser_url);
		        
				int param_index = page_url.toString().indexOf("?");
				String url_without_params = page_url.toString();
				if(param_index >= 0){
					url_without_params = url_without_params.substring(0, param_index);
				}
				
				List<WebElement> web_elements = browser.getDriver().findElements(By.cssSelector("*"));

				web_elements = BrowserService.fitlerNonDisplayedElements(web_elements);
				web_elements = BrowserService.filterStructureTags(web_elements);
				web_elements = BrowserService.filterNoWidthOrHeight(web_elements);
				web_elements = BrowserService.filterNonChildElements(web_elements);
				web_elements = BrowserService.filterElementsWithNegativePositions(web_elements);
				web_elements = BrowserService.filterElementStatesFromList(web_elements, new ArrayList<ElementState>(seen_element_state.values()) );

				int iter_idx=0;
				int last_web_element_size = web_elements.size();
				while(!web_elements.isEmpty()){					
					//log.warn("BrowserService ...identifying page state iteration ...."+iter_idx+"....cycle::... "+cycle_cnt+" elements remaining ...."+web_elements.size());
					if(iter_idx > 1){
						web_elements = web_elements.subList(1, web_elements.size());
						iter_idx=0;
					}
					if(web_elements.isEmpty()){
						break;
					}
					if(!isElementVisibleInPane(browser, web_elements.get(0)) || iter_idx > 0){
						browser.scrollToElement(web_elements.get(0));
					}
					PageState page_state = buildPage(browser);
					Map<String, ElementState> element_hash = new HashMap<String, ElementState>();

					for(ElementState element : page_state.getElements()){
						element_hash.put(element.getXLocation()+""+element.getYLocation(), element);
						seen_element_state.put(element.getKey(), element);
					}
					
					List<WebElement> filtered_list = new ArrayList<>();
					for(WebElement elem : web_elements){
						if(!element_hash.containsKey(elem.getLocation().getX()+""+elem.getLocation().getY())){
							filtered_list.add(elem);
						}
					}
					
					web_elements = filtered_list;
					
					page_states.add(page_state);
					
					if(web_elements.size() == last_web_element_size){
						iter_idx++;
					}
					last_web_element_size = web_elements.size();
				}
				error_occurred = false;
				break;
			}catch(NullPointerException e){
				log.warn("Error happened while browser service attempted to build page states  :: "+e.getMessage());
				e.printStackTrace();
				error_occurred = true;
			} catch (GridException e) {
				log.warn("Grid exception encountered while trying to build page states"+e.getMessage());
				error_occurred = true;
			}
			catch (NoSuchElementException e){
				log.error("Unable to locate element while performing build page states   ::    "+ e.getMessage());
				e.printStackTrace();
				error_occurred = true;
			}
			catch (WebDriverException e) {
				//TODO: HANDLE EXCEPTION THAT OCCURS BECAUSE THE PAGE ELEMENT IS NOT ON THE PAGE
				log.warn("WebDriver exception encountered while trying to crawl exporatory path"+e.getMessage());
				error_occurred = true;
				e.printStackTrace();
			} catch(Exception e){
				log.warn("Exception occurred in getting page states. \n"+e.getMessage());
				e.printStackTrace();
				error_occurred = true;
			}
			finally{
				if(browser != null){
					browser.close();
				}
			}
		}while(error_occurred);
		
		return page_states;
	}
	
	private static List<WebElement> filterElementStatesFromList(List<WebElement> web_elements,
			List<ElementState> values) {
		Map<String, ElementState> element_hash = new HashMap<String, ElementState>();
		for(ElementState element : values){
			element_hash.put(element.getXLocation()+""+element.getYLocation(), element);
		}
		
		List<WebElement> filtered_list = new ArrayList<>();
		for(WebElement elem : web_elements){
			if(!element_hash.containsKey(elem.getLocation().getX()+""+elem.getLocation().getY())){
				filtered_list.add(elem);
			}
		}
		
		return filtered_list;
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

	public static List<WebElement> filterNotVisibleInViewport(Browser browser, List<WebElement> web_elements) {
		List<WebElement> elements = new ArrayList<>();
		
		for(WebElement element : web_elements){
			if(isElementVisibleInPane(browser, element)){
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
			if(element.getTagName().equals("html") || element.getTagName().equals("body")
					|| element.getTagName().equals("link") || element.getTagName().equals("script")
					|| element.getTagName().equals("title") || element.getTagName().equals("meta")
					|| element.getTagName().equals("head")){
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
	 * 
	 * @pre browser != null
	 * @post page_state != null
	 */
	public PageState buildPage(Browser browser) throws GridException, IOException, NoSuchAlgorithmException{
		assert browser != null;
		
		String browser_url = browser.getDriver().getCurrentUrl();
		URL page_url = new URL(browser_url);
        
		int param_index = page_url.toString().indexOf("?");
		String url_without_params = page_url.toString();
		if(param_index >= 0){
			url_without_params = url_without_params.substring(0, param_index);
		}

		BufferedImage viewport_screenshot = browser.getViewportScreenshot();		
		PageState page_state_record2 = page_state_service.findByScreenshotChecksum(PageState.getFileChecksum(viewport_screenshot));
		if(page_state_record2 != null){
			viewport_screenshot.flush();
			page_state_record2.setElements(page_state_service.getElementStates(page_state_record2.getKey()));
			return page_state_record2;
		}
		
		Set<ElementState> visible_elements = getVisibleElements(browser, "", page_url.toString());
		log.warn("Retrieved visible elements..."+visible_elements.size()+"   ....url  ::  "+page_url);

		PageState page_state = new PageState(	page_url.toString(),
				visible_elements,
				org.apache.commons.codec.digest.DigestUtils.sha256Hex(Browser.cleanSrc(browser.getDriver().getPageSource())),
				browser.getXScrollOffset(), 
				browser.getYScrollOffset(),
				browser.getViewportSize().width,
				browser.getViewportSize().height,
				browser.getBrowserName());

		PageState page_state_record = page_state_service.findByKey(page_state.getKey());
		if(page_state_record != null){			
			page_state = page_state_record;
		}
		else{
			String viewport_screenshot_url = UploadObjectSingleOperation.saveImageToS3(viewport_screenshot, page_url.getHost(), page_state.getKey(), "viewport");
			page_state.setScreenshotUrl(viewport_screenshot_url);
			page_state = page_state_service.save(page_state);
		}

		viewport_screenshot.flush();
		return page_state;
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
	public PageState buildPage(Browser browser, List<WebElement> elements) throws GridException, IOException, NoSuchAlgorithmException{
		assert browser != null;
		
		String browser_url = browser.getDriver().getCurrentUrl();
		URL page_url = new URL(browser_url);
        
		int param_index = page_url.toString().indexOf("?");
		String url_without_params = page_url.toString();
		if(param_index >= 0){
			url_without_params = url_without_params.substring(0, param_index);
		}

		BufferedImage viewport_screenshot = browser.getViewportScreenshot();		
		PageState page_state_record2 = page_state_service.findByScreenshotChecksum(PageState.getFileChecksum(viewport_screenshot));
		if(page_state_record2 != null){
			viewport_screenshot.flush();
			return page_state_record2;
		}
		
		log.debug("Getting visible elements...");
		Set<ElementState> visible_elements = getVisibleElements(browser, "", page_url.getHost());
		
		PageState page_state = new PageState(	page_url.toString(),
				visible_elements,
				org.apache.commons.codec.digest.DigestUtils.sha256Hex(Browser.cleanSrc(browser.getDriver().getPageSource())),
				browser.getXScrollOffset(), 
				browser.getYScrollOffset(),
				browser.getViewportSize().width,
				browser.getViewportSize().height,
				browser.getBrowserName());

		log.debug("calculated page state key :: "+ page_state.getKey());

		PageState page_state_record = page_state_service.findByKey(page_state.getKey());
		if(page_state_record != null){			
			page_state = page_state_record;
		}
		else{
			String viewport_screenshot_url = UploadObjectSingleOperation.saveImageToS3(viewport_screenshot, page_url.getHost(), page_state.getKey(), "viewport");
			page_state.setScreenshotUrl(viewport_screenshot_url);
			page_state = page_state_service.save(page_state);
		}

		viewport_screenshot.flush();
		return page_state;
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
	public Set<ElementState> getVisibleElements(Browser browser, String xpath, String host) 
															 throws WebDriverException, GridException, IOException{
		
		List<WebElement> web_elements = browser.getDriver().findElements(By.xpath("//*"));
		BufferedImage page_screenshot = browser.getViewportScreenshot();

		web_elements = BrowserService.filterNotVisibleInViewport(browser, web_elements);
		web_elements = BrowserService.fitlerNonDisplayedElements(web_elements);
		web_elements = BrowserService.filterNonChildElements(web_elements);
		web_elements = BrowserService.filterNoWidthOrHeight(web_elements);
		Set<ElementState> elementList = new HashSet<ElementState>(web_elements.size());

		for(WebElement elem : web_elements){
			ElementState element_state = buildElementState(browser, elem, page_screenshot);
			elementList.add(element_state);
		}
		
		page_screenshot.flush();

		return elementList;
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

		BufferedImage img = null;
		String checksum = "";
		String screenshot = null;
		ElementState page_element_record = null;
		ElementState page_element = null;
		do{
			try{
				//log.warn("Checking if element visible in viewport");
				img = browser.getElementScreenshot(elem, page_screenshot);
				checksum = PageState.getFileChecksum(img);	
				page_element_record = page_element_service.findByScreenshotChecksum(checksum);
			}
			catch(RasterFormatException e){
				log.warn("Raster Format Exception : "+e.getMessage());
			}
		}while(img == null);

		if(page_element_record != null){
			page_element = page_element_record;
		}
		else{
			//TODO: refactor code to handle this asynchronously. Loading CSS properties currently ranges from 470ms-800ms as of 3/25/2019
			Map<String, String> css_props = Browser.loadCssProperties(elem);
			Set<Attribute> attributes = browser.extractAttributes(elem);

			page_element = new ElementState(elem.getText(), null, elem.getTagName(), attributes, css_props, null, checksum, elem.getLocation().getX(), elem.getLocation().getY(), elem.getSize().getWidth(), elem.getSize().getHeight() );				
			page_element_record = page_element_service.findByKey(page_element.getKey()) ;

			if(page_element_record != null){
				page_element = page_element_record;
			}
			else{
				screenshot = UploadObjectSingleOperation.saveImageToS3(img, (new URL(browser.getDriver().getCurrentUrl())).getHost(), checksum, "element_screenshot");	
				
				//TODO: refactor xpath to generation to be faster. Generating xpath can take over 1.6s
				String element_xpath = generateXpath(elem, "", xpath_map, browser.getDriver(), attributes);
				
				page_element.setScreenshot(screenshot);
				page_element.setXpath(element_xpath);
				page_element = page_element_service.save(page_element);
			}
		}
		img.flush();

		return page_element;
	}
	
	
	public static List<WebElement> filterNoWidthOrHeight(List<WebElement> web_elements) {
		List<WebElement> elements = new ArrayList<WebElement>(web_elements.size());
		for(WebElement element : web_elements){
			Dimension dimension = element.getSize();
			if(dimension.getHeight() > 0 && dimension.getWidth() > 0){
				elements.add(element);
			}
		}
	
		return elements;
	}
	

	public static boolean isElementVisibleInPane(Browser browser, WebElement elem){
		int y_offset = browser.getYScrollOffset();
		int x_offset = browser.getXScrollOffset();
		
		Point location = elem.getLocation();
		int x = location.getX();
		int y = location.getY();
		
		Dimension dimension = elem.getSize();
		int height = dimension.getHeight();
		int width = dimension.getWidth();
		
		if(x >= x_offset && y >= y_offset && (x+width) < (browser.getViewportSize().getWidth()+x_offset) 
				&& (y+height) < (browser.getViewportSize().getHeight()+y_offset)){
			return true;
		}
		return false;
	}
	
	public static boolean isElementVisibleInPane(Browser browser, ElementState elem){
		int y_offset = browser.getYScrollOffset();
		int x_offset = browser.getXScrollOffset();
		
		int x = elem.getXLocation();
		int y = elem.getYLocation();
		
		int height = elem.getHeight();
		int width = elem.getWidth();
		
		if(x >= x_offset && y >= y_offset && (x+width) <= (browser.getViewportSize().getWidth()-1+x_offset) 
				&& (y+height) <= (browser.getViewportSize().getHeight()-1+y_offset)){
			return true;
		}
		return false;
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
	        returnString = "concat(";
	        while (quotePos != -1)
	        {
	            String subString = searchString.substring(0, quotePos);
	            returnString += "'" + subString + "', ";
        
                //must be a double quote
                returnString += "'\"', ";
                searchString = searchString.substring(quotePos + 1,
	                             searchString.length());
	            quotePos = searchString.indexOf("\"");
	        }
	        returnString += "'" + searchString + "')";
	    }

	    return returnString;
	}
	
	private static String escapeQuotes(String string) {
		return string.replace("\'", "'");
	}

	/**
	 * generates a unique xpath for this element.
	 * 
	 * @return an xpath that identifies this element uniquely
	 */
	public String generateXpath(WebElement element, String xpath, Map<String, Integer> xpathHash, WebDriver driver, Set<Attribute> attributes){
		ArrayList<String> attributeChecks = new ArrayList<String>();

		xpath += "//"+element.getTagName();
		for(Attribute attr : attributes){
			if(Arrays.asList(valid_xpath_attributes).contains(attr.getName())){
				
				String attribute_values = ArrayUtility.joinArray(attr.getVals().toArray(new String[attr.getVals().size()]));
				if(attribute_values.contains("\"")){
					attributeChecks.add("contains(@" + attr.getName() + ",\"" +generateConcatForXPath(attribute_values.trim())+ "\")");
				}
				else{
					attributeChecks.add("contains(@" + attr.getName() + ",\"" + escapeQuotes(attribute_values.trim()) + "\")");
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
	    while(!parent.getTagName().equals("html") && !parent.getTagName().equals("body") && parent != null && count < 4){
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
			String element_text = elem.getText();
			
			if(elements.size()>1){
				int count = 1;
				for(WebElement element : elements){
					if(element.getTagName().equals(element_tag_name)
							&& element.getText().equals(element_text)){
						return "("+xpath+")[" + count + "]";	
					}					
					count++;
				}
			}
			
		}catch(InvalidSelectorException e){
			log.warn(e.getMessage());
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
		for(WebElement form_elem : form_elements){
			List<String> form_xpath_list = new ArrayList<String>();
			String page_screenshot = "";
			
			String screenshot_url = retrieveAndUploadBrowserScreenshot(browser, form_elem, ImageIO.read(new URL(page_screenshot)));
			ElementState form_tag = new ElementState(form_elem.getText(), uniqifyXpath(form_elem, xpath_map, "//form", browser.getDriver()), "form", browser.extractAttributes(form_elem), Browser.loadCssProperties(form_elem), screenshot_url, form_elem.getLocation().getX(), form_elem.getLocation().getY(), form_elem.getSize().getWidth(), form_elem.getSize().getHeight() );
			
			form_tag.setScreenshot(screenshot_url);
			
			double[] weights = new double[1];
			weights[0] = 0.3;
			
			Form form = new Form(form_tag, new ArrayList<ElementState>(), findFormSubmitButton(form_elem, browser), 
									"Form #1", weights, FormType.values(), FormType.UNKNOWN, new Date(), FormStatus.DISCOVERED );
			List<WebElement> input_elements =  form_elem.findElements(By.xpath(form_tag.getXpath() +"//input"));
			for(WebElement input_elem : input_elements){
				Set<Attribute> attributes = browser.extractAttributes(input_elem);
				
				screenshot_url = retrieveAndUploadBrowserScreenshot(browser, form_elem, ImageIO.read(new URL(page_screenshot)));
				ElementState input_tag = new ElementState(input_elem.getText(), generateXpath(input_elem, "", xpath_map, browser.getDriver(), attributes), input_elem.getTagName(), attributes, Browser.loadCssProperties(input_elem), screenshot_url, input_elem.getLocation().getX(), input_elem.getLocation().getY(), input_elem.getSize().getWidth(), input_elem.getSize().getHeight() );
												
				if(input_tag == null || input_tag.getScreenshot()== null || input_tag.getScreenshot().isEmpty()){

					Crawler.performAction(new Action("click"), input_tag, browser.getDriver());
					BufferedImage viewport = browser.getViewportScreenshot();
										
					if(input_elem.getLocation().getX() < 0 || input_elem.getLocation().getY() < 0){
						continue;
					}
					BufferedImage img = Browser.getElementScreenshot(viewport, input_elem.getSize(), input_elem.getLocation(), browser);

					viewport.flush();
					String screenshot= null;
					try {
						screenshot = UploadObjectSingleOperation.saveImageToS3(img, (new URL(browser.getDriver().getCurrentUrl())).getHost(), PageState.getFileChecksum(img), input_tag.getKey());
					} catch (Exception e) {
						log.warn("Error retrieving screenshot -- "+e.getLocalizedMessage());
					}
					img.flush();
					input_tag.setScreenshot(screenshot);
				}

				boolean alreadySeen = false;
				for(String xpath : form_xpath_list){
					if(xpath.equals(input_tag.getXpath())){
						alreadySeen = true;
					}
				}
				
				if(alreadySeen){
					//log.info("page element already seen before extracting form elements");
					continue;
				}						
				
				List<ElementState> group_inputs = constructGrouping(input_elem, browser);
				
				//Set<ElementState> labels = findLabelsForInputs(form_elem, group_inputs, browser.getDriver());
				/*for(FormField input_field : group_inputs){
					try{
						ElementState label = findLabelForInput(form_elem, input_field, browser.getDriver());
						input_field.setFieldLabel(label);
					}
					catch(NullPointerException e){
						log.info("Error occurred while finding label for form input field");
					}
				}
				*/
				log.info("GROUP INPUTS    :::   "+group_inputs.size());
				for(ElementState page_elem : group_inputs){
					for(Rule rule : extractor.extractInputRules(page_elem)){
						log.info(" RULE     :::   "+ rule);
						log.info("INPUT ELEMENT "+page_elem);
						page_elem.addRule(rule);
					}
				}
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
				log.info("Invalid selector exception occurred " + e.getMessage());
				break;
			}
			
			if(allChildrenMatch){
				//create list with new elements
				List<WebElement> children = parent.findElements(By.xpath(".//input"));
				child_inputs = new ArrayList<ElementState>();

				for(WebElement child : children){
					Set<Attribute> attributes = browser.extractAttributes(child);
					String screenshot_url = retrieveAndUploadBrowserScreenshot(browser, child);

					ElementState elem = new ElementState(child.getText(), generateXpath(child, "", new HashMap<String, Integer>(), browser.getDriver(), attributes), child.getTagName(), attributes, Browser.loadCssProperties(child), screenshot_url, child.getLocation().getX(), child.getLocation().getY(), child.getSize().getWidth(), child.getSize().getHeight() );
					
					ElementState elem_record = page_element_service.findByKey(elem.getKey());
					
					if(elem_record != null){
						elem=elem_record;
					}
					else{
						elem_record = page_element_service.findByScreenshotChecksum(elem.getScreenshotChecksum());
						if(elem_record!= null){
							elem = elem_record;
						}
						else{
							elem = page_element_service.save(elem);
						}
					}
					
					//FormField input_field = new FormField(elem);
					
					child_inputs.add(elem);
				}
				
				page_elem = parent;
			}
			else{
				if(child_inputs.size() == 0){
					Set<Attribute> attributes = browser.extractAttributes(page_elem);
					String screenshot_url = retrieveAndUploadBrowserScreenshot(browser, page_elem);

					ElementState input_tag = new ElementState(page_elem.getText(), generateXpath(page_elem, "", new HashMap<String,Integer>(), browser.getDriver(), attributes), page_elem.getTagName(), attributes, Browser.loadCssProperties(page_elem), screenshot_url, page_elem.getLocation().getX(), page_elem.getLocation().getY(), page_elem.getSize().getWidth(), page_elem.getSize().getHeight() );
					ElementState elem_record = page_element_service.findByKey(input_tag.getKey());
					if(elem_record != null){
						input_tag=elem_record;
					}
					else{
						input_tag = page_element_service.save(input_tag);
					}
										
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
		try{
			submit_element = form_elem.findElement(By.xpath("//button[@type='submit']"));
		}
		catch(NoSuchElementException e){
			submit_element = form_elem.findElement(By.xpath("//input[@type='submit']"));
		}
		Set<Attribute> attributes = browser.extractAttributes(submit_element);
		String screenshot_url = retrieveAndUploadBrowserScreenshot(browser, form_elem);
		ElementState elem = new ElementState(submit_element.getText(), generateXpath(submit_element, "", new HashMap<String, Integer>(), browser.getDriver(), attributes), submit_element.getTagName(), attributes, Browser.loadCssProperties(submit_element), screenshot_url, submit_element.getLocation().getX(), submit_element.getLocation().getY(), submit_element.getSize().getWidth(), submit_element.getSize().getHeight() );
		
		ElementState elem_record = page_element_service.findByKey(elem.getKey());
		if(elem_record != null){
			elem = elem_record;
		}
		return elem;
	}
	
	/**
	 * 
	 * @param driver
	 * @param elem
	 * @return
	 * @throws Exception
	 */
	public String retrieveAndUploadBrowserScreenshot(Browser browser, WebElement elem) throws Exception{
		BufferedImage img = null;
		String checksum = "";
		String screenshot_url = "";
		try{
			
			img = Browser.getElementScreenshot(browser.getViewportScreenshot(), elem.getSize(), elem.getLocation(), browser);
			checksum = PageState.getFileChecksum(img);		
			screenshot_url = UploadObjectSingleOperation.saveImageToS3(img, (new URL(browser.getDriver().getCurrentUrl())).getHost(), checksum);
		}
		catch(RasterFormatException e){
			log.warn("Raster Format Exception : "+e.getMessage());
		} catch (GridException e) {
			log.warn("Grid Exception occurred while retrieving and uploading "+e.getMessage());
		} catch (IOException e) {
			log.warn("IOException occurred while retrieving and uploading "+e.getMessage());
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
	public String retrieveAndUploadBrowserScreenshot(Browser browser, WebElement elem, BufferedImage page_img) throws Exception{
		BufferedImage img = null;
		String checksum = "";
		String screenshot_url = "";
		try{
			img = Browser.getElementScreenshot(browser.getViewportScreenshot(), elem.getSize(), elem.getLocation(), browser);
			checksum = PageState.getFileChecksum(img);		
			screenshot_url = UploadObjectSingleOperation.saveImageToS3(img, (new URL(browser.getDriver().getCurrentUrl())).getHost(), checksum);	
		}
		catch(RasterFormatException e){
			log.warn("Raster Format Exception : "+e.getMessage());
		} catch (GridException e) {
			log.warn("Grid Exception occurred while retrieving and uploading "+e.getMessage());
		} catch (IOException e) {
			log.warn("IOException occurred while retrieving and uploading "+e.getMessage());
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
	 * @param browser
	 * @param page_state
	 * @return
	 * @throws GridException
	 * @throws IOException
	 */
	public boolean doScreenshotsMatch(Browser browser, PageState page_state) throws GridException, IOException{
		BufferedImage viewport_screenshot = browser.getViewportScreenshot();
		
		boolean pages_match = false;
		try {
			BufferedImage img1 = ImageIO.read(new URL(page_state.getScreenshotUrl()));
			BufferedImage img2 = viewport_screenshot;
			pages_match = PageState.compareImages(img1, img2);
			if(pages_match){
				return true;
			}
			log.info("DO THE SCREENSHOTS MATCH????        ::::     "+pages_match);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		viewport_screenshot.flush();
		
		return false;
	}
}
