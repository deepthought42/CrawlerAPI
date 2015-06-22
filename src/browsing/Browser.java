package browsing;
import java.text.DateFormat;
import java.util.List;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;

import util.Timing;


public class Browser {

	private WebDriver driver = null;
	private List<WebElement> elements = null;
	private Page page = null;
	
	public Browser(String url) {
		openWithFirefox(url);
		System.out.println("CREATING PAGE...");
		
		this.page = new Page(this.driver, DateFormat.getDateInstance(), false);
		System.out.println("PAGE CREATED.");
	}
	
	public Browser(String url, Page page) {
		openWithFirefox(url);
		System.out.println("CREATING PAGE...");
		
		this.page = page;
		System.out.println("PAGE CREATED.");
	}
	
	public WebDriver getDriver(){
		return this.driver;
	}
	
	public List<WebElement> findElement(WebElement elem){		
		return elements;
	}
	
	public Page getPage(){
		return this.page;
	}

	public Page updatePage(DateFormat date, boolean valid){
		return new Page(this.driver, date, valid);
	}
	
	/**
	 * @inherit
	 */
	public void close(){
		this.driver.close();
	}
	
	/*public WebDriver openWithFirefox(String url){
		FirefoxProfile firefoxProfile = new FirefoxProfile();
		System.out.println("FIREFOX PROFILE LOADED!");
		WebDriver driver = new FirefoxDriver(firefoxProfile);
		System.out.println("DRIVER LOADED.");
		driver.get(url);
		return driver;
	}*/
	
	public void openWithFirefox(String url){
		FirefoxProfile firefoxProfile = new FirefoxProfile();
		System.out.println("FIREFOX PROFILE LOADED!");
		this.driver = new FirefoxDriver(firefoxProfile);
		System.out.println("DRIVER LOADED.");
		this.driver.get(url);
	}
	

}
