package com.qanairy.services;

import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.openqa.grid.common.exception.GridException;
import org.openqa.selenium.By;
import org.openqa.selenium.InvalidSelectorException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.minion.api.MessageBroadcaster;
import com.minion.aws.UploadObjectSingleOperation;
import com.minion.browsing.Browser;
import com.minion.browsing.Crawler;
import com.minion.browsing.element.ComplexField;
import com.minion.browsing.form.ElementRuleExtractor;
import com.minion.browsing.form.Form;
import com.minion.browsing.form.FormField;
import com.minion.util.ArrayUtility;
import com.qanairy.models.Action;
import com.qanairy.models.Attribute;
import com.qanairy.models.PageElement;
import com.qanairy.models.PageState;
import com.qanairy.models.ScreenshotSet;
import com.qanairy.models.repository.AttributeRepository;
import com.qanairy.models.repository.PageElementRepository;
import com.qanairy.models.repository.PageStateRepository;
import com.qanairy.models.repository.ScreenshotSetRepository;
import com.qanairy.models.rules.Rule;

/**
 * 
 * 
 */
@Component
public class BrowserService {
	private static Logger log = LoggerFactory.getLogger(BrowserService.class);

	@Autowired
	private PageStateRepository page_state_repo;
	
	@Autowired
	private PageElementRepository page_element_repo;
	
	@Autowired
	private AttributeRepository attribute_repo;
	
	@Autowired
	private ScreenshotSetRepository screenshot_set_repo;
	
	private static String[] valid_xpath_attributes = {"class", "id", "name", "title"};	

	/**
	 * 
	 * @return
	 * @throws GridException 
	 * @throws IOException 
	 */
	public PageState buildPage(Browser browser) throws GridException, IOException{
		URL page_url = new URL(browser.getDriver().getCurrentUrl());
		String src = browser.getDriver().getPageSource();

		Set<PageElement> visible_elements = new HashSet<PageElement>();
		String viewport_screenshot_url = null;
		String src_hash = org.apache.commons.codec.digest.DigestUtils.sha256Hex(browser.getDriver().getPageSource());
		try{
			File viewport_screenshot = Browser.getViewportScreenshot(browser.getDriver());
			System.err.println("Uploading screenshot to S3");
			viewport_screenshot_url = UploadObjectSingleOperation.saveImageToS3(ImageIO.read(viewport_screenshot), page_url.getHost(), src_hash, "viewport");
			visible_elements = getVisibleElements(browser.getDriver(), "", ImageIO.read(viewport_screenshot), page_url.getHost());
		}catch(IOException e){
			log.error(e.getMessage());
		}
	
		if(visible_elements == null){
			visible_elements = new HashSet<PageElement>();
		}
		ScreenshotSet screenshot_set = new ScreenshotSet(viewport_screenshot_url, browser.getBrowserName());
		ScreenshotSet screenshot_record = screenshot_set_repo.findByKey(screenshot_set.getKey());
		if(screenshot_record != null){
			screenshot_set = screenshot_record;
		}
		else{
			screenshot_set = screenshot_set_repo.save(screenshot_set);
		}
		Set<ScreenshotSet> browser_screenshot = new HashSet<ScreenshotSet>();
		browser_screenshot.add(screenshot_set);
		
		PageState page_state = new PageState(src,
				page_url.toString(),
				browser_screenshot,
				visible_elements);
		PageState page_record = page_state_repo.findByKey(page_state.getKey());
		
		if(page_record != null){
			page_record.setBrowserScreenshots(browser_screenshot);
			page_record.setElements(visible_elements);
			page_state = page_record;
		}
		else{
			MessageBroadcaster.broadcastPageState(page_state, page_url.getHost().toString());
		}
		
		return page_state_repo.save(page_state);
	}

	/**
	 * Retreives all elements on a given page that are visible. In this instance we take 
	 *  visible to mean that it is not currently set to {@css display: none} and that it
	 *  is visible within the confines of the screen. If an element is not hidden but is also 
	 *  outside of the bounds of the screen it is assumed hidden
	 *  
	 * @param driver
	 * @return list of webelements that are currently visible on the page
	 */
	private Set<PageElement> getVisibleElements(WebDriver driver, String xpath, BufferedImage page_screenshot, String host) 
															 throws WebDriverException{
		
		List<WebElement> pageElements = driver.findElements(By.cssSelector("*"));

		Set<PageElement> elementList = new HashSet<PageElement>();
		if(pageElements.size() == 0){
			return elementList;
		}
		
		Map<String, Integer> xpath_map = new HashMap<String, Integer>();
		for(WebElement elem : pageElements){
			
			try{
				boolean is_child = getChildElements(elem).isEmpty();
				
				if(is_child && elem.getSize().getHeight() > 0 && elem.isDisplayed() 
						&& !elem.getTagName().equals("body") && !elem.getTagName().equals("html")){
					//(elem.getAttribute("backface-visibility")==null || !elem.getAttribute("backface-visiblity").equals("hidden"))

					String this_xpath = generateXpath(elem, xpath, xpath_map, driver); 
					
					try{
						PageElement tag = new PageElement(elem.getText(), this_xpath, elem.getTagName(), extractAttributes(elem, driver), Browser.loadCssProperties(elem) );
	
						BufferedImage img = Browser.getElementScreenshot(page_screenshot, elem.getSize(), elem.getLocation());
						String screenshot = UploadObjectSingleOperation.saveImageToS3(img, (new URL(driver.getCurrentUrl())).getHost(), org.apache.commons.codec.digest.DigestUtils.sha256Hex(driver.getPageSource())+"/"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(elem.getTagName()+elem.getText()), tag.getKey());	
						
						PageElement tag_record = page_element_repo.findByKey(tag.getKey());
						if(tag_record != null){
							tag = tag_record;
							tag.setScreenshot(screenshot);
							tag = page_element_repo.save(tag);
						}
						else{
							tag.setScreenshot(screenshot);
							tag = page_element_repo.save(tag);
							MessageBroadcaster.broadcastPageElement(tag, host);
						}
						
						
						elementList.add(tag);
					}
					catch(RasterFormatException e){
						//System.err.println("Raster Format Exception : "+e.getMessage());
					}

				}
			}catch(StaleElementReferenceException e){
				log.error(e.getMessage());
			}
			catch(RasterFormatException e){
				log.error(e.getMessage());
			}
			catch(GridException e){
				log.error(e.getMessage());
			} 
			catch (IOException e) {
				log.error(e.getMessage());
			}
		}
		
		return elementList;
	}
	
	public boolean isElementVisibleInPane(WebElement elem, int panel_width, int panel_height){
		int x = elem.getLocation().getX();
		int y = elem.getLocation().getY();
		int height = elem.getSize().getHeight();
		int width = elem.getSize().getWidth();
		
		if(x >= 0 && y >= 0 && (x+width) <= panel_width && (y+height) <= panel_height){
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
	public static WebElement getParentElement(WebElement elem) throws WebDriverException{
		return elem.findElement(By.xpath(".."));
	}
	
	/**
	 * generates a unique xpath for this element.
	 * 
	 * @return an xpath that identifies this element uniquely
	 */
	public String generateXpath(WebElement element, String xpath, Map<String, Integer> xpathHash, WebDriver driver){
		ArrayList<String> attributeChecks = new ArrayList<String>();
		
		xpath += "//"+element.getTagName();
		for(Attribute attr : extractAttributes(element, driver)){
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
		
		WebElement parent = null;
		while(!element.getTagName().equals("html")){
			try{
				parent = element.findElement(By.xpath(".."));
				if(parent == null){
					break;
				}
				xpath = "/" + parent.getTagName() + xpath;
				element = parent;
			}catch(InvalidSelectorException e){
				parent = null;
				log.error("Invalid selector exception occurred while generating xpath through parent nodes");
				break;
			}
		}
		
		xpath = uniqifyXpath(element, xpathHash, xpath);
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
					
					Attribute attribute_record = attribute_repo.findByKey(attribute.getKey());
					if(attribute_record != null){
						attribute = attribute_record;
					}
					else{
						attribute = attribute_repo.save(attribute);
					}
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
                System.err.println("search str length ::  "+searchString.length());
                System.err.println("quote position :: " + quotePos);
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
	 * @param driver
	 * @param xpathHash
	 * 
	 * @return
	 */
	public static String uniqifyXpath(WebElement elem, Map<String, Integer> xpathHash, String xpath){
		try {
			List<WebElement> elements = elem.findElements(By.xpath(xpath));
			
			if(elements.size()>1){
				int count = 1;
				if(xpathHash.containsKey(xpath)){
					count = xpathHash.get(xpath);
					count += 1;
				}
				xpathHash.put(xpath, count);
				xpath = "("+xpath+")[" + count + "]";
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
	 * @throws IOException 
	 */
	public List<Form> extractAllForms(PageState page, Browser browser) throws IOException{
		Map<String, Integer> xpath_map = new HashMap<String, Integer>();
		List<Form> form_list = new ArrayList<Form>();
		
		List<WebElement> form_elements = browser.getDriver().findElements(By.xpath("//form"));
		for(WebElement form_elem : form_elements){
			List<String> form_xpath_list = new ArrayList<String>();
			PageElement form_tag = new PageElement(form_elem.getText(), uniqifyXpath(form_elem, xpath_map, "//form"), "form", extractAttributes(form_elem, browser.getDriver()), Browser.loadCssProperties(form_elem) );
			Form form = new Form(form_tag, new ArrayList<ComplexField>(), findFormSubmitButton(form_elem, browser) );
			List<WebElement> input_elements =  form_elem.findElements(By.xpath(form_tag.getXpath() +"//input"));

			Set<PageElement> input_tags = new HashSet<PageElement>(); 
			for(WebElement input_elem : input_elements){
				PageElement input_tag = new PageElement(input_elem.getText(), generateXpath(input_elem, "", xpath_map, browser.getDriver()), input_elem.getTagName(), extractAttributes(input_elem, browser.getDriver()), Browser.loadCssProperties(input_elem) );
				Crawler.performAction(new Action("click"), input_tag, browser.getDriver());
				
				//System.err.println("Screenshot url :: "+screenshot_sub);
			/*	File viewport = Browser.getViewportScreenshot(browser.getDriver());
				BufferedImage img = Browser.getElementScreenshot(ImageIO.read(viewport), input_elem.getSize(), input_elem.getLocation());
				String screenshot = UploadObjectSingleOperation.saveImageToS3(img, (new URL(browser.getDriver().getCurrentUrl())).getHost(), org.apache.commons.codec.digest.DigestUtils.sha256Hex(browser.getDriver().getPageSource())+"/"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(input_elem.getTagName()+input_elem.getText()), input_tag.getKey());	
				*/
				input_tag.setScreenshot("missing_elem_screenshot.jpg");
				
				System.err.println("########  !!!!!!!!!!!!!!  DONT FORGET TO FIX SCREENSHOTS FOR FORM TEST ELEMENTS !!!!!!!!!!!!!  ######");

				System.err.println("Input tag text :: "+(input_tag.getText()==null));
				System.err.println("Input tag text :: "+input_tag.getText());
				System.err.println("Input tag xpath :: "+input_tag.getXpath());
				System.err.println("Input tag name ::  "+input_tag.getName());
				System.err.println("Input tag attributes size :: " + input_tag.getAttributes().size());
				System.err.println("INput tag css values size :: " + input_tag.getCssValues().size());
				System.err.println("Input tag rules size :: " + input_tag.getRules().size());
				System.err.println("Input tag screenshot :: "+input_tag.getScreenshot());
				
				boolean alreadySeen = false;
				for(String xpath : form_xpath_list){
					if(xpath.equals(input_tag.getXpath())){
						alreadySeen = true;
					}
				}
				
				if(alreadySeen){
					System.err.println("page element already seen before extracting form elements");
					continue;
				}						
				
				List<FormField> group_inputs = constructGrouping(input_elem, browser.getDriver());
				ComplexField combo_input = new ComplexField(group_inputs);
				
				//Set<PageElement> labels = findLabelsForInputs(form_elem, group_inputs, browser.getDriver());
				/*for(FormField input_field : group_inputs){
					try{
						PageElement label = findLabelForInput(form_elem, input_field, browser.getDriver());
						input_field.setFieldLabel(label);
					}
					catch(NullPointerException e){
						System.err.println("Error occurred while finding label for form input field");
					}
				}
				*/
				System.err.println("GROUP INPUTS    :::   "+group_inputs.size());
				for(FormField input_field : group_inputs){
					
					for(Rule rule : ElementRuleExtractor.extractInputRules(input_field.getInputElement())){
						input_field.getInputElement().addRule(rule);
					}
				}
				//combo_input.getElements().addAll(labels);
				form.addFormField(combo_input);
				input_tags.add(input_tag);
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
	 */
	public List<FormField> constructGrouping(WebElement page_elem, WebDriver driver){

		List<FormField> child_inputs = new ArrayList<FormField>();

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
				e.printStackTrace();
				break;
			}
			if(allChildrenMatch){
				//create list with new elements
				List<WebElement> children = parent.findElements(By.xpath(".//input"));
				child_inputs = new ArrayList<FormField>();

				for(WebElement child : children){
					PageElement elem = new PageElement(child.getText(), generateXpath(child, "", new HashMap<String, Integer>(), driver), child.getTagName(), extractAttributes(child, driver), Browser.loadCssProperties(child) );
					FormField input_field = new FormField(elem);
					child_inputs.add(input_field);
				}
				
				page_elem = parent;
			}
			else{
				if(child_inputs.size() == 0){
					PageElement input_tag = new PageElement(page_elem.getText(), generateXpath(page_elem, "", new HashMap<String,Integer>(), driver), page_elem.getTagName(), extractAttributes(page_elem, driver), Browser.loadCssProperties(page_elem) );
					FormField input_field = new FormField(input_tag);
					child_inputs.add(input_field);
				}
			}
		}while(allChildrenMatch);
		
		return child_inputs;
	}
	
	/**
	 * locates and returns the form submit button 
	 * @param form_elem
	 * @return
	 */
	private PageElement findFormSubmitButton(WebElement form_elem, Browser browser) {
		WebElement submit_element = form_elem.findElement(By.xpath("//button[@type='submit']"));
		return new PageElement(submit_element.getText(), generateXpath(submit_element, "", new HashMap<String, Integer>(), browser.getDriver()), submit_element.getTagName(), extractAttributes(submit_element, browser.getDriver()), Browser.loadCssProperties(submit_element) );
	}
	
	/**
	 * 
	 * @param browser_name
	 * @param page_state
	 * @return
	 */
	public boolean checkIfLandable(String browser_name, PageState page_state) {
		boolean landable = false;
		boolean page_visited_successfully = true;
		do{
			try{
				Browser browser = new Browser(browser_name);
				browser.getDriver().get(page_state.getUrl().toString());
				try{
					new WebDriverWait(browser.getDriver(), 360).until(
							webDriver -> ((JavascriptExecutor) webDriver).executeScript("return document.readyState").equals("complete"));
				}catch(GridException e){
					log.error(e.getMessage());
				}
				catch(Exception e){
					log.error(e.getMessage());
				}
				
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {}
				
				if(page_state.equals(buildPage(browser))){
					landable = true;
				}
				browser.close();
				break;
			}catch(Exception e){
				page_visited_successfully = false;
				//e.printStackTrace();
				log.error("ERROR VISITING PAGE AT ::: "+page_state.getUrl().toString());
			}
		}while(!page_visited_successfully);
		
		return landable;
	}
}
