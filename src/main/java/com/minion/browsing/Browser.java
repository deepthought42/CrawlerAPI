package com.minion.browsing;

import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.InvalidSelectorException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.UnreachableBrowserException;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minion.aws.UploadObjectSingleOperation;
import com.minion.browsing.element.ComplexField;
import com.minion.browsing.form.ElementRuleExtractor;
import com.minion.browsing.form.Form;
import com.minion.browsing.form.FormField;
import com.minion.util.ArrayUtility;
import com.qanairy.models.Page;
import com.qanairy.models.PageElement;

/**
 * Handles the mnanagement of selenium browser instances and provides various methods for interacting with the browser 
 */
public class Browser {
    private static final Logger log = LoggerFactory.getLogger(Browser.class);

	private WebDriver driver;
	private static String[] invalid_xpath_attributes = {"ng-view", "ng-include", "ng-repeat","ontouchstart", "ng-click", "ng-class", "onload", "lang", "xml:lang", "xmlns", "xmlns:fb", "onsubmit", "webdriver",/*Wordpress generated field*/"data-blogger-escaped-onclick", "src", "alt", "scale", "title", "name","data-analytics","onmousedown", "data-rank", "data-domain", "data-url", "data-subreddit", "data-fullname", "data-type", "onclick", "data-outbound-expiration", "data-outbound-url", "rel", "onmouseover","height","width","onmouseout", "data-cid","data-imp-pixel"};	

	public Browser(String url) throws MalformedURLException {
		this.driver = openWithChrome(url);
		
		//this.driver = openWithFirefox(url);
		//this.driver = openWithPhantomjs(url);
		this.driver.get(url);
	}
	
	/**
	 * @return current {@link WebDriver driver}
	 */
	public WebDriver getDriver(){
		return this.driver;
	}

	/**
	 * 
	 * @return
	 * @throws IOException 
	 * @throws MalformedURLException 
	 */
	public Page getPage() throws MalformedURLException, IOException{
		return new Page(this.driver.getPageSource(), this.driver.getCurrentUrl(), Browser.getScreenshot(this.driver), Browser.getVisibleElements(this.driver, ""));

	}
	
	/**
	 * Removes canvas element added by Selenium when taking screenshots
	 * 
	 * @param src
	 * @return
	 */
	public static String cleanSrc(String src){
		src = src.replaceAll("\\s", "");
		
		Pattern p = Pattern.compile("<canvas id=\"fxdriver-screenshot-canvas\" style=\"display: none;\" width=\"([0-9]*)\" height=\"([0-9]*)\"></canvas>",
	            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
	    return p.matcher(src).replaceAll("");
	}
	
	/**
	 * Closes the browser opened by the current driver.
	 */
	public void close(){
		try{
			driver.quit();
		}
		catch(NullPointerException e){
			log.error("Error closing driver. Driver is NULL");
		}
		catch(UnreachableBrowserException e){
			log.error("Error closing driver");
		}
	}
	
	/**
	 * 
	 * @param url
	 */
	public void getUrl(String url){
		try{
			this.driver.get(url);
		}
		catch(UnhandledAlertException exc){
			AcceptAlert(driver, new WebDriverWait(driver, 5));
		}
	}
	
	/**
	 * open new firefox browser
	 * 
	 * @param url
	 * @return firefox web driver
	 * @throws MalformedURLException 
	 */
	public static WebDriver openWithFirefox(String url) throws MalformedURLException{
		System.setProperty("webdriver.gecko.driver", "C:\\Users\\brand\\Dev\\geckodriver-v0.9.0-win64\\geckodriver.exe");

		log.info("Opening Firefox WebDriver connection using URL : " +url);
		//FirefoxProfile firefoxProfile = new FirefoxProfile();
	    DesiredCapabilities capabilities = DesiredCapabilities.firefox();

	    //capabilities.setBrowserName("firefox");
	    //capabilities.setPlatform(Platform.LINUX);
	    //capabilities.setVersion("3.6");
		WebDriver driver = new FirefoxDriver(capabilities);
		//WebDriver driver = new RemoteWebDriver(new URL("http://localhost:4444"), capabilities);
		log.info("firefox opened");
		return driver;
	}
	
	/**
	 * open new Safari browser window
	 * 
	 * @param url
	 * @return safari web driver
	 */
	public static WebDriver openWithSafari(String url){
		log.info("Opening Firefox WebDriver connection using URL : " +url);
		//FirefoxProfile firefoxProfile = new FirefoxProfile();
	    DesiredCapabilities capabilities = DesiredCapabilities.safari();

		WebDriver driver = new SafariDriver(capabilities);
		log.info("Safari opened");
		return driver;
	}
	
	/**
	 * Opens internet explorer browser window
	 * 
	 * @param url
	 * @return internet explorer web driver
	 */
	public static WebDriver openWithInternetExplorer(String url){
		System.setProperty("webdriver.gecko.driver", "C:\\Users\\brand\\Dev\\geckodriver-v0.9.0-win64\\geckodriver.exe");

		log.info("Opening Safari WebDriver connection using URL : " +url);
	    DesiredCapabilities capabilities = DesiredCapabilities.internetExplorer();

		WebDriver driver = new InternetExplorerDriver(capabilities);
		log.info("Internet Explorer opened");
		return driver;
	}
	
	/**
	 * open new Chrome browser window
	 * 
	 * @param url
	 * @return Chrome web driver
	 * @throws MalformedURLException 
	 */
	public static WebDriver openWithChrome(String url) throws MalformedURLException{
		System.setProperty("webdriver.chrome.driver", "C:\\Users\\brand\\Dev\\browser_drivers\\chromedriver_win32\\chromedriver.exe");

		log.info("Opening Chrome WebDriver connection using URL : " +url);
		//FirefoxProfile firefoxProfile = new FirefoxProfile();
	    DesiredCapabilities capabilities = DesiredCapabilities.chrome();
	    //capabilities.setBrowserName("chrome");
	    //capabilities.setPlatform(Platform.LINUX);
	    WebDriver driver = new ChromeDriver(capabilities);
		//WebDriver driver = new RemoteWebDriver(new URL(url), capabilities);

		log.info("Chrome opened");
		return driver;
	}
	
	/**
	 * open new firefox browser
	 * 
	 * @param url
	 * @return
	 */
	public static PhantomJSDriver openWithPhantomjs(String url){
		System.setProperty("webdriver.chrome.driver", "C:\\Users\\brand\\Dev\\browser_drivers\\chromedriver_win32\\chromedriver.exe");

		log.info("Opening Phantomjs WebDriver Connection using URL : "+url);
	    //Create instance of PhantomJS driver
	    DesiredCapabilities capabilities = DesiredCapabilities.phantomjs();
	    PhantomJSDriver driver = new PhantomJSDriver(capabilities);
		return driver;
	}
	
	/**
	 * Accepts alert
	 * 
	 * @param driver
	 * @param wait
	 */
	public static void AcceptAlert(WebDriver driver, WebDriverWait wait) {
	    if (wait == null) {
	        wait = new WebDriverWait(driver, 5);
	    }
	    try{
	        Alert alert = wait.until(new ExpectedCondition<Alert>(){
				public Alert apply(WebDriver driver) {
	                try {
	                  return driver.switchTo().alert();
	                } catch (NoAlertPresentException e) {
	                  return null;
	                }
	              }
	            }
	        );
	        alert.accept();
	    }
	    catch(TimeoutException e){}
	}
	
	/**
	 * Gets image as a base 64 string
	 * 
	 * @return File png file of image
	 * @throws IOException
	 */
	public static File getScreenshot(WebDriver driver) throws IOException{
		File screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
		
		return screenshot;
	}
	 
	/**
	 * Get immediate child elements for a given element
	 * @param elem	WebElement to get children for
	 * @return list of WebElements
	 */
	public static List<WebElement> getChildElements(WebElement elem) throws WebDriverException{
		return elem.findElements(By.xpath("./*"));
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
	public static List<PageElement> getVisibleElements(WebDriver driver, String xpath) 
															 throws WebDriverException {
		
		List<WebElement> pageElements = driver.findElements(By.cssSelector("*"));
		//TO MAKE BETTER TIME ON THIS PIECE IT WOULD BE BETTER TO PARALELLIZE THIS PART
		ArrayList<PageElement> elementList = new ArrayList<PageElement>();
		if(pageElements.size() == 0){
			return elementList;
		}
		
		Map<String, Integer> xpath_map = new HashMap<String, Integer>();
		for(WebElement elem : pageElements){
			try{
				//log.info("checking visibily and extracting attributes for element " + counter++);
				Date start = new Date();
				if(elem.isDisplayed() && (elem.getAttribute("backface-visibility")==null || !elem.getAttribute("backface-visiblity").equals("hidden"))){
					String this_xpath = Browser.generateXpath(elem, "", xpath_map, driver); 
					//PageElement pageElem = new PageElement(ActionFactory.getActions(), new PageElement(elem, this_xpath, "", loadAttributes(Browser.extractedAttributes(elem, (JavascriptExecutor)driver))));
					PageElement tag = new PageElement(elem.getText(), this_xpath, elem.getTagName(), Browser.extractedAttributes(elem, (JavascriptExecutor)driver));
					try{
						//tag.setScreenshot(Browser.capturePageElementScreenshot(elem, tag, driver));
						elementList.add(tag);
					}
					catch(RasterFormatException e){
						//e.printStackTrace();
					}

				}
				
				Date end = new Date();
				double execution_time = (end.getTime() - start.getTime())/1000.0;
				if( execution_time > 1.0){
					log.debug("All attributes extracted in " + execution_time + " seconds");
				}
			}catch(StaleElementReferenceException e){
				log.error(e.toString());
			}
			catch(RasterFormatException e){
				log.error(e.toString());
			}
		}
		
		return elementList;
	}
	
	/**
	 * Extracts all forms including the child inputs and associated labels. 
	 * 
	 * @param elem
	 * @param tag
	 * @param driver
	 * @return
	 */
	public static List<Form> extractAllForms(Page page, Browser browser){
		//Document doc = Jsoup.parse(page.getSrc());

		Map<String, Integer> xpath_map = new HashMap<String, Integer>();

		List<Form> form_list = new ArrayList<Form>();
		List<WebElement> form_elements = browser.getDriver().findElements(By.xpath("//form"));
		log.info("total forms found :: " + form_elements.size());
		for(WebElement form_elem : form_elements){
			List<String> form_xpath_list = new ArrayList<String>();
				PageElement form_tag = new PageElement(form_elem.getText(), generateXpath(form_elem, "", xpath_map, browser.getDriver()), "form", Browser.extractedAttributes(form_elem, (JavascriptExecutor)browser.getDriver()));
				Form form = new Form(form_tag, new ArrayList<ComplexField>(), browser.findFormSubmitButton(form_elem) );
				List<WebElement> input_elements =  form_elem.findElements(By.xpath(form_tag.getXpath() +"//input"));
				
				List<PageElement> input_tags = new ArrayList<PageElement>(); 
				for(WebElement input_elem : input_elements){
					PageElement input_tag = new PageElement(input_elem.getText(), generateXpath(input_elem, "", xpath_map, browser.getDriver()), input_elem.getTagName(), Browser.extractedAttributes(input_elem, (JavascriptExecutor)browser.getDriver()));
					
					boolean alreadySeen = false;
					for(String path : form_xpath_list){
						if(path.equals(input_tag.getXpath())){
							alreadySeen = true;
						}
					}
					
					if(alreadySeen){
						continue;
					}						
					
					List<FormField> group_inputs = constructGrouping(input_elem, browser.getDriver());
					ComplexField combo_input = new ComplexField(group_inputs);
					
					//List<PageElement> labels = findLabelsForInputs(form_elem, group_inputs, browser.getDriver());
					for(FormField input_field : group_inputs){
						PageElement label = findLabelForInput(form_elem, input_field, browser.getDriver());
						input_field.setFieldLabel(label);
					}
					//combo_input.getElements().addAll(labels);
					form.addFormField(combo_input);
					for(FormField input : group_inputs){
						input.addRules(ElementRuleExtractor.extractRules(input.getInputElement()));
						//combo_rules.addAll(ElementRuleExtractor.extractRules(input.getInputElement()));
					}
					//log.info("Form combo field has a total of " + combo_rules.size() + " rules");
					input_tags.add(input_tag);
				}
				
				log.info("Total inputs for form : "+form.getFormFields().size());
				
				form_list.add(form);
				log.info(form.getType() + " : Form discovered");
		}
		return form_list;
	}
	
	private PageElement findFormSubmitButton(WebElement form_elem) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * 
	 */
	public static PageElement findLabelForInput(WebElement form_elem, FormField input_field, WebDriver driver){
		List<WebElement> label_elements = form_elem.findElements(By.xpath(".//label"));
		//get all ids for current inputs
		List<String> input_ids = new ArrayList<String>();
		input_ids.add(input_field.getInputElement().getAttribute("id").getVals()[0]);
		
		List<PageElement> label_tags = new ArrayList<PageElement>();
		for(WebElement label_elem : label_elements){
			//check if input for attribute references an existing id on any of the current child_inputs
			for(String id : input_ids){
				log.info("checking labels for id association");
				log.info(label_elem.getAttribute("for") + " == " + id);

				if(label_elem.getAttribute("for").equals(id)){
					PageElement label_tag = new PageElement(label_elem.getText(), generateXpath(label_elem, "", new HashMap<String, Integer>(), driver), label_elem.getTagName(), Browser.extractedAttributes(label_elem, (JavascriptExecutor)driver));
					return label_tag;
				}
			}
		}
		
		log.info("Total labels added : "+label_tags.size() + " :: Total ids : "+input_ids.size());
		
		return null;
	}
	
	/**
	 * 
	 */
	public static List<PageElement> findLabelsForInputs(WebElement form_elem, List<FormField> group_inputs, WebDriver driver){
		List<WebElement> label_elements = form_elem.findElements(By.xpath(".//label"));
		//get all ids for current inputs
		List<String> input_ids = new ArrayList<String>();
		for(FormField input : group_inputs){
			input_ids.add(input.getInputElement().getAttribute("id").getVals()[0]);
		}
		
		List<PageElement> label_tags = new ArrayList<PageElement>();
		for(WebElement label_elem : label_elements){
			//check if input for attribute references an existing id on any of the current child_inputs
			for(String id : input_ids){
				log.info("checking labels for id association");
				log.info(label_elem.getAttribute("for") + " == " + id);

				if(label_elem.getAttribute("for").equals(id)){
					PageElement label_tag = new PageElement(label_elem.getText(), generateXpath(label_elem, "", new HashMap<String, Integer>(), driver), label_elem.getTagName(), Browser.extractedAttributes(label_elem, (JavascriptExecutor)driver));
					label_tags.add(label_tag);
					break;
				}
			}
		}
		
		log.info("Total labels added : "+label_tags.size() + " :: Total ids : "+input_ids.size());
		
		return label_tags;
	}
	
	/**
	 * Finds all other inputs that are grouped with this one by observing each parent of a {@link WebElement} until it 
	 *   finds a parent which has inputs with a different type than the provided {@link WebElement} 
	 *   
	 * @param page_elem
	 * @param driver
	 * @return
	 */
	public static List<FormField> constructGrouping(WebElement page_elem, WebDriver driver){

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
					PageElement elem = new PageElement(child.getText(), Browser.generateXpath(child, "", new HashMap<String, Integer>(), driver), child.getTagName(), Browser.extractedAttributes(child, (JavascriptExecutor)driver));
					FormField input_field = new FormField(elem);
					child_inputs.add(input_field);
				}
				
				page_elem = parent;
				log.info("All Children match and have been loaded into list");
			}
			else{
				if(child_inputs.size() == 0){
					PageElement input_tag = new PageElement(page_elem.getText(), generateXpath(page_elem, page_elem.getTagName(), new HashMap<String,Integer>(), driver), page_elem.getTagName(), Browser.extractedAttributes(page_elem, (JavascriptExecutor)driver));
					FormField input_field = new FormField(input_tag);
					child_inputs.add(input_field);
				}
			}
			log.info("Total children discovered in current loop ... " + child_inputs.size());

		}while(allChildrenMatch);
		
		return child_inputs;
	}
	
	public static List<Form> extractAllSelectOptions(Page page, WebDriver driver){
		return null;
	}
	
	/**
	 * Extracts all form input fields
	 * 
	 * @param page
	 * @param driver
	 * @return
	 */
	public static List<PageElement> extractAllInputElements(Page page, WebDriver driver){
		List<PageElement> choices = new ArrayList<PageElement>();
		log.info("Searching elements for radio/checkbox inputs : "+page.getElements().size());
		for(PageElement tag : page.getElements()){
			//PageElement tag = (PageElement)elem;
			log.info("Exampining tag element");
			if(tag.getName().equalsIgnoreCase("input")){
				//List<Attribute> attr_list = tag.getAttributes();
				log.info("loaded attribute list ");
				Attribute attr = tag.getAttribute("type");
				if(attr != null){
					for(String attr_val : attr.getVals()){
						if(attr_val.equalsIgnoreCase("checkbox")){
							/*CheckboxField field = new CheckboxField(tag);
							log.info("identified checkbox :: "+tag.getText());
							
							String[] id_vals = tag.getAttributeValues("id");
							
							//get label 
							PageElement label = findLabelFor(page.getElements(), id_vals );
							
							field.setLabel(label);
							*/
							choices.add(tag);
							break;
						}
						else if(attr_val.equalsIgnoreCase("radio")){
							/*RadioField field = new RadioField(tag);
							log.info("identified radio");
							
							String[] id_vals = tag.getAttributeValues("id");
							
							//get label 
							PageElement label = findLabelFor(page.getElements(), id_vals );
							field.setRadio(tag);
							field.setRadio_label(label);
							//attempt to identify label
							 * 
							 */
							choices.add(tag);
							break;
						}
						else if(attr_val.equalsIgnoreCase("text")){
							log.info("text input encountered");
						}
						else if(attr_val.equalsIgnoreCase("textarea")){
							log.info("text area input encountered");
						}
						else if(attr_val.equalsIgnoreCase("color")){
							log.info("color input encountered");
	
						}
						else if(attr_val.equalsIgnoreCase("email")){
							log.info("email input encountered");
	
						}
						else if(attr_val.equalsIgnoreCase("file")){
							log.info("file input encountered");
	
						}
						else if(attr_val.equalsIgnoreCase("image")){
							log.info("image input button encountered");
	
						}
						else if(attr_val.equalsIgnoreCase("month")){
							log.info("month and year input encountered");
	
						}
						else if(attr_val.equalsIgnoreCase("number")){
							log.info("number input encountered");
	
						}
						else if(attr_val.equalsIgnoreCase("password")){
							log.info("password input encountered");
	
						}
						else if(attr_val.equalsIgnoreCase("range")){
							log.info("range input encountered");
	
						}
						else if(attr_val.equalsIgnoreCase("reset")){
							log.info("reset input encountered");
	
						}
						else if(attr_val.equalsIgnoreCase("search")){
							log.info("search input encountered");
		
						}
						else if(attr_val.equalsIgnoreCase("submit")){
							log.info("submit input encountered");
	
						}
						else if(attr_val.equalsIgnoreCase("tel")){
							log.info("telephone input encountered");
	
						}
						else if(attr_val.equalsIgnoreCase("time")){
							log.info("time input encountered");
	
						}
						else if(attr_val.equalsIgnoreCase("url")){
							log.info("url input encountered");
	
						}
						else if(attr_val.equalsIgnoreCase("week")){
							log.info("week input encountered");
	
						}
					}
				}
			}
		}
		
		return choices;
	}
	
	public static PageElement findLabelFor(List<PageElement> elements, String for_id){
		for(PageElement elem : elements){
			//PageElement tag = (PageElement)elem;
			if(elem.getName().equals("label") ){
				for(Attribute attr : elem.getAttributes()){
					log.info("Getting Attributes for label");
					if(attr.getName().equals("for")){
						for(String val : attr.getVals()){
							if(val.equals(for_id)){
								log.info("LABEL FOUND FOR : " + for_id);
								return elem;
							}
						}
					}
				}
			
			}
		}
		
		return null;
	}
	
	/**
	 * Finds labels that match ids passed
	 * @param elements
	 * @param for_ids
	 * @return
	 */
	public static List<PageElement> findLabelsFor(List<PageElement> elements, String[] for_ids){
		List<PageElement> labels = new ArrayList<PageElement>();
		for(PageElement elem : elements){
			//PageElement tag = (PageElement)elem;
			if(elem.getName().equals("label") ){
				for(Attribute attr : elem.getAttributes()){
					log.info("Getting Attributes for label");
					if(attr.getName().equals("for")){
						
						for(String val : attr.getVals()){
							for(String id : for_ids){
								if(val.equals(id)){
									log.info("LABEL FOUND FOR : " + id);
									labels.add(elem);
								}
							}
						}
					}
				}
			}
		}
		
		return labels;
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
		if(elem.findElements(By.xpath(xpath)).size() <= 1){
			return xpath;
		}
		else{
			int count = 1;
			if(xpathHash.containsKey(xpath)){
				count = xpathHash.get(xpath);
				count += 1;
			}
			xpathHash.put(xpath, count);
			xpath = xpath+"[" + count + "]";
		}
		return xpath;
	}

	/**
	 * generates a unique xpath for this element.
	 * 
	 * @return an xpath that identifies this element uniquely
	 */
	public static String generateXpath(WebElement element, String xpath, Map<String, Integer> xpathHash, WebDriver driver){
		ArrayList<String> attributeChecks = new ArrayList<String>();
		
		xpath += "//"+element.getTagName();
		for(Attribute attr : Browser.extractedAttributes(element, (JavascriptExecutor)driver)){
			if(!Arrays.asList(invalid_xpath_attributes).contains(attr.getName())){
				attributeChecks.add("contains(@" + attr.getName() + ",\"" + ArrayUtility.joinArray(attr.getVals()) + "\")");
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
				e.printStackTrace();
				break;
			}
		}
		
		xpath = uniqifyXpath(element, xpathHash, xpath);
		return xpath;
	}
	
	/**
	 * Loads attributes for this element into a list of {@link Attribute}s
	 * 
	 * @param attributeList
	 */
	public static List<Attribute> loadAttributes( List<String> attributeList){
		List<Attribute> attr_lst = new ArrayList<Attribute>();
		
		for(int i = 0; i < attributeList.size(); i++){
			String[] attributes = attributeList.get(i).split("::");
			String[] attributeVals;
			if(attributes.length > 1){
				attributeVals = attributes[1].split(" ");
			}
			else{
				attributeVals = new String[0];
			}
			
			attr_lst.add(new Attribute(attributes[0].trim().replace("\'", "'"), attributeVals));
		}
		 return attr_lst;
	}
	
	/**
	 * Extract all attributes from a given {@link WebElement}
	 * 
	 * @param element {@link WebElement} to have attributes loaded for
	 * @param javascriptDriver - 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static List<Attribute> extractedAttributes(WebElement element, JavascriptExecutor javascriptDriver) {
		List<String> attribute_strings = (ArrayList<String>)javascriptDriver.executeScript("var items = []; for (index = 0; index < arguments[0].attributes.length; ++index) { items.push(arguments[0].attributes[index].name + '::' + arguments[0].attributes[index].value) }; return items;", element);
		return loadAttributes(attribute_strings);
	}
	
	
	/**
	 * Captures current page screenshot
	 * 
	 * @param ele
	 * @param page_elem
	 * @param driver
	 * @return
	 * @throws RasterFormatException
	 */
	public static String capturePageElementScreenshot(WebElement ele, PageElement tag, WebDriver driver) throws RasterFormatException{
		// Process the objectData stream.
		BufferedImage fullImg;
		try {
			fullImg = ImageIO.read(Browser.getScreenshot(driver));
			// Get the location of element on the page
			Point point = ele.getLocation();

			// Get width and height of the element
			int eleWidth = ele.getSize().getWidth();
			int eleHeight = ele.getSize().getHeight();
			
			// Crop the entire page screenshot to get only element screenshot
			String elem_screenshot = null;
			BufferedImage eleScreenshot= fullImg.getSubimage(point.getX(), point.getY(),
			    eleWidth, eleHeight);
		    File outputfile = new File(tag.getKey().replace(":", "")+".png");
			ImageIO.write(eleScreenshot, "png", outputfile);

			elem_screenshot = UploadObjectSingleOperation.saveImageToS3(outputfile, driver.getCurrentUrl()+"/webelements", tag.getXpath());
			outputfile.delete();
			return elem_screenshot;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}
