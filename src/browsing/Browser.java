package browsing;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.util.List;

import org.openqa.selenium.Alert;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.UnreachableBrowserException;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

public class Browser {

	private WebDriver driver;
	private List<WebElement> elements = null;
	private Page page = null;
	
	public Browser(String url) throws MalformedURLException {
		System.err.println(Thread.currentThread().getName() + " -> URL :: "+url);
		this.driver = openWithFirefox(url);
		this.driver.get(url);
		page = new Page(this.driver, DateFormat.getDateInstance());
	}
	
	public Browser(String url, Page browserPage) {
		driver = openWithFirefox(url);
		driver.get(url);
		page = browserPage;
	}
	
	/*public Browser(String url, Page browserPage) {
		driver = openWithFirefox(url);
		page = browserPage;
	}
	*/
	
	public WebDriver getDriver(){
		return driver;
	}
	
	public List<WebElement> findElement(WebElement elem){		
		return elements;
	}

	/**
	 * 
	 * @return
	 */
	public Page getPage(){
		return page;
	}

	/**
	 * 
	 * @param date
	 * @param valid
	 * @return
	 * @throws MalformedURLException
	 */
	public Page updatePage(DateFormat date, boolean valid) throws MalformedURLException{
		return new Page(driver, date);
	}
	
	/**
	 * Closes the browser opened by the current driver.
	 */
	public void close(){
		try{
			driver.close();
		}
		catch(NullPointerException e){
			System.err.println("Error closing driver. Driver is NULL");
		}
		catch(UnreachableBrowserException e){
			System.err.println("Error closing driver");
		}
		finally{
			driver = null;
		}
	}
	
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
	 * @return
	 */
	public static WebDriver openWithFirefox(String url){
		FirefoxProfile firefoxProfile = new FirefoxProfile();
		WebDriver driver = new FirefoxDriver(firefoxProfile);
		return driver;
	}
	
	/**
	 * open new firefox browser
	 * 
	 * @param url
	 * @return
	 */
	public static WebDriver openWithPhantomjs(String url){
		
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
}
