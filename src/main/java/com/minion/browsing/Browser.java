package com.minion.browsing;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.OutputType;
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
import org.openqa.selenium.remote.UnreachableBrowserException;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minion.aws.UploadObjectSingleOperation;

/**
 * 
 * @author Brandon Kindred
 */
public class Browser {
    private static final Logger log = LoggerFactory.getLogger(Browser.class);

	private WebDriver driver;
	
	public Browser(String url) throws IOException {
		//log.info(" -> URL :: "+url);
		this.driver = openWithChrome(url);
		//this.driver = openWithFirefox(url);
		//this.driver = openWithPhantomjs(url);
		this.driver.get(url);
		//page = new Page(this.driver, DateFormat.getDateInstance());
	}
	
	public Browser(String url, Page browserPage) {
		driver = openWithPhantomjs(url);
		driver.get(url);
		//page = browserPage;
	}
	
	/**
	 * 
	 * 
	 */
	public WebDriver getDriver(){
		return driver;
	}

	/**
	 * 
	 * @return
	 * @throws IOException 
	 * @throws MalformedURLException 
	 */
	public Page getPage() throws MalformedURLException, IOException{
		return new Page(driver);
	}
	
	public static String cleanSrc(String src){
		src = src.replaceAll("\\s", "");
		
		Pattern p = Pattern.compile("<canvas id=\"fxdriver-screenshot-canvas\" style=\"display: none;\" width=\"([0-9]*)\" height=\"([0-9]*)\"></canvas>",
	            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
	    return p.matcher(src).replaceAll("");
	    
		//return src;
	}

	/**
	 * 
	 * @param date
	 * @param valid
	 * @return
	 * @throws IOException 
	 */
	public Page updatePage(DateFormat date) throws IOException{
		return new Page(driver);
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
		finally{
			driver = null;
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
	 */
	public static WebDriver openWithFirefox(String url){
		System.setProperty("webdriver.gecko.driver", "C:\\Users\\brand\\Dev\\geckodriver-v0.9.0-win64\\geckodriver.exe");

		log.info("Opening Firefox WebDriver connection using URL : " +url);
		//FirefoxProfile firefoxProfile = new FirefoxProfile();
	    DesiredCapabilities capabilities = DesiredCapabilities.firefox();

		WebDriver driver = new FirefoxDriver(capabilities);
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
		log.info("firefox opened");
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

		log.info("Opening Firefox WebDriver connection using URL : " +url);
		//FirefoxProfile firefoxProfile = new FirefoxProfile();
	    DesiredCapabilities capabilities = DesiredCapabilities.internetExplorer();

		WebDriver driver = new InternetExplorerDriver(capabilities);
		log.info("firefox opened");
		return driver;
	}
	
	/**
	 * open new Chrome browser window
	 * 
	 * @param url
	 * @return Chrome web driver
	 */
	public static WebDriver openWithChrome(String url){
		System.setProperty("webdriver.chrome.driver", "C:\\Users\\brand\\Dev\\browser_drivers\\chromedriver_win32\\chromedriver.exe");

		log.info("Opening Firefox WebDriver connection using URL : " +url);
		//FirefoxProfile firefoxProfile = new FirefoxProfile();
	    DesiredCapabilities capabilities = DesiredCapabilities.phantomjs();

		WebDriver driver = new ChromeDriver(capabilities);
		log.info("firefox opened");
		return driver;
	}
	
	/**
	 * open new firefox browser
	 * 
	 * @param url
	 * @return
	 */
	public static PhantomJSDriver openWithPhantomjs(String url){
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
	public List<WebElement> getChildElements(String xpath) throws WebDriverException{
		return driver.findElements(By.xpath(xpath+"/*"));
	}
	
	/**
	 * Retreives all elements on the current page that are visible. In this instance we take 
	 *  visible to mean that it is not currently set to {@css display: none} and that it
	 *  is visible within the confines of the screen. If an element is not hidden but is also 
	 *  outside of the bounds of the screen it is assumed hidden
	 *  
	 * @param driver
	 * @return list of webelements that are currently visible on the page
	 */
	public ArrayList<PageElement> getVisibleElements(String xpath, 
													 HashMap<String, Integer> xpathHash) 
															 throws WebDriverException {
		List<WebElement> pageElements = driver.findElements(By.xpath(xpath + "//*"));
		//TO MAKE BETTER TIME ON THIS PIECE IT WOULD BE BETTER TO PARALELLIZE THIS PART
		ArrayList<PageElement> elementList = new ArrayList<PageElement>();
		if(pageElements.size() <= 0){
			return elementList;
		}
		for(WebElement elem : pageElements){
			
			if(elem.isDisplayed() && (elem.getAttribute("backface-visibility")==null 
					|| !elem.getAttribute("backface-visiblity").equals("hidden"))){
				PageElement pageElem = new PageElement(elem, xpath, ActionFactory.getActions(), xpathHash, PageElement.extractedAttributes(elem, (JavascriptExecutor)driver));
				elementList.add(pageElem);
			}
		}
		
		return elementList;
	}
	
	/**
	 * retreives all elements on a given page that are visible. In this instance we take 
	 *  visible to mean that it is not currently set to {@css display: none} and that it
	 *  is visible within the confines of the screen. If an element is not hidden but is also 
	 *  outside of the bounds of the screen it is assumed hidden
	 *  
	 * @param driver
	 * @return list of webelements that are currently visible on the page
	 */
	 public ArrayList<PageElement> getVisibleElements(String xpath, 
													 int depth, 
													 HashMap<String, Integer> xpathHash) throws WebDriverException {
		List<WebElement> childElements = getChildElements(xpath);
		//TO MAKE BETTER TIME ON THIS PIECE IT WOULD BE BETTER TO PARALELLIZE THIS PART
		ArrayList<PageElement> elementList = new ArrayList<PageElement>();
		if(childElements.size() <= 0){
			return elementList;
		}
		for(WebElement elem : childElements){			
			if(elem.isDisplayed() && (elem.getAttribute("backface-visibility")==null || !elem.getAttribute("backface-visiblity").equals("hidden"))){
				PageElement pageElem = new PageElement( elem, xpath, ActionFactory.getActions(), xpathHash, PageElement.extractedAttributes(elem, (JavascriptExecutor)driver));
				elementList.add(pageElem);
			}
		}
		
		for(PageElement pageElem : elementList){
			pageElem.setChild_elements(getVisibleElements(pageElem.getXpath(), depth+1, xpathHash));
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
		 */
		public static List<PageElement> getVisibleElements(WebDriver driver, String xpath) 
																 throws WebDriverException {
			
			List<WebElement> pageElements = driver.findElements(By.cssSelector("*"));
			log.info("page elements found :: " +pageElements.size());
			//TO MAKE BETTER TIME ON THIS PIECE IT WOULD BE BETTER TO PARALELLIZE THIS PART
			ArrayList<PageElement> elementList = new ArrayList<PageElement>();
			if(pageElements.size() <= 0){
				return elementList;
			}

			int counter = 0;
			for(WebElement elem : pageElements){
				try{
					//log.info("checking visibily and extracting attributes for element " + counter++);
					Date start = new Date();
					if(elem.isDisplayed() && (elem.getAttribute("backface-visibility")==null || !elem.getAttribute("backface-visiblity").equals("hidden"))){
						PageElement pageElem = new PageElement(elem, xpath, ActionFactory.getActions(), new HashMap<String, Integer>(), PageElement.extractedAttributes(elem, (JavascriptExecutor)driver));
						elementList.add(pageElem);
					}
					
					Date end = new Date();
					double execution_time = (end.getTime() - start.getTime())/1000.0;
					if( execution_time > 1.0){
						log.info("All attributes extracted in " + execution_time + " seconds");
					}
				}catch(StaleElementReferenceException e){
					log.error(e.toString());
				}
			}
			
			
			return elementList;
		}	
		

}
