package com.minion.browsing;

import java.awt.image.BufferedImage;
import java.io.File;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.openqa.grid.common.exception.GridException;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.UnreachableBrowserException;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.qanairy.models.Attribute;
import com.qanairy.models.Form;
import com.qanairy.models.PageElement;
import com.qanairy.models.PageState;
import ru.yandex.qatools.ashot.AShot;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.qatools.ashot.shooting.ShootingStrategies;

/**
 * Handles the management of selenium browser instances and provides various methods for interacting with the browser 
 */
@Component
public class Browser {
	private static Logger log = LoggerFactory.getLogger(Browser.class);

	private WebDriver driver = null;
	//private static String[] invalid_xpath_attributes = {"ng-view", "ng-include", "ng-repeat","ontouchstart", "ng-click", "ng-class", "onload", "lang", "xml:lang", "xmlns", "xmlns:fb", "@xmlns:cc", "onsubmit", "webdriver",/*Wordpress generated field*/"data-blogger-escaped-onclick", "src", "alt", "scale", "title", "name","data-analytics","onmousedown", "data-rank", "data-domain", "data-url", "data-subreddit", "data-fullname", "data-type", "onclick", "data-outbound-expiration", "data-outbound-url", "rel", "onmouseover","height","width","onmouseout", "data-cid","data-imp-pixel", "value", "placeholder", "data-wow-duration", "data-wow-offset", "data-wow-delay", "required", "xlink:href"};	

	private static String[] valid_elements = {"div", "span", "ul", "li", "a", "img", "button", "input", "form", "i", "canvas", "h1", "h2", "h3", "h4", "h5", "h6", "datalist", "label", "nav", "option", "ol", "p", "select", "table", "tbody", "td", "textarea", "th", "thead", "tr", "video", "audio", "track"};
	private String browser_name; 
    //private static final String DISCOVERY_HUB_IP_ADDRESS= "xxx.xxx.xxx.xxx";
	//private static final String TEST_HUB_IP_ADDRESS= "xxx.xxx.xxx.xxx";
    
	// PRODUCTION HUB ADDRESS
	private static final String HUB_IP_ADDRESS= "142.93.192.184:4444";
	//private static final String HUB_IP_ADDRESS= "10.136.111.115:4444";

	//STAGING HUB ADDRESS
	//private static final String HUB_IP_ADDRESS="159.89.226.116:4444";
	
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
	public Browser(String browser) throws MalformedURLException, NullPointerException {
		assert browser != null;
		
		int cnt = 0;
		this.setBrowserName(browser);
		while(driver == null && cnt < 20){
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
				Thread.sleep(30000L);
			} catch (InterruptedException e1) {}
		}
	}
	
	/**
	 * @return current {@link WebDriver driver}
	 */
	public WebDriver getDriver(){
		return this.driver;
	}

	public void navigateTo(String url){
		getDriver().get(url);
		try{
			new WebDriverWait(getDriver(), 360).until(
					webDriver -> ((JavascriptExecutor) webDriver).executeScript("return document.readyState").equals("complete"));
		}catch(GridException e){
			log.error(e.getMessage());
		}
		catch(Exception e){
			log.error(e.getMessage());
		}			
		try {
			Thread.sleep(5000L);
		} catch (InterruptedException e) {}
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
			log.error("Grid exception occurred when closing browser", e.getMessage());
		}
		catch(Exception e){
			log.error("Unknown exception occurred when closing browser", e.getMessage());
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
		String node = "http://"+HUB_IP_ADDRESS+"/wd/hub";
	    DesiredCapabilities cap = DesiredCapabilities.firefox();
	    cap.setBrowserName("firefox");
		cap.setJavascriptEnabled(true);

	    RemoteWebDriver driver = new RemoteWebDriver(new URL(node), cap);
	    // Puts an Implicit wait, Will wait for 10 seconds before throwing exception
	    driver.manage().timeouts().implicitlyWait(300, TimeUnit.SECONDS);
	    
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
		String node = "http://"+HUB_IP_ADDRESS+"/wd/hub";
	    DesiredCapabilities cap = DesiredCapabilities.opera();
	    cap.setBrowserName("opera");
		cap.setJavascriptEnabled(true);

	    RemoteWebDriver driver = new RemoteWebDriver(new URL(node), cap);
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
	public static WebDriver openWithSafari() throws MalformedURLException, UnreachableBrowserException, GridException{
		String node = "http://"+HUB_IP_ADDRESS+"/wd/hub";
	    DesiredCapabilities cap = DesiredCapabilities.safari();

		RemoteWebDriver driver = new RemoteWebDriver(new URL(node), cap);
	    driver.manage().timeouts().implicitlyWait(300, TimeUnit.SECONDS);

		return driver;
	}
	
	/**
	 * Opens internet explorer browser window
	 * 
	 * @param url
	 * @return internet explorer web driver
	 */
	public static WebDriver openWithInternetExplorer() throws MalformedURLException, UnreachableBrowserException, GridException {
		String node = "http://"+HUB_IP_ADDRESS+"/wd/hub";
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
		ChromeOptions options = new ChromeOptions();
		//options.setHeadless(true);
		DesiredCapabilities cap = DesiredCapabilities.chrome();
		cap.setCapability(ChromeOptions.CAPABILITY, options);
		
		cap.setJavascriptEnabled(true);
		
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
		
		System.err.println("Requesting chrome remote driver from hub");
        String hub_node_url = "http://"+HUB_IP_ADDRESS+"/wd/hub";
		RemoteWebDriver driver = new RemoteWebDriver(new URL(hub_node_url), cap);
	    //driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);

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
	public static File getViewportScreenshot(WebDriver driver) throws IOException, GridException{
		return ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
	}
	
	
	/**
	 * Gets image as a base 64 string
	 * 
	 * @return File png file of image
	 * @throws IOException
	 */
	public static Screenshot getFullScreenshot(WebDriver driver) throws IOException, GridException{
		return new AShot().shootingStrategy(ShootingStrategies.viewportPasting(1000)).takeScreenshot(driver);
	}
	
	/**
	 * 
	 * @param screenshot
	 * @param elem
	 * @return
	 * @throws IOException
	 */
	public static BufferedImage getElementScreenshot(BufferedImage page_screenshot, Dimension dimension, Point point) throws IOException{
		// Get width and height of the element
		int elem_width = dimension.getWidth();
		int elem_height = dimension.getHeight();
		int point_x = point.getX();
		int point_y = point.getY();
		
		if( (elem_width + 5 + point_x) < page_screenshot.getWidth()){
			elem_width = elem_width+5;
		}
		else{
			elem_width = page_screenshot.getWidth() - point_x;
		}
		
		if((elem_height + 5 + point_y) < page_screenshot.getHeight()){
			elem_height = elem_height+5;
		}
		else{
			elem_height = page_screenshot.getHeight() - point_y;
		}
		
		if( (point_x - 5) >= 0){
			elem_width = elem_width + 5;
			point_x = point_x - 5;
		}
		else{
			elem_width += point_x;
			point_x = 0;
		}
		
		if( (point_y - 5) >= 0){
			elem_height = elem_height + 5;
			point_y = point_y - 5;
		}
		else{
			elem_height += point_y;
			point_y = 0;
		}
		return page_screenshot.getSubimage(point_x, point_y, elem_width, elem_height);
	}
	
	/**
	 * 
	 * @param screenshot
	 * @param elem
	 * @return
	 * @throws IOException
	 */
	public static BufferedImage getElementScreenshot(BufferedImage page_screenshot, Dimension dimension, Point point, WebDriver driver) throws IOException{
		// Get width and height of the element
		int elemWidth = dimension.getWidth();
		int elemHeight = dimension.getHeight();

		JavascriptExecutor executor = (JavascriptExecutor) driver;
		Long viewport_offset = (Long) executor.executeScript("return window.pageYOffset;");
		int y_coord = point.getY()-viewport_offset.intValue();
		return page_screenshot.getSubimage(point.getX(), y_coord, elemWidth, elemHeight);
	}
	
	
	/**
	 * Checks if element is visible in a given screenshot
	 * 
	 * @param screenshot
	 * @param elem
	 * @return
	 * @throws IOException
	 */
	private static boolean isElementVisibleInPane(BufferedImage screenshot, WebElement elem) throws IOException {
		Dimension weD = elem.getSize();
	    Point weP = elem.getLocation();
	    //BufferedImage  fullImg = ImageIO.read(screenshot);

	    int x = screenshot.getWidth();;
	    int y = screenshot.getHeight();
	    int x2 = weD.getWidth() + weP.getX();
	    int y2 = weD.getHeight() + weP.getY();

	    return x2 <= x && y2 <= y && weD.getWidth()>0 && weD.getHeight()>0;
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
}
