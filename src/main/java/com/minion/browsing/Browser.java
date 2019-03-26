package com.minion.browsing;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
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
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.UnreachableBrowserException;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.minion.util.Timing;
import com.qanairy.models.Attribute;
import com.qanairy.models.Form;
import com.qanairy.models.PageElement;
import com.qanairy.models.PageState;

/**
 * Handles the management of selenium browser instances and provides various methods for interacting with the browser 
 */
@Component
public class Browser {
	private static Logger log = LoggerFactory.getLogger(Browser.class);

	private static final int DIMENSION_OFFSET_PIXELS = 5;
	
	private WebDriver driver = null;
	private String browser_name; 
	private int y_scroll_offset;
	private int x_scroll_offset;
	private Dimension viewport_size;
	
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
	 * 
	 * @pre url != null
	 * @pre browser != null
	 */
	public Browser(String browser, URL hub_node_url) throws MalformedURLException {
		assert browser != null;
		
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
		setYScrollOffset(0);
		setXScrollOffset(0);
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
	 */
	public void navigateTo(String url){
		getDriver().get(url);
		log.debug("successfully navigated to "+url);
		Browser.waitForPageToLoad(getDriver());
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
		Pattern link_pattern = Pattern.compile("<link (.*)></link>",
	            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
		Pattern script_pattern = Pattern.compile("<script (.*)></script>",
	            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
		src = script_pattern.matcher(src).replaceAll("");
		src = link_pattern.matcher(src).replaceAll("");
		return p.matcher(src).replaceAll("");
	}
	
	/**
	 * Closes the browser opened by the current driver.
	 */
	public void close(){
		try{
			driver.quit();
		}
		catch(Exception e){
			log.info("Unknown exception occurred when closing browser" + e.getMessage());
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
		//options.setHeadless(true);
	    RemoteWebDriver driver = new RemoteWebDriver(hub_node_url, options);
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
		ChromeOptions options = new ChromeOptions();
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
		log.info("Requesting chrome remote driver from hub");
		RemoteWebDriver driver = new RemoteWebDriver(hub_node_url, options);
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
	public static BufferedImage getViewportScreenshot(WebDriver driver) throws IOException, GridException{
		return ImageIO.read(((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE));
	}
	
	/**
	 * 
	 * @param screenshot
	 * @param elem
	 * @return
	 * @throws IOException
	 */
	public static BufferedImage getElementScreenshot(Browser browser, WebElement elem, BufferedImage page_screenshot) throws IOException{
		
		//calculate element position within screen
		Point point = browser.getLocationInViewport(elem);
		Dimension dimension = elem.getSize();
		
		// Get width and height of the element
		int elem_width = dimension.getWidth();
		int elem_height = dimension.getHeight()+DIMENSION_OFFSET_PIXELS;
		
		int point_x = point.getX();
		int point_y = point.getY();
		
		return page_screenshot.getSubimage(point_x, point_y, elem_width, elem_height);
	}
	
	/**
	 * 
	 * @param screenshot
	 * @param elem
	 * @return
	 * @throws IOException
	 */
	public static BufferedImage getElementScreenshot(BufferedImage page_screenshot, Dimension dimension, Point point, Browser browser) throws IOException{
		// Get width and height of the element
		int elemWidth = dimension.getWidth();
		int elemHeight = dimension.getHeight();

		int y_coord = point.getY()-browser.getYScrollOffset();
		return page_screenshot.getSubimage(point.getX(), y_coord, elemWidth, elemHeight);
	}
	
	public static List<Form> extractAllSelectOptions(PageState page, WebDriver driver){
		return null;
	}
	
	public static PageElement findLabelFor(Set<PageElement> elements, String for_id){
		for(PageElement elem : elements){
			//PageElement tag = (PageElement)elem;
			if(elem.getName().equals("label") ){
				for(Attribute attr : elem.getAttributes()){
					if(attr.getName().equals("for")){
						for(String val : attr.getVals()){
							if(val.equals(for_id)){
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
	public static Set<PageElement> findLabelsFor(Set<PageElement> elements, String[] for_ids){
		Set<PageElement> labels = new HashSet<PageElement>();
		for(PageElement elem : elements){
			//PageElement tag = (PageElement)elem;
			if(elem.getName().equals("label") ){
				for(Attribute attr : elem.getAttributes()){
					if(attr.getName().equals("for")){
						for(String val : attr.getVals()){
							for(String id : for_ids){
								if(val.equals(id)){
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
	 * 
	 * @param page_element
	 * @param driver
	 */
	public static void outlineElement(PageElement page_element, WebDriver driver) {
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
	 */
	public static Map<String, String> loadCssProperties(WebElement element){
		String[] cssList = {"backface-visibility", "visible", "display", "position", "color", "font-family", "width", "height", "left", "right", "top", "bottom", "transform"};
		Map<String, String> css_map = new HashMap<String, String>();
		
		for(String propertyName : cssList){
			try{
				String element_value = element.getCssValue(propertyName);
				if(element_value != null && !element_value.isEmpty()){
					css_map.put(propertyName, element_value);
				}
			}catch(Exception e){
				
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
	

	public int getYScrollOffset() {
		return y_scroll_offset;
	}

	public void setYScrollOffset(int y_scroll_offset) {
		this.y_scroll_offset = y_scroll_offset;
	}

	public int getXScrollOffset() {
		return x_scroll_offset;
	}

	public void setXScrollOffset(int x_scroll_offset) {
		this.x_scroll_offset = x_scroll_offset;
	}
	
	public void scrollToElement(WebElement elem) 
    { 
		((JavascriptExecutor)driver).executeScript("arguments[0].scrollIntoView();", elem);
		Timing.pauseThread(1000);

		Point offsets = getViewportScrollOffset();
		this.setXScrollOffset(offsets.getX());
		this.setYScrollOffset(offsets.getY());
    }
	
	/**
	 * Retrieves the x and y scroll offset of the viewport as a {@link Point}
	 * 
	 * @param browser
	 * 
	 * @return {@link Point} containing offsets
	 */
	private Point getViewportScrollOffset(){		
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
	private Point getLocationInViewport(WebElement element) {
		Point location = element.getLocation();
		int y_coord = calculateYCoordinate(this.getYScrollOffset(), location);
		int x_coord = calculateXCoordinate(this.getXScrollOffset(), location);
       
		return new Point(x_coord, y_coord);
	}
	
	public static int calculateYCoordinate(int y_offset, Point location){
		return location.getY() - y_offset;
	}
	
	public static int calculateXCoordinate(int x_offset, Point location){
		return location.getX() - x_offset;
	}

	public static void waitForPageToLoad(WebDriver driver) {
		new WebDriverWait(driver, 600).until(
				webDriver -> ((JavascriptExecutor) webDriver).executeScript("return document.readyState").equals("complete"));
		Timing.pauseThread(500);
	}
	
	private static Dimension getViewportSize(WebDriver driver) {
		int width = extractViewportWidth(driver);
		int height = extractViewportHeight(driver);
		return new Dimension(width, height);
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

	private static final String JS_GET_VIEWPORT_WIDTH = "var width = undefined; if (window.innerWidth) {width = window.innerWidth;} else if (document.documentElement && document.documentElement.clientWidth) {width = document.documentElement.clientWidth;} else { var b = document.getElementsByTagName('body')[0]; if (b.clientWidth) {width = b.clientWidth;}};return width;";

	private static final String JS_GET_VIEWPORT_HEIGHT = "var height = undefined;  if (window.innerHeight) {height = window.innerHeight;}  else if (document.documentElement && document.documentElement.clientHeight) {height = document.documentElement.clientHeight;}  else { var b = document.getElementsByTagName('body')[0]; if (b.clientHeight) {height = b.clientHeight;}};return height;";
}
