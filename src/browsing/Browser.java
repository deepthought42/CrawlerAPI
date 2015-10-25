package browsing;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.util.List;

import org.openqa.selenium.Alert;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
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

/**
 * 
 * @author Brandon Kindred
 */
public class Browser {

	private WebDriver driver;
	private List<WebElement> elements = null;
	//private Page page = null;
	
	public Browser(String url) throws IOException {
		System.err.println(Thread.currentThread().getName() + " -> URL :: "+url);
		this.driver = openWithFirefox(url);
		this.driver.get(url);
		//page = new Page(this.driver, DateFormat.getDateInstance());
	}
	
	public Browser(String url, Page browserPage) {
		driver = openWithFirefox(url);
		driver.get(url);
		//page = browserPage;
	}
	
	/*public Browser(String url, Page browserPage) {
		driver = openWithFirefox(url);
		page = browserPage;
	}
	*/
	
	/**
	 * 
	 * 
	 */
	public WebDriver getDriver(){
		return driver;
	}
	
	/**
	 * 
	 * @param elem
	 * @return
	 */
	public List<WebElement> findElement(WebElement elem){		
		return elements;
	}

	/**
	 * 
	 * @return
	 * @throws IOException 
	 * @throws MalformedURLException 
	 */
	public Page getPage() throws MalformedURLException, IOException{
		return new Page(driver, DateFormat.getInstance());
	}

	/**
	 * 
	 * @param date
	 * @param valid
	 * @return
	 * @throws IOException 
	 */
	public Page updatePage(DateFormat date) throws IOException{
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
	
	/**
	 * Gets image as a base 64 string
	 * @return base64 representation of image
	 * @throws IOException
	 */
	public static String getScreenshot(WebDriver driver) throws IOException{
		String src = ((TakesScreenshot)driver).getScreenshotAs(OutputType.BASE64);
		//File srcFile = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
		//Random rand = new Random();
		// Now you can do whatever you need to do with it, for example copy somewhere
		//FileUtils.copyFile(srcFile, new File("/home/deepthought/Desktop/screenshots/screenshot"+DateFormat.getInstance().format(new Date()).replace(" ", "")+""+rand.nextInt() +".png"));
		
		return src;
	}
}
