import java.text.DateFormat;
import java.util.List;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;


public class Browser {

	private WebDriver driver = null;
	private List<WebElement> elements = null;
	private Page page = null;
	
	public Browser(String url) {
		this.driver = openWithFirefox(url);
		this.page = new Page(driver, DateFormat.getDateInstance(), false);
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
		return new Page(driver, date, valid);
	}
	
	/**
	 * @inherit
	 */
	public void close(){
		driver.close();
	}
	
	public static WebDriver openWithFirefox(String url){
		FirefoxProfile firefoxProfile = new FirefoxProfile();
		
		WebDriver driver = new FirefoxDriver(firefoxProfile);
		driver.get(url);
		
		return driver;
	}
	
	

}
