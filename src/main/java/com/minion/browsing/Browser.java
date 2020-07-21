package com.minion.browsing;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import javax.imageio.ImageIO;
import javax.net.ssl.HttpsURLConnection;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.css.sac.InputSource;
import org.w3c.dom.Node;
import org.w3c.dom.css.CSSRule;
import org.w3c.dom.css.CSSRuleList;
import org.w3c.dom.css.CSSStyleDeclaration;
import org.w3c.dom.css.CSSStyleSheet;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.grid.common.exception.GridException;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.UnreachableBrowserException;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.assertthat.selenium_shutterbug.core.Shutterbug;
import com.assertthat.selenium_shutterbug.utils.web.ScrollStrategy;
import com.qanairy.models.Form;
import com.qanairy.models.PageAlert;
import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.enums.AlertChoice;
import com.steadystate.css.parser.CSSOMParser;
import com.steadystate.css.parser.SACParserCSS3;

import cz.vutbr.web.css.CSSException;
import cz.vutbr.web.css.CSSFactory;
import cz.vutbr.web.css.CombinedSelector;
import cz.vutbr.web.css.Declaration;
import cz.vutbr.web.css.MediaSpec;
import cz.vutbr.web.css.MediaSpecAll;
import cz.vutbr.web.css.NodeData;
import cz.vutbr.web.css.RuleSet;
import cz.vutbr.web.css.StyleSheet;
import cz.vutbr.web.csskit.RuleFontFaceImpl;
import cz.vutbr.web.csskit.RuleKeyframesImpl;
import cz.vutbr.web.csskit.RuleMediaImpl;
import cz.vutbr.web.domassign.StyleMap;

/**
 * Handles the management of selenium browser instances and provides various methods for interacting with the browser 
 */
@Component
public class Browser {
	
	private static Logger log = LoggerFactory.getLogger(Browser.class);
	private WebDriver driver = null;
	private String browser_name; 
	private long y_scroll_offset;
	private long x_scroll_offset;
	private Dimension viewport_size;
	private static final String JS_GET_VIEWPORT_WIDTH = "var width = undefined; if (window.innerWidth) {width = window.innerWidth;} else if (document.documentElement && document.documentElement.clientWidth) {width = document.documentElement.clientWidth;} else { var b = document.getElementsByTagName('body')[0]; if (b.clientWidth) {width = b.clientWidth;}};return width;";
	private static final String JS_GET_VIEWPORT_HEIGHT = "var height = undefined;  if (window.innerHeight) {height = window.innerHeight;}  else if (document.documentElement && document.documentElement.clientHeight) {height = document.documentElement.clientHeight;}  else { var b = document.getElementsByTagName('body')[0]; if (b.clientHeight) {height = b.clientHeight;}};return height;";
	
    public Browser(){}

	/**
	 * 
	 * @param url
	 * @param browser  the name of the browser to use
	 * 			chrome = google chrome
	 * 			firefox = Firefox
	 * 			ie = internet explorer
	 * 			safari = safari
	 * 
	 * @throws MalformedURLException
	 * @throws ApiException 
	 * 
	 * @pre url != null
	 * @pre browser != null
	 */
	public Browser(String browser, URL hub_node_url) throws MalformedURLException {
		assert browser != null;
		
		//create proxy server connection for handling browserup proxy calls
		//BrowserUpProxyServer browserup_proxy = new BrowserUpProxyServer();
		
		/*
		DefaultApi browserup_proxy = new DefaultApi();
		
		List<HarEntry> har_entries = browserup_proxy.entries(port, urlPattern) //entries(8000, "\"^(http|https)://" + hub_node_url.getHost() + "\\\\.com/.*$\"");
		for(HarEntry entry: har_entries) {
			entry.
		}
		*/
		
		this.setBrowserName(browser);
		if("chrome".equals(browser)){
			this.driver = openWithChrome(hub_node_url);
		}
		else if("firefox".equals(browser)){
			this.driver = openWithFirefox(hub_node_url);
		}
		else if("internet_explorer".equals(browser)){
			this.driver = openWithInternetExplorer(hub_node_url);
		}
		else if("safari".equals(browser)){
			this.driver = openWithSafari(hub_node_url);
		}
		else if("opera".equals(browser)){
			this.driver = openWithOpera(hub_node_url);
		}
		setYScrollOffset(extractYOffset(driver));
		setXScrollOffset(extractXOffset(driver));
		setViewportSize(getViewportSize(driver));
	}
	
	/**
	 * @return current {@link WebDriver driver}
	 */
	public WebDriver getDriver(){
		return this.driver;
	}

	/**
	 * Navigates to a given url and waits for it the readyState to be complete
	 * 
	 * @param url
	 * @throws MalformedURLException 
	 */
	public void navigateTo(String url) throws MalformedURLException{
		getDriver().get(url);
		
		try {
			waitForPageToLoad();
		}catch(Exception e) {
			Alert alert = isAlertPresent();
			if(alert != null){
				log.debug("Alert was encountered during navigation page load!!!");
				PageAlert page_alert = new PageAlert(alert.getText());
				
				page_alert.performChoice(getDriver(), AlertChoice.DISMISS);
			}
		}
		
		waitForPageToLoad();
		log.debug("successfully navigated to "+url);
	}

	/**
	 * Removes canvas element added by Selenium when taking screenshots
	 * 
	 * @param src
	 * @return
	 * 
	 * @precondition src != null
	 */
	public static String cleanSrc(String src) {
		Document html_doc = Jsoup.parse(src);
		html_doc.select("canvas").remove();

		return html_doc.html();
	}
	
	/**
	 * Closes the browser opened by the current driver.
	 */
	public void close(){
		try{
			driver.quit();
		}
		catch(Exception e){
			log.debug("Unknown exception occurred when closing browser" + e.getMessage());
		}
	}
	
	/**
	 * open new firefox browser
	 * 
	 * @param url
	 * @return firefox web driver
	 * @throws MalformedURLException 
	 */
	public static WebDriver openWithFirefox(URL hub_node_url) throws MalformedURLException, UnreachableBrowserException, GridException{
		FirefoxOptions options = new FirefoxOptions();
		options.addArguments("user-agent=QanairyBot");

		//options.setHeadless(true);
	    RemoteWebDriver driver = new RemoteWebDriver(hub_node_url, options);
		driver.manage().window().maximize();

	    //driver.manage().window().setSize(new Dimension(1024, 768));
	    // Puts an Implicit wait, Will wait for 10 seconds before throwing exception
	    //driver.manage().timeouts().implicitlyWait(300, TimeUnit.SECONDS);
	    
		return driver;
	}

	/**
	 * open new opera browser
	 * 
	 * @param url
	 * @return Opera web driver
	 * @throws MalformedURLException 
	 */
	public static WebDriver openWithOpera(URL hub_node_url) throws MalformedURLException, UnreachableBrowserException, GridException{
	    DesiredCapabilities cap = DesiredCapabilities.opera();
	    cap.setBrowserName("opera");
		cap.setJavascriptEnabled(true);

	    RemoteWebDriver driver = new RemoteWebDriver(hub_node_url, cap);
	    // Puts an Implicit wait, Will wait for 10 seconds before throwing exception
	    driver.manage().timeouts().implicitlyWait(300, TimeUnit.SECONDS);
	    
		return driver;
	}
	
	/**
	 * open new Safari browser window
	 * 
	 * @param url
	 * @return safari web driver
	 */
	public static WebDriver openWithSafari(URL hub_node_url) throws MalformedURLException, UnreachableBrowserException, GridException{
	    DesiredCapabilities cap = DesiredCapabilities.safari();

		RemoteWebDriver driver = new RemoteWebDriver(hub_node_url, cap);
	    driver.manage().timeouts().implicitlyWait(300, TimeUnit.SECONDS);

		return driver;
	}
	
	/**
	 * Opens internet explorer browser window
	 * 
	 * @param url
	 * @return internet explorer web driver
	 */
	public static WebDriver openWithInternetExplorer(URL hub_node_url) throws MalformedURLException, UnreachableBrowserException, GridException {
	    DesiredCapabilities capabilities = DesiredCapabilities.internetExplorer();

		RemoteWebDriver driver = new RemoteWebDriver(hub_node_url, capabilities);
		
		return driver;
	}
	
	/**
	 * open new Chrome browser window
	 * 
	 * @param url
	 * @return Chrome web driver
	 * @throws MalformedURLException 
	 */
	public static WebDriver openWithChrome(URL hub_node_url) 
			throws MalformedURLException, UnreachableBrowserException, WebDriverException, GridException {
		ChromeOptions chrome_options = new ChromeOptions();
		chrome_options.addArguments("user-agent=LookseeBot");
		chrome_options.addArguments("window-size=1920,1080");

		//options.setHeadless(true);

		//cap.setCapability("video", "True"); // NOTE: "True" is a case sensitive string, not boolean.

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
		log.debug("Requesting chrome remote driver from hub");
		RemoteWebDriver driver = new RemoteWebDriver(hub_node_url, chrome_options);
		driver.manage().window().maximize();

		//driver.manage().window().setSize(new Dimension(1024, 768));
	    //driver.manage().timeouts().implicitlyWait(30L, TimeUnit.SECONDS);
	    //driver.manage().timeouts().pageLoadTimeout(30L, TimeUnit.SECONDS);
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
	public BufferedImage getViewportScreenshot() throws IOException, GridException{
		return ImageIO.read(((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE));
	}
	
	/**
	 * Gets image as a base 64 string
	 * 
	 * @return File png file of image
	 * @throws IOException
	 */
	public BufferedImage getFullPageScreenshot() throws IOException, GridException{
		return Shutterbug.shootPage(driver,ScrollStrategy.WHOLE_PAGE, 1000).getImage();
	}
	
	/**
	 * 
	 * @param screenshot
	 * @param elem
	 * @return
	 * @throws IOException
	 */	
	public BufferedImage getElementScreenshot(ElementState element) throws IOException{
		//calculate element position within screen
		WebElement web_element = driver.findElement(By.xpath(element.getXpath()));
		return Shutterbug.shootElementVerticallyCentered(driver, web_element).getImage();
	}
	
	/**
	 * 
	 * @param screenshot
	 * @param elem
	 * @return
	 * @throws IOException
	 */
	public BufferedImage getElementScreenshot(WebElement element) throws IOException{
		//calculate element position within screen
		return Shutterbug.shootElementVerticallyCentered(driver, element).getImage();
	}
	
	/**
	 * 
	 * @param screenshot
	 * @param elem
	 * @return
	 * @throws IOException
	 */
	public static BufferedImage getElementScreenshot(ElementState element_state, BufferedImage page_screenshot, Browser browser) throws IOException{
		//calculate element position within screen
		int point_x = element_state.getXLocation()+5;
		int point_y = element_state.getYLocation();
		int width = element_state.getWidth()+5;
		int height = element_state.getHeight();
		if((point_x+width) >= page_screenshot.getWidth()) {
			width = page_screenshot.getWidth()-point_x-1;
		}
		
		return page_screenshot.getSubimage(point_x, point_y, width, height);
	}
	
	public static List<Form> extractAllSelectOptions(PageState page, WebDriver driver){
		return null;
	}
	
	public static ElementState findLabelFor(Set<ElementState> elements, String for_id){
		for(ElementState elem : elements){
			//ElementState tag = (ElementState)elem;
			if(elem.getName().equals("label") ){
				if(elem.getAttribute("for").contains(for_id)){
					return elem;
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
	public static Set<ElementState> findLabelsFor(Set<ElementState> elements, String[] for_ids){
		Set<ElementState> labels = new HashSet<ElementState>();
		for(ElementState elem : elements){
			//ElementState tag = (ElementState)elem;
			if(elem.getName().equals("label") ){
				for(String id : for_ids){
					if(elem.getAttributes().get("for").contains(id)){
						labels.add(elem);
					}
				}
			}
		}
		
		return labels;
	}


	/**
	 * 
	 * @param page_element
	 * @param driver
	 */
	public static void outlineElement(ElementState page_element, WebDriver driver) {
		WebElement element = driver.findElement(By.xpath(page_element.getXpath()));
		((JavascriptExecutor)driver).executeScript("arguments[0].style.border='2px solid yellow'", element);
	}
	
	/**
	 * Finds page element by xpath
	 * 
	 * @param xpath
	 * 
	 * @return {@link WebElement} located at the provided xpath
	 * 
	 * @pre xpath != null
	 * @pre !xpath.isEmpty()
	 */
	public WebElement findWebElementByXpath(String xpath){
		assert xpath != null;
		assert !xpath.isEmpty();
		
		return driver.findElement(By.xpath(xpath));
	}
	
	/**
	 * Reads all css styles and loads them into a hash for a given {@link WebElement element}
	 * 
	 * NOTE: THIS METHOD IS VERY SLOW DUE TO SLOW NATURE OF getCssValue() METHOD. AS cssList GROWS
	 * SO WILL THE TIME IN AT LEAST A LINEAR FASHION. THIS LIST CURRENTLY TAKES ABOUT .4 SECONDS TO CHECK ENTIRE LIST OF 13 CSS ATTRIBUTE TYPES
	 * @param element the element to for which css styles should be loaded.
	 * @throws XPathExpressionException 
	 * @throws IOException 
	 */
	public static Map<String, String> loadPreRenderCssProperties(Document jsoup_doc, org.w3c.dom.Document w3c_document, Map<String, Map<String, String>> rule_set_list, URL url, String xpath, Element element) throws XPathExpressionException, IOException{
		assert w3c_document != null;
		assert url != null;
		assert xpath != null;
		
		log.warn("-----------------------------------------------------------------------------");
		log.warn("-----------------------------------------------------------------------------");
		log.warn("loading post render css properties");
		Map<String, String> css_map = new HashMap<>();

		//THE FOLLOWING WORKS TO GET RENDERED CSS VALUES FOR EACH ELEMENT THAT ACTUALLY HAS CSS


		//count all elements with non 0 px values that aren't decimals
		//extract all rules
		
		for(String css_selector : rule_set_list.keySet()) {
			if(css_selector.startsWith("@")){
				continue;
			}
			String suffixless_selector = css_selector;
			if(css_selector.contains(":")) {
				suffixless_selector = css_selector.substring(0, css_selector.indexOf(":"));
			}
			Elements selected_elements = jsoup_doc.select(suffixless_selector);
			for(Element selected_elem : selected_elements) {
				if(selected_elem.html().equals(element.html())) {
					//apply rule styling to element css_map
					css_map.putAll(rule_set_list.get(css_selector));
				}
			}
		}
		
		return css_map;
	}
	
	/**
	 * Reads all css styles and loads them into a hash for a given {@link WebElement element}
	 * 
	 * NOTE: THIS METHOD IS VERY SLOW DUE TO SLOW NATURE OF getCssValue() METHOD. AS cssList GROWS
	 * SO WILL THE TIME IN AT LEAST A LINEAR FASHION. THIS LIST CURRENTLY TAKES ABOUT .4 SECONDS TO CHECK ENTIRE LIST OF 13 CSS ATTRIBUTE TYPES
	 * @param element the element to for which css styles should be loaded.
	 * @throws XPathExpressionException 
	 */
	public static Map<String, String> loadCssPropertiesUsingParser(org.w3c.dom.Document w3c_document, URL url, String xpath) throws XPathExpressionException{
		assert w3c_document != null;
		assert url != null;
		assert xpath != null;
		
		Map<String, String> css_map = new HashMap<>();

		//THE FOLLOWING WORKS TO GET RENDERED CSS VALUES FOR EACH ELEMENT THAT ACTUALLY HAS CSS


		//count all elements with non 0 px values that aren't decimals
		MediaSpec media = new MediaSpecAll(); //use styles for all media
		
		StyleMap map = CSSFactory.assignDOM(w3c_document, "UTF-8", url, media, true);
		
		//create the style map

		XPath xPath = XPathFactory.newInstance().newXPath();
		Node node = (Node)xPath.compile(xpath).evaluate(w3c_document, XPathConstants.NODE);
		NodeData style = map.get((org.w3c.dom.Element)node); //get the style map for the element
		//log.warn("Element styling  ::  "+style);
		if(style != null) {
			for(String property : style.getPropertyNames()) {
				
				//log.warn("property value 2 :: "+style.getAsString(property, false));
				if(style.getValue(property, false) == null) {
					continue;
				}
				String property_value = style.getValue(property, true).toString();
				//String property_value = CssPropertyFactory.construct(style.getProperty(property));
				//log.warn("Property : value    ->    "+property+  "   :    "+property_value);
				if("background-color".contentEquals(property)) {
					log.warn("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
					log.warn("background color property found " + property_value);
					log.warn("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
					
				}
				if(property_value == null || property_value.isEmpty() || "none".equalsIgnoreCase(property_value)) {
					continue;
				}
				css_map.put(property, property_value);
			}
		}
		//}
		return css_map;
	}
	
	/**
	 * Reads all css styles and loads them into a hash for a given {@link WebElement element}
	 * 
	 * NOTE: THIS METHOD IS VERY SLOW DUE TO SLOW NATURE OF getCssValue() METHOD. AS cssList GROWS
	 * SO WILL THE TIME IN AT LEAST A LINEAR FASHION. THIS LIST CURRENTLY TAKES ABOUT .4 SECONDS TO CHECK ENTIRE LIST OF 13 CSS ATTRIBUTE TYPES
	 * @param root the element to for which css styles should be loaded.
	 * @throws XPathExpressionException 
	 */
	public static Map<String, String> loadCssPrerenderedPropertiesUsingParser(List<RuleSet> rule_sets, Element element){

		
		Map<String, String> css_map = new HashMap<>();
		//map rule set declarations with elements and save element
		for(RuleSet rule_set : rule_sets) {
			for(CombinedSelector selector : rule_set.getSelectors()) {
				
				String selector_str = selector.toString();
				if(selector_str.startsWith(".")
					|| selector_str.startsWith("#")) 
				{
					selector_str = selector_str.substring(1);
				}

				if(element.attr("class").contains(selector_str) || element.attr("id").contains(selector_str) || element.tagName().equals(selector_str)) {
					
					//TODO look for padding and add it to the document
					for(Declaration declaration : rule_set) {
						String raw_property_value = declaration.toString();
						raw_property_value = raw_property_value.replace(";", "");
						String[] property_val = raw_property_value.split(":");
						
						css_map.put(property_val[0], property_val[1]);
					}
				}
			}
		}
		
		return css_map;
	}
	
	/**
	 * Reads all css styles and loads them into a hash for a given {@link WebElement element}
	 * 
	 * NOTE: THIS METHOD IS VERY SLOW DUE TO SLOW NATURE OF getCssValue() METHOD. AS cssList GROWS
	 * SO WILL THE TIME IN AT LEAST A LINEAR FASHION. THIS LIST CURRENTLY TAKES ABOUT .4 SECONDS TO CHECK ENTIRE LIST OF 13 CSS ATTRIBUTE TYPES
	 * @param element the element to for which css styles should be loaded.
	 */
	@Deprecated
	public static Map<String, String> loadCssProperties(WebElement element, WebDriver driver){
		JavascriptExecutor executor = (JavascriptExecutor)driver;
		String script = "var s = '';" +
		                "var o = getComputedStyle(arguments[0]);" +
		                "for(var i = 0; i < o.length; i++){" +
		                "s+=o[i] + ':' + o.getPropertyValue(o[i])+';';}" + 
		                "return s;";

		String response = executor.executeScript(script, element).toString();
		
		Map<String, String> css_map = new HashMap<String, String>();

		String[] css_prop_vals = response.split(";");
		for(String prop_val_pair : css_prop_vals) {
			String[] prop_val = prop_val_pair.split(":");
			css_map.put(prop_val[0], prop_val[1]);
		}
		
		return css_map;
	}
	
	/**
	 * Reads all css styles and loads them into a hash for a given {@link WebElement element}
	 * 
	 * NOTE: THIS METHOD IS VERY SLOW DUE TO SLOW NATURE OF getCssValue() METHOD. AS cssList GROWS
	 * SO WILL THE TIME IN AT LEAST A LINEAR FASHION. THIS LIST CURRENTLY TAKES ABOUT .4 SECONDS TO CHECK ENTIRE LIST OF 13 CSS ATTRIBUTE TYPES
	 * @param element the element to for which css styles should be loaded.
	 */
	public static Map<String, String> loadTextCssProperties(WebElement element){
		String[] cssList = {"font-family", "font-size", "text-decoration-color", "text-emphasis-color"};
		Map<String, String> css_map = new HashMap<String, String>();
		
		for(String propertyName : cssList){
			String element_value = element.getCssValue(propertyName);
			if(element_value != null && !element_value.isEmpty()){
				css_map.put(propertyName, element_value);
			}
		}
		
		return css_map;
	}
	
	public String getBrowserName() {
		return browser_name;
	}

	public void setBrowserName(String browser_name) {
		this.browser_name = browser_name;
	}
	

	public long getYScrollOffset() {
		return y_scroll_offset;
	}

	public void setYScrollOffset(long y_scroll_offset) {
		this.y_scroll_offset = y_scroll_offset;
	}

	public long getXScrollOffset() {
		return x_scroll_offset;
	}

	public void setXScrollOffset(long x_scroll_offset) {
		this.x_scroll_offset = x_scroll_offset;
	}
	
	public void scrollToElement(WebElement elem) 
    { 
		((JavascriptExecutor)driver).executeScript("arguments[0].scrollIntoView({block: \"center\"});", elem);
		Point offsets = getViewportScrollOffset();
		this.setXScrollOffset(offsets.getX());
		this.setYScrollOffset(offsets.getY());
    }
	
	public void scrollToElement(ElementState element_state) 
    { 
		WebElement elem = driver.findElement(By.xpath(element_state.getXpath()));
		((JavascriptExecutor)driver).executeScript("arguments[0].scrollIntoView({block: \"center\"});", elem);
		Point offsets = getViewportScrollOffset();
		this.setXScrollOffset(offsets.getX());
		this.setYScrollOffset(offsets.getY());
    }
	
	public void scrollTo(long x_offset, long y_offset) 
    {
		//only scroll to position if it isn't the same position
		((JavascriptExecutor)driver).executeScript("window.scrollTo("+ x_offset +","+ y_offset +");");
		//Timing.pauseThread(1000);
		Point offsets = getViewportScrollOffset();
		this.setXScrollOffset(offsets.getX());
		this.setYScrollOffset(offsets.getY());
    }
	
	
	/**
	 * Extract all attributes from a given {@link WebElement}
	 * 
	 * @param element {@link WebElement} to have attributes loaded for
	 * @param javascriptDriver - 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Map<String, String> extractAttributes(WebElement element) {
		List<String> attribute_strings = (ArrayList<String>)((JavascriptExecutor)driver).executeScript("var items = []; for (index = 0; index < arguments[0].attributes.length; ++index) { items.push(arguments[0].attributes[index].name + '::' + arguments[0].attributes[index].value) }; return items;", element);
		return loadAttributes(attribute_strings);
	}
	

	
	/**
	 * Loads attributes for this element into a list of {@link Attribute}s
	 * 
	 * @param attributeList
	 */
	private Map<String, String> loadAttributes( List<String> attributeList){
		Map<String, String> attributes_seen = new HashMap<String, String>();
		
		for(int i = 0; i < attributeList.size(); i++){
			String[] attributes = attributeList.get(i).split("::");
			
			if(attributes.length > 1){
				String attribute_name = attributes[0].trim().replace("\'", "'");
				String[] attributeVals = attributes[1].split(" ");

				if(!attributes_seen.containsKey(attribute_name)){
					attributes_seen.put(attribute_name, Arrays.asList(attributeVals).toString());	
				}
			}
		}

		return attributes_seen;
	}

	
	/**
	 * Retrieves the x and y scroll offset of the viewport as a {@link Point}
	 * 
	 * @param browser
	 * 
	 * @return {@link Point} containing offsets
	 */
	public Point getViewportScrollOffset(){		
		Object objy = ((JavascriptExecutor)driver).executeScript("return window.pageYOffset;");
		Object objx = ((JavascriptExecutor)driver).executeScript("return window.pageXOffset;");

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
		
		return new Point(x_offset, y_offset);
	}
	
	/**
	 * Retrieve coordinates of {@link WebElement} in the current viewport
	 * 
	 * @param element {@link WebElement}
	 * @return {@link Point} coordinates
	 */
	private static Point getLocationInViewport(WebElement element, int x_offset, int y_offset) {
		Point location = element.getLocation();
		int y_coord = calculateYCoordinate(y_offset, location);
		int x_coord = calculateXCoordinate(x_offset, location);
       
		return new Point(x_coord, y_coord);
	}
	
	/**
	 * Retrieve coordinates of {@link WebElement} in the current viewport
	 * 
	 * @param element {@link WebElement}
	 * @return {@link Point} coordinates
	 */
	public static Point getLocationInViewport(ElementState element, int x_offset, int y_offset) {
		int y_coord = element.getYLocation() - y_offset;
		int x_coord = element.getXLocation() - x_offset;

		return new Point(x_coord, y_coord);
	}
	
	public static int calculateYCoordinate(int y_offset, Point location){
		if((location.getY() - y_offset) >= 0){
			return location.getY() - y_offset;
		}
		return y_offset;
	}
	
	public static int calculateXCoordinate(int x_offset, Point location){
		if((location.getX() - x_offset) >= 0){
			return location.getX() - x_offset;
		}
		return x_offset;
	}

	/**
	 * Waits for the document ready state to be complete, then observes page transition if it exists
	 */
	public void waitForPageToLoad() throws MalformedURLException {
		new WebDriverWait(driver, 30).until(
				webDriver -> ((JavascriptExecutor) webDriver)
					.executeScript("return document.readyState")
					.equals("complete"));
	}
	
	private static Dimension getViewportSize(WebDriver driver) {
		int width = extractViewportWidth(driver);
		int height = extractViewportHeight(driver);
		return new Dimension(width, height);
	}

	private static long extractXOffset(WebDriver driver) {
		JavascriptExecutor executor = (JavascriptExecutor) driver;
		return (Long) executor.executeScript("return window.pageXOffset;");
	}
	
	private static long extractYOffset(WebDriver driver) {
		JavascriptExecutor executor = (JavascriptExecutor) driver;
		return (Long) executor.executeScript("return window.pageYOffset;");
	}
	
	private static int extractViewportWidth(WebDriver driver) {
		JavascriptExecutor js = (JavascriptExecutor) driver;
		int viewportWidth = Integer.parseInt(js.executeScript(JS_GET_VIEWPORT_WIDTH, new Object[0]).toString());
		return viewportWidth;
	}

	private static int extractViewportHeight(WebDriver driver) {
		JavascriptExecutor js = (JavascriptExecutor) driver;
		int result = Integer.parseInt(js.executeScript(JS_GET_VIEWPORT_HEIGHT, new Object[0]).toString());
		return result;
	}

	public Dimension getViewportSize() {
		return viewport_size;
	}

	public void setViewportSize(Dimension viewport_size) {
		this.viewport_size = viewport_size;
	}

	public void moveMouseOutOfFrame() {
		try{
			Actions mouseMoveAction = new Actions(driver).moveByOffset(-(getViewportSize().getWidth()/3), -(getViewportSize().getHeight()/3) );
			mouseMoveAction.build().perform();
		}catch(Exception e){
			//log.warn("Exception occurred while moving mouse out of frame :: " + e.getMessage());
		}
	}

	public void moveMouseToNonInteractive(Point point) {
		try{
			Actions mouseMoveAction = new Actions(driver).moveByOffset(point.getX(), point.getY());
			mouseMoveAction.build().perform();
		}catch(Exception e){
			//log.warn("Exception occurred while moving mouse out of frame :: " + e.getMessage());
		}
	}
	
	/**
	 * 
	 * @param driver
	 * @return
	 */
	public Alert isAlertPresent(){
	    try { 
	        return driver.switchTo().alert(); 
	    }   // try 
	    catch (NoAlertPresentException Ex) { 
	        return null; 
	    }   // catch 
	}

	public boolean isDisplayed(ElementState element) {
		WebElement web_element = driver.findElement(By.xpath(element.getXpath()));
		return web_element.isDisplayed();
	}

	public static List<RuleSet> extractRuleSetsFromStylesheets(List<String> raw_stylesheets, URL page_state_url) {
		List<RuleSet> rule_sets = new ArrayList<>();
		for(String raw_stylesheet : raw_stylesheets) {
			//parse the style sheet
			try {
				StyleSheet sheet = CSSFactory.parseString(raw_stylesheet, page_state_url);
				for(int idx = 0; idx < sheet.size(); idx++) {
					if(sheet.get(idx) instanceof RuleFontFaceImpl
							|| sheet.get(idx) instanceof RuleMediaImpl
							|| sheet.get(idx) instanceof RuleKeyframesImpl) {
						continue;
					}
					
					//access the rules and declarations
					RuleSet rule = (RuleSet) sheet.get(idx);       //get the first rule
					rule_sets.add(rule);
				}
				//or even print the entire style sheet (formatted)
			} catch (MalformedURLException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			} catch (CSSException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
		}
		return rule_sets;
	}

	public static List<String> extractStylesheets(String src) {
		List<String> raw_stylesheets = new ArrayList<>();
		Document doc = Jsoup.parse(src);	
		Elements stylesheets = doc.select("link");
		for(Element stylesheet : stylesheets) {
			if("text/css".equalsIgnoreCase(stylesheet.attr("type"))) {
				String stylesheet_url = stylesheet.absUrl("href");
				//parse the style sheet
				if(stylesheet_url.trim().isEmpty()) {
					stylesheet_url = stylesheet.attr("href");
					if(stylesheet_url.startsWith("//")) {
						stylesheet_url = "https:"+stylesheet_url;
					}
				}
				try {
					raw_stylesheets.add(URLReader(new URL(stylesheet_url)));
				} catch (MalformedURLException e1) {
					// TODO Auto-generated catch block
					log.warn(e1.getMessage());
					//e1.printStackTrace();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
		
		return raw_stylesheets;
	}
	
	
	public static String URLReader(URL url) throws IOException {
        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        
        if(con.getContentEncoding() != null && con.getContentEncoding().equalsIgnoreCase("gzip")) {
        	return readGzipStream(con.getInputStream());
        }
        else {
        	return readStream(con.getInputStream());
        }
	}
	

	
	private static String readGzipStream(InputStream inputStream) {
		 StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream( inputStream )));) {
            String nextLine = "";
            while ((nextLine = reader.readLine()) != null) {
                sb.append(nextLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
	}

	private static String readStream(InputStream in) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in));) {
            String nextLine = "";
            while ((nextLine = reader.readLine()) != null) {
                sb.append(nextLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}
