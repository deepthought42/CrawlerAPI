package com.minion.browsing;

import java.awt.image.RasterFormatException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;
import org.openqa.grid.common.exception.GridException;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.InvalidSelectorException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.UnreachableBrowserException;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import com.minion.aws.UploadObjectSingleOperation;
import com.minion.browsing.element.ComplexField;
import com.minion.browsing.form.ElementRuleExtractor;
import com.minion.browsing.form.Form;
import com.minion.browsing.form.FormField;
import com.minion.util.ArrayUtility;
import com.qanairy.models.Attribute;
import com.qanairy.models.Page;
import com.qanairy.models.PageElement;

/**
 * Handles the management of selenium browser instances and provides various methods for interacting with the browser 
 */
public class Browser {
	private static Logger log = LoggerFactory.getLogger(Browser.class);

	private WebDriver driver = null;
	private static String[] invalid_xpath_attributes = {"ng-view", "ng-include", "ng-repeat","ontouchstart", "ng-click", "ng-class", "onload", "lang", "xml:lang", "xmlns", "xmlns:fb", "@xmlns:cc", "onsubmit", "webdriver",/*Wordpress generated field*/"data-blogger-escaped-onclick", "src", "alt", "scale", "title", "name","data-analytics","onmousedown", "data-rank", "data-domain", "data-url", "data-subreddit", "data-fullname", "data-type", "onclick", "data-outbound-expiration", "data-outbound-url", "rel", "onmouseover","height","width","onmouseout", "data-cid","data-imp-pixel", "value", "placeholder", "data-wow-duration", "data-wow-offset", "data-wow-delay", "required", "xlink:href"};	
	private static String[] valid_elements = {"div", "span", "ul", "li", "a", "img", "button", "input", "form", "i", "canvas", "h1", "h2", "h3", "h4", "h5", "h6", "datalist", "label", "nav", "option", "ol", "p", "select", "table", "tbody", "td", "textarea", "th", "thead", "tr", "video", "audio", "track"};
	private String url = "";
	private String browser_name; 
    //private static final String HUB_IP_ADDRESS= "165.227.120.79";
    private static final String HUB_IP_ADDRESS= "104.131.30.168";

	/**
	 * 
	 * @param url
	 * @param browser  the name of the browser to use
	 * 			chrome = google chrome
	 * 			firefox = Firefox
	 * 			ie = internet explorer
	 * 			safari = safari
	 * @throws MalformedURLException
	 */
	public Browser(String url, String browser) throws MalformedURLException, NullPointerException {
		int cnt = 0;
		this.setBrowserName(browser);
		while(driver == null && cnt < 20){
			log.info("Opening "+ browser +" browser attempt #"+ cnt);
			try{
				if(browser.equals("chrome")){
					this.driver = openWithChrome();
				}
				else if(browser.equals("firefox")){
					this.driver = openWithFirefox();
				}
				else if(browser.equals("internet_explorer")){
					this.driver = openWithInternetExplorer();
				}
				else if(browser.equals("safari")){
					this.driver = openWithSafari();
				}
				else if(browser.equals("opera")){
					this.driver = openWithOpera();
				}
				WebDriverWait wait = new WebDriverWait(driver, 120);
				wait.until( webDriver -> ((JavascriptExecutor) webDriver).executeScript("return document.readyState").equals("complete"));
				break;
			}
			catch(UnreachableBrowserException e){
				log.error(e.getMessage());
			}
			catch(WebDriverException e){
				log.error(e.getMessage());
			}
			catch(GridException e){
				log.error(e.getMessage());
			}
			
			cnt++;

			try {
				Thread.sleep(300000);
			} catch (InterruptedException e1) {}
		}
		
		if(this.driver != null){			
			System.err.println("Driver isn't null! Getting url "+url);
			this.url = url;
			this.driver.get(url);
		}
		else{
			throw new NullPointerException();
		}
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
		URL page_url = new URL(url);
		String screenshot = "";
		String src = null;
		List<PageElement> visible_elements = null;
		for(int i=0; i<10; i++){
			try{
				src = this.driver.getPageSource();
				visible_elements = Browser.getVisibleElements(this.driver, "");
				screenshot = UploadObjectSingleOperation.saveImageToS3(Browser.getScreenshot(this.driver), page_url.getHost(), org.apache.commons.codec.digest.DigestUtils.sha256Hex(this.driver.getPageSource()));
				break;
			}catch(Exception e){
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {}
			}
		}
		
		if(visible_elements == null){
			visible_elements = new ArrayList<PageElement>();
		}
		
		Map<String, String> browser_screenshot = new HashMap<String, String>();
		browser_screenshot.put(browser_name, screenshot);
		return new Page(src, 
						url,
						browser_screenshot, 
						visible_elements);
	}
	
	/**
	 * Removes canvas element added by Selenium when taking screenshots
	 * 
	 * @param src
	 * @return
	 * 
	 * @precondition src != null
	 */
	public static String cleanSrc(String src) throws NullPointerException{
		assert src != null;
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
			//log.error("Error closing driver. Driver is NULL");
		}
		catch(UnreachableBrowserException e){
			log.error(e.getMessage());
		}
		catch(NoSuchSessionException e){
			log.error(e.getMessage());			
		}
		catch(GridException e){
			log.error(e.getMessage());
		}
	}
	
	/**
	 * open new firefox browser
	 * 
	 * @param url
	 * @return firefox web driver
	 * @throws MalformedURLException 
	 */
	public static WebDriver openWithFirefox() throws MalformedURLException, UnreachableBrowserException, GridException{
		String node = "http://"+HUB_IP_ADDRESS+":4444/wd/hub";
	    DesiredCapabilities cap = DesiredCapabilities.firefox();
	    cap.setBrowserName("firefox");
		cap.setJavascriptEnabled(true);

	    RemoteWebDriver driver = new RemoteWebDriver(new URL(node), cap);
	    // Puts an Implicit wait, Will wait for 10 seconds before throwing exception
	    driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
	    
		return driver;
	}

	/**
	 * open new opera browser
	 * 
	 * @param url
	 * @return Opera web driver
	 * @throws MalformedURLException 
	 */
	public static WebDriver openWithOpera() throws MalformedURLException, UnreachableBrowserException, GridException{
		String node = "http://"+HUB_IP_ADDRESS+":4444/wd/hub";
	    DesiredCapabilities cap = DesiredCapabilities.opera();
	    cap.setBrowserName("opera");
		cap.setJavascriptEnabled(true);

	    RemoteWebDriver driver = new RemoteWebDriver(new URL(node), cap);
	    // Puts an Implicit wait, Will wait for 10 seconds before throwing exception
	    driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
	    
		return driver;
	}
	
	/**
	 * open new Safari browser window
	 * 
	 * @param url
	 * @return safari web driver
	 */
	public static WebDriver openWithSafari() throws MalformedURLException, UnreachableBrowserException, GridException{
		String node = "http://"+HUB_IP_ADDRESS+":4444/wd/hub";

		log.info("Opening Firefox WebDriver connection using");
	    DesiredCapabilities cap = DesiredCapabilities.safari();

		RemoteWebDriver driver = new RemoteWebDriver(new URL(node), cap);
	    driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);

		return driver;
	}
	
	/**
	 * Opens internet explorer browser window
	 * 
	 * @param url
	 * @return internet explorer web driver
	 */
	public static WebDriver openWithInternetExplorer() throws MalformedURLException, UnreachableBrowserException, GridException {
		String node = "http://"+HUB_IP_ADDRESS+":4444/wd/hub";

		log.info("Opening IE WebDriver connection");
	    DesiredCapabilities capabilities = DesiredCapabilities.internetExplorer();

		RemoteWebDriver driver = new RemoteWebDriver(new URL(node), capabilities);
		
		return driver;
	}
	
	/**
	 * open new Chrome browser window
	 * 
	 * @param url
	 * @return Chrome web driver
	 * @throws MalformedURLException 
	 */
	public static WebDriver openWithChrome() 
			throws MalformedURLException, UnreachableBrowserException, WebDriverException, GridException {
		DesiredCapabilities cap = DesiredCapabilities.chrome();
		cap.setJavascriptEnabled(true);
		//cap.setCapability("screenshot", true);
		//cap.setPlatform(Platform.LINUX);
		//cap.setCapability("maxInstances", 5);
		// optional video recording
		/*String record_video = "True";
		// video record
		if (record_video.equalsIgnoreCase("True")) {
			cap.setCapability("video", "True"); // NOTE: "True" is a case sensitive string, not boolean.
		} else {
			cap.setCapability("video", "False"); // NOTE: "False" is a case sensitive string, not boolean.
		}*/
        String hub_node_url = "http://"+HUB_IP_ADDRESS+":4444/wd/hub";
		RemoteWebDriver driver = new RemoteWebDriver(new URL(hub_node_url), cap);
	    // Puts an Implicit wait, Will wait for 10 seconds before throwing exception
	    //driver.manage().timeouts().implicitlyWait(60, TimeUnit.SECONDS);
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
	public static File getScreenshot(WebDriver driver) throws IOException, GridException{
		/*try{
			driver.manage().window().maximize();
		}catch(Exception e){}
		*/
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
															 throws WebDriverException{
		
		List<WebElement> pageElements = driver.findElements(By.cssSelector("*"));

		ArrayList<PageElement> elementList = new ArrayList<PageElement>();
		if(pageElements.size() == 0){
			return elementList;
		}
		
		Map<String, Integer> xpath_map = new HashMap<String, Integer>();
		//log.info("Total elements on page :: "+pageElements.size() + "; with url "+driver.getCurrentUrl());
		for(WebElement elem : pageElements){
			
			try{
				if(elem.isDisplayed() && (elem.getAttribute("backface-visibility")==null || !elem.getAttribute("backface-visiblity").equals("hidden"))
						&& !elem.getTagName().equals("body") && !elem.getTagName().equals("html")){
					String this_xpath = Browser.generateXpath(elem, xpath, xpath_map, driver); 
					PageElement tag = new PageElement(elem.getText(), this_xpath, elem.getTagName(), Browser.extractedAttributes(elem, (JavascriptExecutor)driver), PageElement.loadCssProperties(elem) );
					try{
						//tag.setScreenshot(Browser.capturePageElementScreenshot(elem, tag, driver));
						elementList.add(tag);
					}
					catch(Exception e){
						log.error(e.getMessage());
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
		}
		log.debug("Total elements that are visible on page :: "
					+ elementList.size() + "; with url "+driver.getCurrentUrl());

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
			PageElement form_tag = new PageElement(form_elem.getText(), uniqifyXpath(form_elem, xpath_map, "//form"), "form", Browser.extractedAttributes(form_elem, (JavascriptExecutor)browser.getDriver()), PageElement.loadCssProperties(form_elem) );
			Form form = new Form(form_tag, new ArrayList<ComplexField>(), browser.findFormSubmitButton(form_elem) );
			log.info("FORM PATH : " + form_tag.getXpath());
			List<WebElement> input_elements =  form_elem.findElements(By.xpath(form_tag.getXpath() +"//input"));
			log.info("total form elements found :: " + input_elements.size());

			List<PageElement> input_tags = new ArrayList<PageElement>(); 
			for(WebElement input_elem : input_elements){
				log.info("building form element input :: " + input_elem.getTagName());
				PageElement input_tag = new PageElement(input_elem.getText(), generateXpath(input_elem, "", xpath_map, browser.getDriver()), input_elem.getTagName(), Browser.extractedAttributes(input_elem, (JavascriptExecutor)browser.getDriver()), PageElement.loadCssProperties(input_elem) );
				
				boolean alreadySeen = false;
				for(String xpath : form_xpath_list){
					if(xpath.equals(input_tag.getXpath())){
						alreadySeen = true;
					}
				}
				
				if(alreadySeen){
					continue;
				}						
				
				List<FormField> group_inputs = constructGrouping(input_elem, browser.getDriver());
				ComplexField combo_input = new ComplexField(group_inputs);
				
				//List<PageElement> labels = findLabelsForInputs(form_elem, group_inputs, browser.getDriver());
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
				
				for(FormField input_field : group_inputs){
					input_field.getInputElement().addRules(ElementRuleExtractor.extractInputRules(input_field.getInputElement()));
				}
				//combo_input.getElements().addAll(labels);
				form.addFormField(combo_input);
				input_tags.add(input_tag);
			}
			
			log.info("Total inputs for form : "+form.getFormFields().size());
			
			form_list.add(form);
		}
		return form_list;
	}
	
	/**
	 * locates and returns the form submit button 
	 * @param form_elem
	 * @return
	 */
	private PageElement findFormSubmitButton(WebElement form_elem) {
		WebElement submit_element = form_elem.findElement(By.xpath("//button[@type='submit']"));
		return new PageElement(submit_element.getText(), generateXpath(submit_element, "", new HashMap<String, Integer>(), driver), submit_element.getTagName(), Browser.extractedAttributes(submit_element, (JavascriptExecutor)driver), PageElement.loadCssProperties(submit_element) );
	}

	/**
	 * 
	 */
	public static PageElement findLabelForInput(WebElement form_elem, FormField input_field, WebDriver driver) throws NullPointerException{
		List<WebElement> label_elements = form_elem.findElements(By.xpath(".//label"));
		//get all ids for current inputs
		List<String> input_ids = new ArrayList<String>();
		input_ids.add(input_field.getInputElement().getAttribute("id").getVals().get(0));
		
		List<PageElement> label_tags = new ArrayList<PageElement>();
		for(WebElement label_elem : label_elements){
			//check if input for attribute references an existing id on any of the current child_inputs
			for(String id : input_ids){
				log.info("checking labels for id association");
				log.info(label_elem.getAttribute("for") + " == " + id);

				if(label_elem.getAttribute("for").equals(id)){
					PageElement label_tag = new PageElement(label_elem.getText(), generateXpath(label_elem, "", new HashMap<String, Integer>(), driver), label_elem.getTagName(), Browser.extractedAttributes(label_elem, (JavascriptExecutor)driver), PageElement.loadCssProperties(label_elem) );
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
			input_ids.add(input.getInputElement().getAttribute("id").getVals().get(0));
		}
		
		List<PageElement> label_tags = new ArrayList<PageElement>();
		for(WebElement label_elem : label_elements){
			//check if input for attribute references an existing id on any of the current child_inputs
			for(String id : input_ids){
				log.info("checking labels for id association");
				log.info(label_elem.getAttribute("for") + " == " + id);

				if(label_elem.getAttribute("for").equals(id)){
					PageElement label_tag = new PageElement(label_elem.getText(), generateXpath(label_elem, "", new HashMap<String, Integer>(), driver), label_elem.getTagName(), Browser.extractedAttributes(label_elem, (JavascriptExecutor)driver), PageElement.loadCssProperties(label_elem) );
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
					PageElement elem = new PageElement(child.getText(), Browser.generateXpath(child, "", new HashMap<String, Integer>(), driver), child.getTagName(), Browser.extractedAttributes(child, (JavascriptExecutor)driver), PageElement.loadCssProperties(child) );
					FormField input_field = new FormField(elem);
					child_inputs.add(input_field);
				}
				
				page_elem = parent;
			}
			else{
				if(child_inputs.size() == 0){
					PageElement input_tag = new PageElement(page_elem.getText(), generateXpath(page_elem, "", new HashMap<String,Integer>(), driver), page_elem.getTagName(), Browser.extractedAttributes(page_elem, (JavascriptExecutor)driver), PageElement.loadCssProperties(page_elem) );
					FormField input_field = new FormField(input_tag);
					child_inputs.add(input_field);
				}
			}
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
			log.info("Examining tag element");
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
							choices.add(tag);
						}
						else if(attr_val.equalsIgnoreCase("textarea")){
							log.info("text area input encountered");
							choices.add(tag);
						}
						else if(attr_val.equalsIgnoreCase("color")){
							log.info("color input encountered");
							choices.add(tag);
	
						}
						else if(attr_val.equalsIgnoreCase("email")){
							log.info("email input encountered");
							choices.add(tag);
						}
						else if(attr_val.equalsIgnoreCase("file")){
							log.info("file input encountered");
							choices.add(tag);
						}
						else if(attr_val.equalsIgnoreCase("image")){
							log.info("image input button encountered");
							choices.add(tag);
						}
						else if(attr_val.equalsIgnoreCase("month")){
							log.info("month and year input encountered");
							choices.add(tag);
						}
						else if(attr_val.equalsIgnoreCase("number")){
							log.info("number input encountered");
							choices.add(tag);
						}
						else if(attr_val.equalsIgnoreCase("password")){
							log.info("password input encountered");
							choices.add(tag);
						}
						else if(attr_val.equalsIgnoreCase("range")){
							log.info("range input encountered");
							choices.add(tag);
						}
						else if(attr_val.equalsIgnoreCase("reset")){
							log.info("reset input encountered");
							choices.add(tag);
						}
						else if(attr_val.equalsIgnoreCase("search")){
							log.info("search input encountered");
							choices.add(tag);
						}
						else if(attr_val.equalsIgnoreCase("submit")){
							log.info("submit input encountered");
							choices.add(tag);
						}
						else if(attr_val.equalsIgnoreCase("tel")){
							log.info("telephone input encountered");
							choices.add(tag);
						}
						else if(attr_val.equalsIgnoreCase("time")){
							log.info("time input encountered");
							choices.add(tag);
						}
						else if(attr_val.equalsIgnoreCase("url")){
							log.info("url input encountered");
							choices.add(tag);
						}
						else if(attr_val.equalsIgnoreCase("week")){
							log.info("week input encountered");
							choices.add(tag);
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
		try {
			List<WebElement> elements = elem.findElements(By.xpath(xpath));
			
			if(elements.size()>1){
				int count = 1;
				if(xpathHash.containsKey(xpath)){
					count = xpathHash.get(xpath);
					count += 1;
				}
				xpathHash.put(xpath, count);
				xpath = xpath+"[" + count + "]";
			}
			
		}catch(InvalidSelectorException e){
			log.error(e.getMessage());
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
				attributeChecks.add("contains(@" + attr.getName() + ",\"" + ArrayUtility.joinArray(attr.getVals().toArray(new String[attr.getVals().size()])).trim() + "\")");
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
			
			attr_lst.add(new Attribute(attributes[0].trim().replace("\'", "'"), Arrays.asList(attributeVals)));
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

	public String getBrowserName() {
		return browser_name;
	}

	public void setBrowserName(String browser_name) {
		this.browser_name = browser_name;
	}
}
