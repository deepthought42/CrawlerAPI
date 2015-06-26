package browsing;
import java.text.DateFormat;
import java.util.List;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.UnreachableBrowserException;

import util.Timing;


public class Browser {

	private static WebDriver driver;
	private static List<WebElement> elements = null;
	private static Page page = null;
	
	public Browser(WebDriver driver, String url) {
		System.out.println("CREATING PAGE...");
		
		page = new Page(driver, DateFormat.getDateInstance(), false);
		System.out.println("PAGE CREATED.");
	}
	
	public Browser(String url, Page browserPage) {
		//openWithFirefox(url);
		System.out.print("CREATING PAGE...");
		
		page = browserPage;
		System.out.println("PAGE CREATED.");
	}
	
	public WebDriver getDriver(){
		return driver;
	}
	
	public List<WebElement> findElement(WebElement elem){		
		return elements;
	}
	
	public Page getPage(){
		return page;
	}

	public Page updatePage(DateFormat date, boolean valid){
		return new Page(driver, date, valid);
	}
	
	/**
	 * @inherit
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
	
	public static WebDriver openWithFirefox(String url){
		FirefoxProfile firefoxProfile = new FirefoxProfile();
		System.out.println("FIREFOX PROFILE LOADED!");
		WebDriver driver = new FirefoxDriver(firefoxProfile);
		System.out.println("DRIVER LOADED.");
		driver.get(url);
		return driver;
	}
}
