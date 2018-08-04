package com.qanairy.services;

import static com.qanairy.config.SpringExtension.SpringExtProvider;

import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.openqa.grid.common.exception.GridException;
import org.openqa.selenium.By;
import org.openqa.selenium.InvalidSelectorException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.minion.actors.AwsS3ScreenshotUploader.ElementScreenshotUpload;
import com.minion.actors.LandabilityChecker.BrowserPageState;
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
import com.qanairy.models.FormRecord;
import com.qanairy.models.PageElement;
import com.qanairy.models.PageState;
import com.qanairy.models.ScreenshotSet;
import com.qanairy.models.repository.AttributeRepository;
import com.qanairy.models.repository.FormRecordRepository;
import com.qanairy.models.repository.PageElementRepository;
import com.qanairy.models.repository.PageStateRepository;
import com.qanairy.models.repository.ScreenshotSetRepository;
import com.qanairy.models.rules.Rule;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

/**
 * 
 * 
 */
@Component
public class BrowserService {
	private static Logger log = LoggerFactory.getLogger(BrowserService.class);
	
	@Autowired
	private ActorSystem actor_system;
	
	@Autowired
	private PageStateRepository page_state_repo;
	
	@Autowired
	private PageElementRepository page_element_repo;
	
	@Autowired
	private ScreenshotSetRepository screenshot_set_repo;
	
	@Autowired
	private AttributeRepository attribute_repo;
	
	@Autowired
	private FormRecordRepository form_record_repo;
	
	private static String[] valid_xpath_attributes = {"class", "id", "name", "title"};	

	/**
	 * 
	 * @param browser_name
	 * @param page_state
	 * @return
	 * @throws IOException 
	 * @throws GridException 
	 */
	public boolean checkIfLandable(Browser browser, PageState page_state) throws GridException, IOException {
		boolean landable = false;

		boolean page_visited_successfully = false;
		int cnt  = 0;
		do{
			page_visited_successfully = false;

			try{
				Browser landable_browser = new Browser(browser.getBrowserName());
				landable_browser.navigateTo(page_state.getUrl());
				page_visited_successfully = true;
				if(page_state.equals(buildPage(landable_browser))){
					landable= true;
				}
				landable_browser.close();

			}catch(GridException e){
				log.error(e.getMessage());
			}
			catch(Exception e){
				log.error("ERROR VISITING PAGE AT ::: "+page_state.getUrl().toString());
				log.error(e.getMessage());
			}
			cnt++;
		}while(!page_visited_successfully && cnt < 3);
		
		System.err.println("is page state landable  ?? :: "+landable);
		return landable;
	}
	
	/**
	 * 
	 * @return
	 * @throws GridException 
	 * @throws IOException 
	 */
	public PageState buildPage(Browser browser) throws GridException, IOException{
		URL page_url = new URL(browser.getDriver().getCurrentUrl());
		String page_key = "";
		Set<PageElement> visible_elements = new HashSet<PageElement>();
		String viewport_screenshot_url = null;
		File viewport_screenshot = null;
		try{
			viewport_screenshot = Browser.getViewportScreenshot(browser.getDriver());
			page_key = PageState.getFileChecksum(ImageIO.read(viewport_screenshot)); 
			System.err.println("Getting visible elements...");
			visible_elements = getVisibleElements(browser.getDriver(), "", ImageIO.read(viewport_screenshot), page_url.getHost());
		}catch(IOException e){
			log.error(e.getMessage());
		}
	
		if(visible_elements == null){
			visible_elements = new HashSet<PageElement>();
		}

		viewport_screenshot_url = UploadObjectSingleOperation.saveImageToS3(ImageIO.read(viewport_screenshot), page_url.getHost(), page_key, "viewport");
		ScreenshotSet screenshot_set = new ScreenshotSet(viewport_screenshot_url, browser.getBrowserName());
		
		ScreenshotSet screenshot_record = screenshot_set_repo.findByKey(screenshot_set.getKey());
		if(screenshot_record != null){
			screenshot_set = screenshot_record;
		}
		else{
			screenshot_set = screenshot_set_repo.save(screenshot_set);
		}
		
		HashSet<ScreenshotSet> screenshots = new HashSet<ScreenshotSet>();
		screenshots.add(screenshot_set);
		
		
		PageState page_record = page_state_repo.findByKey(page_key.toLowerCase());
		PageState page_state = null;
		if(page_record != null){
			page_state = page_record;
		}
		else{
			page_state = new PageState("",
					page_url.toString(),
					screenshots,
					visible_elements);
			
			//page_state.setLandable(checkIfLandable(browser, page_state));
			System.err.println("Page state is new. Checking landability");
			//have page checked for landability
			BrowserPageState bps = new BrowserPageState(page_state, browser.getBrowserName());

			final ActorRef landibility_checker = actor_system.actorOf(SpringExtProvider.get(actor_system)
					  .props("landabilityChecker"), "landability_checker"+UUID.randomUUID());
			landibility_checker.tell(bps, ActorRef.noSender() );
		}
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
						&& !elem.getTagName().equals("body") && !elem.getTagName().equals("html") 
						&& !elem.getTagName().equals("script") && !elem.getTagName().equals("link")){
					
					BufferedImage img = null;
					String checksum = "";
					try{
						img = Browser.getElementScreenshot(page_screenshot, elem.getSize(), elem.getLocation());
						checksum = PageState.getFileChecksum(img);		
					}
					catch(RasterFormatException e){
						log.warn("Raster Format Exception : "+e.getMessage());
						continue;
					}
					
					Set<Attribute> attributes = extractAttributes(elem, driver);
					Map<String, String> css_props = Browser.loadCssProperties(elem);
					String this_xpath = generateXpath(elem, xpath, xpath_map, driver, attributes);
					String screenshot = UploadObjectSingleOperation.saveImageToS3(img, (new URL(driver.getCurrentUrl())).getHost(), checksum, "element_screenshot");	

					PageElement tag = new PageElement(elem.getText(), this_xpath, elem.getTagName(), attributes,  css_props, screenshot);
					PageElement tag_record = page_element_repo.findByKey(tag.getKey());

					if(tag_record == null){					
						tag_record = page_element_repo.save(tag);
					}
					
					if(!elementList.contains(tag_record)){
						elementList.add(tag_record);
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
	    		log.error("Invalid selector exception occurred while generating xpath through parent nodes");
	    		break;
	    	}
	    	count++;
	    }
	    xpath = "/"+xpath;
		return uniqifyXpath(element, xpathHash, xpath, driver);
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
	public static String uniqifyXpath(WebElement elem, Map<String, Integer> xpathHash, String xpath, WebDriver driver){
		try {
			List<WebElement> elements = driver.findElements(By.xpath(xpath));
			
			if(elements.size()>1){
				int count = 1;
				for(WebElement element : elements){
					if(element.getTagName().equals(elem.getTagName())
							&& element.getText().equals(elem.getText())){
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
	 * @throws IOException 
	 */
	public List<Form> extractAllForms(PageState page, Browser browser) throws IOException{
		Map<String, Integer> xpath_map = new HashMap<String, Integer>();
		List<Form> form_list = new ArrayList<Form>();

		List<WebElement> form_elements = browser.getDriver().findElements(By.xpath("//form"));
		for(WebElement form_elem : form_elements){
			List<String> form_xpath_list = new ArrayList<String>();
			
			System.err.println("EXTACTED FORM ELEMENT WITH TEXT   : "+form_elem.getText());

			String page_screenshot = "";
			for(ScreenshotSet screenshot : page.getBrowserScreenshots()){
				if(screenshot.getBrowser().equals(browser.getBrowserName())){
					page_screenshot = screenshot.getViewportScreenshot();
				}
			}
			String screenshot_url = retrieveAndUploadBrowserScreenshot(browser.getDriver(), form_elem, ImageIO.read(new URL(page_screenshot)));
			PageElement form_tag = new PageElement(form_elem.getText(), uniqifyXpath(form_elem, xpath_map, "//form", browser.getDriver()), "form", extractAttributes(form_elem, browser.getDriver()), Browser.loadCssProperties(form_elem), screenshot_url );
			
			Form form = new Form(form_tag, new ArrayList<ComplexField>(), findFormSubmitButton(form_elem, browser) );
			List<WebElement> input_elements =  form_elem.findElements(By.xpath(form_tag.getXpath() +"//input"));

			Set<PageElement> input_tags = new HashSet<PageElement>(); 
			for(WebElement input_elem : input_elements){
				Set<Attribute> attributes = extractAttributes(input_elem, browser.getDriver());
				
				for(ScreenshotSet screenshot : page.getBrowserScreenshots()){
					if(screenshot.getBrowser().equals(browser.getBrowserName())){
						page_screenshot = screenshot.getViewportScreenshot();
					}
				}
				
				screenshot_url = retrieveAndUploadBrowserScreenshot(browser.getDriver(), form_elem, ImageIO.read(new URL(page_screenshot)));
				PageElement input_tag = new PageElement(input_elem.getText(), generateXpath(input_elem, "", xpath_map, browser.getDriver(), attributes), input_elem.getTagName(), attributes, Browser.loadCssProperties(input_elem), screenshot_url );
				
  				PageElement elem_record = page_element_repo.findByKey(input_tag.getKey());				
								
				if(elem_record == null || elem_record.getScreenshot()== null || elem_record.getScreenshot().isEmpty()){

					Crawler.performAction(new Action("click"), input_tag, browser.getDriver());
					File viewport = Browser.getViewportScreenshot(browser.getDriver());
										
					if(input_elem.getLocation().getX() < 0 || input_elem.getLocation().getY() < 0){
						continue;
					}
					BufferedImage img = Browser.getElementScreenshot(ImageIO.read(viewport), input_elem.getSize(), input_elem.getLocation(), browser.getDriver());
					String screenshot = UploadObjectSingleOperation.saveImageToS3(img, (new URL(browser.getDriver().getCurrentUrl())).getHost(), org.apache.commons.codec.digest.DigestUtils.sha256Hex(browser.getDriver().getPageSource())+"/"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(input_elem.getTagName()+input_elem.getText()), input_tag.getKey());
									
					if(elem_record == null){
						input_tag.setScreenshot(screenshot);
						elem_record = page_element_repo.save(input_tag);
					}
					else{
						elem_record.setScreenshot(screenshot);
						elem_record = page_element_repo.save(elem_record); 
					}
				}

				boolean alreadySeen = false;
				for(String xpath : form_xpath_list){
					if(xpath.equals(elem_record.getXpath())){
						alreadySeen = true;
					}
				}
				
				if(alreadySeen){
					//System.err.println("page element already seen before extracting form elements");
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
			FormRecord form_record = new FormRecord(form_elem.getAttribute("innerHTML"), input_tags , page_screenshot ,page);
			form_record = form_record_repo.save(form_record);
			
			MessageBroadcaster.broadcastFormRecord(form_record, new URL(browser.getDriver().getCurrentUrl()).getHost());
			
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
					Set<Attribute> attributes = extractAttributes(child, driver);
					String screenshot_url = retrieveAndUploadBrowserScreenshot(driver, child);

					PageElement elem = new PageElement(child.getText(), generateXpath(child, "", new HashMap<String, Integer>(), driver, attributes), child.getTagName(), attributes, Browser.loadCssProperties(child), screenshot_url );
					FormField input_field = new FormField(elem);
					child_inputs.add(input_field);
				}
				
				page_elem = parent;
			}
			else{
				if(child_inputs.size() == 0){
					Set<Attribute> attributes = extractAttributes(page_elem, driver);
					String screenshot_url = retrieveAndUploadBrowserScreenshot(driver, page_elem);

					PageElement input_tag = new PageElement(page_elem.getText(), generateXpath(page_elem, "", new HashMap<String,Integer>(), driver, attributes), page_elem.getTagName(), attributes, Browser.loadCssProperties(page_elem), screenshot_url );
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
		Set<Attribute> attributes = extractAttributes(submit_element, browser.getDriver());
		String screenshot_url = retrieveAndUploadBrowserScreenshot(browser.getDriver(), form_elem);
		return new PageElement(submit_element.getText(), generateXpath(submit_element, "", new HashMap<String, Integer>(), browser.getDriver(), attributes), submit_element.getTagName(), attributes, Browser.loadCssProperties(submit_element), screenshot_url );
	}
	
	public String retrieveAndUploadBrowserScreenshot(WebDriver driver, WebElement elem){
		BufferedImage img = null;
		String checksum = "";
		String screenshot_url = "";
		try{
			img = Browser.getElementScreenshot(ImageIO.read(Browser.getViewportScreenshot(driver)), elem.getSize(), elem.getLocation());
			checksum = PageState.getFileChecksum(img);		
			screenshot_url = UploadObjectSingleOperation.saveImageToS3(img, (new URL(driver.getCurrentUrl())).getHost(), checksum);	

		}
		catch(RasterFormatException e){
			log.warn("Raster Format Exception : "+e.getMessage());
			e.printStackTrace();
		} catch (GridException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return screenshot_url;
	}
	
	public String retrieveAndUploadBrowserScreenshot(WebDriver driver, WebElement elem, BufferedImage page_img){
		BufferedImage img = null;
		String checksum = "";
		String screenshot_url = "";
		try{
			img = Browser.getElementScreenshot(ImageIO.read(Browser.getViewportScreenshot(driver)), elem.getSize(), elem.getLocation());
			checksum = PageState.getFileChecksum(img);		
			screenshot_url = UploadObjectSingleOperation.saveImageToS3(img, (new URL(driver.getCurrentUrl())).getHost(), checksum);	

		}
		catch(RasterFormatException e){
			log.warn("Raster Format Exception : "+e.getMessage());
			e.printStackTrace();
		} catch (GridException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return screenshot_url;
	}
	
	public boolean doScreenshotsMatch(Browser browser, PageState page_state) throws GridException, IOException{
		File viewport_screenshot = Browser.getViewportScreenshot(browser.getDriver());
		
		ScreenshotSet page_screenshot = null;
		System.err.println("page state screenshots :: "+page_state.getBrowserScreenshots().size());
		for(ScreenshotSet screenshot : page_state.getBrowserScreenshots()){
			if(screenshot.getBrowser().equals(browser.getBrowserName())){
				System.err.println("Browser name matches screenshot browser!");
				page_screenshot = screenshot;
			}
		}
		
		boolean pages_match = false;
		try {
			BufferedImage img1 = ImageIO.read(new URL(page_screenshot.getViewportScreenshot()));
			BufferedImage img2 = ImageIO.read(viewport_screenshot);
			pages_match = PageState.compareImages(img1, img2);
			if(pages_match){
				return true;
			}
			System.err.println("DO THE SCREENSHOTS MATCH????        ::::     "+pages_match);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		return false;
	}
}
