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
import org.openqa.selenium.JavascriptExecutor;
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
import com.qanairy.models.PageElement;
import com.qanairy.models.PageState;
import com.qanairy.models.ScreenshotSet;
import com.qanairy.models.enums.BrowserEnvironment;
import com.qanairy.models.enums.FormStatus;
import com.qanairy.models.enums.FormType;
import com.qanairy.models.rules.Rule;

import akka.actor.ActorSystem;

/**
 * 
 * 
 */
@Component
public class BrowserService {
	private static Logger log = LoggerFactory.getLogger(BrowserService.class);
	
	private static final int DIMENSION_OFFSET_PIXELS = 5;
	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private PageElementService page_element_service;
	
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
				if(page_state.getKey().equals(landable_page_state.getKey())){
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
				log.warn("ERROR CHECKING LANDABILITY OF PAGE AT ::: "+ e.getMessage());
			}
			finally {
				if(landable_browser != null){
					landable_browser.close();
				}
			}
			cnt++;
		}while(!page_visited_successfully && cnt < Integer.MAX_VALUE);
		
		log.info("is page state landable  ?? :: "+page_state.isLandable());
		return isLandable;
	}
	
	
	/**
	 * Constructs a new page object
	 * @param url
	 * @param screenshot_url
	 * @param visible_elements
	 * @param is_landable
	 * @param src
	 * @param browser_name
	 * @return
	 */
	public PageState buildPage(URL url, String screenshot_url, Set<PageElement> visible_elements, boolean is_landable, String src, String browser_name){
		ScreenshotSet screenshot_record = new ScreenshotSet(screenshot_url, browser_name);
		Set<ScreenshotSet> screenshot_set = new HashSet<>();
		screenshot_set.add(screenshot_record);
		
		String clean_src = org.apache.commons.codec.digest.DigestUtils.sha256Hex(Browser.cleanSrc(src));
		return new PageState(url.toString(), screenshot_set, visible_elements, clean_src);
	}
	
	public PageState collectDataAndBuildPage(Browser browser) throws GridException, IOException{
		assert browser != null;
		
		URL url = new URL(browser.getDriver().getCurrentUrl());
		int param_index = url.toString().indexOf("?");
		String url_without_params = url.toString();
		if(param_index >= 0){
			url_without_params = url_without_params.substring(0, param_index);
		}
		
		String page_src = browser.getDriver().getPageSource();
		BufferedImage viewport_screenshot = Browser.getViewportScreenshot(browser.getDriver());		
		String page_key = "pagestate::" + org.apache.commons.codec.digest.DigestUtils.sha256Hex(url_without_params+ PageState.getFileChecksum(viewport_screenshot));
		
		String viewport_screenshot_url = UploadObjectSingleOperation.saveImageToS3(viewport_screenshot, url.getHost(), page_key, "viewport");
		log.info("Getting visible elements...");
		Set<PageElement> visible_elements = getVisibleElements(browser, "", url.getHost());

		return buildPage(url, viewport_screenshot_url, visible_elements, true, page_src, browser.getBrowserName());
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
	@Deprecated
	public PageState buildPage(Browser browser) throws GridException, IOException, NoSuchAlgorithmException{
		assert browser != null;
		
		log.debug("building page");
		String browser_url = browser.getDriver().getCurrentUrl();
		URL page_url = new URL(browser_url);
		BufferedImage viewport_screenshot = Browser.getViewportScreenshot(browser.getDriver());		
        
		int param_index = page_url.toString().indexOf("?");
		String url_without_params = page_url.toString();
		if(param_index >= 0){
			url_without_params = url_without_params.substring(0, param_index);
		}
		
		String page_key = "pagestate::" + org.apache.commons.codec.digest.DigestUtils.sha256Hex(url_without_params+ PageState.getFileChecksum(viewport_screenshot));
		log.debug("calculated page state key :: "+ page_key);

		PageState page_state = page_state_service.findByKey(page_key);
		if(page_state != null){
			page_state.setElements(page_state_service.getPageElements(page_key));
			page_state.setBrowserScreenshots(page_state_service.getScreenshots(page_key));
			return page_state;
		}
		log.debug("Getting visible elements...");
		Set<PageElement> visible_elements = getVisibleElements(browser, "", page_url.getHost());

		log.debug("uploading element screenshot to S3");
		String viewport_screenshot_url = UploadObjectSingleOperation.saveImageToS3(viewport_screenshot, page_url.getHost(), page_key, "viewport");
		
		ScreenshotSet screenshot_set = new ScreenshotSet(viewport_screenshot_url, browser.getBrowserName());
		HashSet<ScreenshotSet> screenshots = new HashSet<ScreenshotSet>();
		screenshots.add(screenshot_set);
		
		page_state = new PageState(	page_url.toString(),
				screenshots,
				visible_elements,
				org.apache.commons.codec.digest.DigestUtils.sha256Hex(Browser.cleanSrc(browser.getDriver().getPageSource())));
			
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
	private Set<PageElement> getVisibleElements(Browser browser, String xpath, String host) 
															 throws WebDriverException, GridException, IOException{
		
		List<WebElement> pageElements = browser.getDriver().findElements(By.cssSelector("*"));

		Set<PageElement> elementList = new HashSet<PageElement>();
		
		if(pageElements.size() == 0){
			return elementList;
		}
		BufferedImage page_screenshot = Browser.getViewportScreenshot(browser.getDriver());

		Map<String, Integer> xpath_map = new HashMap<String, Integer>();
		for(WebElement elem : pageElements){
			boolean is_child = getChildElements(elem).isEmpty();
			
			if(is_child && elem.isDisplayed()){
				
				BufferedImage img = null;
				String checksum = "";
				String screenshot = null;
				PageElement page_element_record = null;
				PageElement page_element = null;
				try{
					log.debug("Checking if element visible in viewport");
					if(!isElementVisibleInPane(browser, elem)){
						log.info("element not visible in viewport. SCROLLING TO ELEMENT");
						browser.scrollToElement(elem);
						page_screenshot = Browser.getViewportScreenshot(browser.getDriver());
					}
					img = Browser.getElementScreenshot(browser, elem, page_screenshot);
					checksum = PageState.getFileChecksum(img);		
				}
				catch(RasterFormatException e){
					log.info("Raster Format Exception : "+e.getMessage());
					continue;
				}
				
				page_element = new PageElement(elem.getText(), null, elem.getTagName(), null,  null, null, checksum);				
				page_element_record = page_element_service.findByKey(page_element.getKey()) ;

				if(page_element_record == null){

					screenshot = UploadObjectSingleOperation.saveImageToS3(img, (new URL(browser.getDriver().getCurrentUrl())).getHost(), checksum, "element_screenshot");	
					
					//TODO: refactor code to handle this asynchronously. Loading CSS properties currently ranges from 470ms-800ms as of 3/25/2019
					Map<String, String> css_props = Browser.loadCssProperties(elem);

					Set<Attribute> attributes = extractAttributes(elem, browser.getDriver());
					
					//TODO: refactor xpath to generation to be faster. Generating xpath can take over 1.6s
					String element_xpath = generateXpath(elem, xpath, xpath_map, browser.getDriver(), attributes);
					
					page_element.setCssValues(css_props);
					page_element.setScreenshot(screenshot);
					page_element.setAttributes(attributes);
					page_element.setXpath(element_xpath);
					
					page_element = page_element_service.save(page_element);
				}
				else{
					page_element = page_element_record;
				}
				
				if(!elementList.contains(page_element)){
					elementList.add(page_element);
				}
			}
		}
		page_screenshot.flush();

		return elementList;
	}
	
	public static boolean isElementVisibleInPane(Browser browser, WebElement elem){
		Object objy = browser.getYScrollOffset();
		Object objx = browser.getXScrollOffset();
		
		int y_offset = 0;
		int x_offset = 0;
		
		if(objy instanceof Double){
			y_offset = ((Double)objy).intValue(); 
		}
		else if(objy instanceof Long){
			y_offset = ((Long)objy).intValue(); 
		}
		
		if(objx instanceof Double){
			x_offset = ((Double)objx).intValue(); 
		}
		else if(objx instanceof Long){
			x_offset = ((Long)objx).intValue(); 
		}
		Point location = elem.getLocation();
		int x = location.getX();
		int y = location.getY();
		
		Dimension dimension = elem.getSize();
		int height = dimension.getHeight();
		int width = dimension.getWidth();
		
		if(x >= x_offset && y >= y_offset && (x+width) <= (browser.getViewportSize().getWidth()+x_offset-DIMENSION_OFFSET_PIXELS) 
				&& (y+height) <= (browser.getViewportSize().getHeight()+y_offset-DIMENSION_OFFSET_PIXELS)){
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
	 * generates a unique xpath for this element.
	 * 
	 * @return an xpath that identifies this element uniquely
	 */
	public String generateXpath(WebElement element, String xpath, Map<String, Integer> xpathHash, WebDriver driver, Set<Attribute> attributes){
		ArrayList<String> attributeChecks = new ArrayList<String>();
		String element_text = element.getText();
		
		xpath += "//"+element.getTagName();
		
		if(!element_text.isEmpty()){
			attributeChecks.add("contains(text(), \"" + element_text + "\")");
		}
		
		String new_xpath = xpath;

		if(attributeChecks.size()>0){
			new_xpath += "[";
			for(int i = 0; i < attributeChecks.size(); i++){
				new_xpath += attributeChecks.get(i).toString();
				if(i < attributeChecks.size()-1){
					new_xpath += " and ";
				}
			}
			new_xpath += "]";
		}
		
		try{
			List<WebElement> element_list = driver.findElements(By.xpath(new_xpath));
			if(element_list.size() == 1){
				xpath = new_xpath;
			}
		}
		catch(NoSuchElementException e){
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
		}

	    WebElement parent = element;
	    String element_name = parent.getTagName();
	    List<WebElement> similar_elements = new ArrayList<>();
	    do{
	    	try{
	    		parent = getParentElement(parent);
	    		element_name = parent.getTagName();
	    		xpath = element_name + xpath;
	    		similar_elements = driver.findElements(By.xpath("//"+xpath));
	    		if(similar_elements.size() == 1){
	    			return "//"+xpath;
	    		}
	    		else{
		    		xpath = "/"+xpath;		
	    		}
	    	}catch(InvalidSelectorException e){
	    		parent = null;
	    		log.error("Invalid selector exception occurred while generating xpath through parent nodes");
	    		break;
	    	}
	    }while(!element_name.equals("body"));

		xpath = "/"+xpath;	    
		xpath = uniqifyXpath(element, xpathHash, xpath, driver);

		return xpath;
	}
	
	/**
	 * Extract all attributes from a given {@link WebElement}
	 * 
	 * @param element {@link WebElement} to have attributes loaded for
	 * @param javascriptDriver - 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Set<Attribute> extractAttributes(WebElement element, WebDriver driver) {
		List<String> attribute_strings = (ArrayList<String>)((JavascriptExecutor)driver).executeScript("var items = []; for (index = 0; index < arguments[0].attributes.length; ++index) { items.push(arguments[0].attributes[index].name + '::' + arguments[0].attributes[index].value) }; return items;", element);
		return loadAttributes(attribute_strings);
	}
	
	/**
	 * Loads attributes for this element into a list of {@link Attribute}s
	 * 
	 * @param attributeList
	 */
	private Set<Attribute> loadAttributes( List<String> attributeList){
		Set<Attribute> attr_set = new HashSet<Attribute>();
		
		Map<String, Boolean> attributes_seen = new HashMap<String, Boolean>();
		
		for(int i = 0; i < attributeList.size(); i++){
			String[] attributes = attributeList.get(i).split("::");
			
			if(attributes.length > 1){
				String attribute_name = attributes[0].trim().replace("\'", "'");
				String[] attributeVals = attributes[1].split(" ");

				if(!attributes_seen.containsKey(attribute_name)){
					attributes_seen.put(attribute_name, true);
					Attribute attribute = new Attribute(attribute_name, Arrays.asList(attributeVals));
					attr_set.add(attribute);	
				}
			}
		}

		return attr_set;
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
	 * creates a unique xpath based on a given hash of xpaths
	 * 
	 * @param elem
	 * @param xpathHash
	 * @param xpath
	 * @param driver
	 * @return
	 */
	public static String uniqifyXpath(WebElement elem, Map<String, Integer> xpathHash, String xpath, WebDriver driver){
		try {
			String elem_text = elem.getText();
			List<WebElement> similar_elements = driver.findElements(By.xpath(xpath));
			if(similar_elements.size()>1){
				int count = 1;
				
				for(WebElement element : similar_elements){
					if(element.getText().equals(elem_text)){
						return "(" + xpath + ")[" + count + "]";	
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
			for(ScreenshotSet screenshot : page.getBrowserScreenshots()){
				log.info("screenshot browser  ::   "+screenshot.getBrowser());
				log.info("browser browsername ::   "+browser.getBrowserName());
				if(screenshot.getBrowser().equals(browser.getBrowserName())){
					page_screenshot = screenshot.getViewportScreenshot();
					break;
				}
			}
			
			String screenshot_url = retrieveAndUploadBrowserScreenshot(browser, form_elem, ImageIO.read(new URL(page_screenshot)));
			PageElement form_tag = new PageElement(form_elem.getText(), uniqifyXpath(form_elem, xpath_map, "//form", browser.getDriver()), "form", extractAttributes(form_elem, browser.getDriver()), Browser.loadCssProperties(form_elem), screenshot_url );
			
			form_tag.setScreenshot(screenshot_url);
			
			double[] weights = new double[1];
			weights[0] = 0.3;
			
			Form form = new Form(form_tag, new ArrayList<PageElement>(), findFormSubmitButton(form_elem, browser), 
									"Form #1", weights, FormType.values(), FormType.UNKNOWN, new Date(), FormStatus.DISCOVERED );
			List<WebElement> input_elements =  form_elem.findElements(By.xpath(form_tag.getXpath() +"//input"));
			for(WebElement input_elem : input_elements){
				Set<Attribute> attributes = extractAttributes(input_elem, browser.getDriver());

				for(ScreenshotSet screenshot : page.getBrowserScreenshots()){
					if(screenshot.getBrowser().equals(browser.getBrowserName())){
						page_screenshot = screenshot.getViewportScreenshot();
						break;
					}
				}
				
				screenshot_url = retrieveAndUploadBrowserScreenshot(browser, form_elem, ImageIO.read(new URL(page_screenshot)));
				PageElement input_tag = new PageElement(input_elem.getText(), generateXpath(input_elem, "", xpath_map, browser.getDriver(), attributes), input_elem.getTagName(), attributes, Browser.loadCssProperties(input_elem), screenshot_url );
												
				if(input_tag == null || input_tag.getScreenshot()== null || input_tag.getScreenshot().isEmpty()){

					Crawler.performAction(new Action("click"), input_tag, browser.getDriver());
					BufferedImage viewport = Browser.getViewportScreenshot(browser.getDriver());
										
					if(input_elem.getLocation().getX() < 0 || input_elem.getLocation().getY() < 0){
						continue;
					}
					BufferedImage img = Browser.getElementScreenshot(viewport, input_elem.getSize(), input_elem.getLocation(), browser);

					String screenshot= null;
					try {
						screenshot = UploadObjectSingleOperation.saveImageToS3(img, (new URL(browser.getDriver().getCurrentUrl())).getHost(), PageState.getFileChecksum(img), input_tag.getKey());
					} catch (Exception e) {
						log.warn("Error retrieving screenshot -- "+e.getLocalizedMessage());
					}

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
				
				List<PageElement> group_inputs = constructGrouping(input_elem, browser);
				
				//Set<PageElement> labels = findLabelsForInputs(form_elem, group_inputs, browser.getDriver());
				/*for(FormField input_field : group_inputs){
					try{
						PageElement label = findLabelForInput(form_elem, input_field, browser.getDriver());
						input_field.setFieldLabel(label);
					}
					catch(NullPointerException e){
						log.info("Error occurred while finding label for form input field");
					}
				}
				*/
				log.info("GROUP INPUTS    :::   "+group_inputs.size());
				for(PageElement page_elem : group_inputs){
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
	public List<PageElement> constructGrouping(WebElement page_elem, Browser browser) throws Exception{

		List<PageElement> child_inputs = new ArrayList<PageElement>();

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
				child_inputs = new ArrayList<PageElement>();

				for(WebElement child : children){
					Set<Attribute> attributes = extractAttributes(child, browser.getDriver());
					String screenshot_url = retrieveAndUploadBrowserScreenshot(browser, child);

					PageElement elem = new PageElement(child.getText(), generateXpath(child, "", new HashMap<String, Integer>(), browser.getDriver(), attributes), child.getTagName(), attributes, Browser.loadCssProperties(child), screenshot_url );
					PageElement elem_record = page_element_service.findByKey(elem.getKey());
					
					if(elem_record != null){
						elem=elem_record;
					}
					else{
						elem = page_element_service.save(elem);
					}
					
					//FormField input_field = new FormField(elem);
					
					child_inputs.add(elem);
				}
				
				page_elem = parent;
			}
			else{
				if(child_inputs.size() == 0){
					Set<Attribute> attributes = extractAttributes(page_elem, browser.getDriver());
					String screenshot_url = retrieveAndUploadBrowserScreenshot(browser, page_elem);

					PageElement input_tag = new PageElement(page_elem.getText(), generateXpath(page_elem, "", new HashMap<String,Integer>(), browser.getDriver(), attributes), page_elem.getTagName(), attributes, Browser.loadCssProperties(page_elem), screenshot_url );
					PageElement elem_record = page_element_service.findByKey(input_tag.getKey());
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
	private PageElement findFormSubmitButton(WebElement form_elem, Browser browser) throws Exception {
		WebElement submit_element = null;
		try{
			submit_element = form_elem.findElement(By.xpath("//button[@type='submit']"));
		}
		catch(NoSuchElementException e){
			submit_element = form_elem.findElement(By.xpath("//input[@type='submit']"));
		}
		Set<Attribute> attributes = extractAttributes(submit_element, browser.getDriver());
		String screenshot_url = retrieveAndUploadBrowserScreenshot(browser, form_elem);
		PageElement elem = new PageElement(submit_element.getText(), generateXpath(submit_element, "", new HashMap<String, Integer>(), browser.getDriver(), attributes), submit_element.getTagName(), attributes, Browser.loadCssProperties(submit_element), screenshot_url );
		PageElement elem_record = page_element_service.findByKey(elem.getKey());
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
			
			img = Browser.getElementScreenshot(Browser.getViewportScreenshot(browser.getDriver()), elem.getSize(), elem.getLocation(), browser);
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
			img = Browser.getElementScreenshot(Browser.getViewportScreenshot(browser.getDriver()), elem.getSize(), elem.getLocation(), browser);
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
		BufferedImage viewport_screenshot = Browser.getViewportScreenshot(browser.getDriver());
		
		ScreenshotSet page_screenshot = null;
		log.info("page state screenshots :: "+page_state.getBrowserScreenshots().size());
		for(ScreenshotSet screenshot : page_state.getBrowserScreenshots()){
			if(screenshot.getBrowser().equals(browser.getBrowserName())){
				log.info("Browser name matches screenshot browser!");
				page_screenshot = screenshot;
			}
		}
		
		boolean pages_match = false;
		try {
			BufferedImage img1 = ImageIO.read(new URL(page_screenshot.getViewportScreenshot()));
			BufferedImage img2 = viewport_screenshot;
			pages_match = PageState.compareImages(img1, img2);
			if(pages_match){
				return true;
			}
			log.info("DO THE SCREENSHOTS MATCH????        ::::     "+pages_match);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		return false;
	}
}
