import java.text.DateFormat;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import util.Timing;


public class Actor implements Runnable {

	private String url = null;
	private WebElement element = null;
	private String action = null;
	private WebDriver driver = null;
	
	public Actor(String url) {
		this.url = url;
	}

	public Actor(String url, WebElement element, String action) {
		this.url = url;
		this.element = element;
		this.action = action;
	}
	
	public void signIn(String username, String pass){
		driver.findElement(By.xpath("//a[@id='signInLink']")).click();
	    new WebDriverWait(driver, 5).until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//input[@id='userFormEmailField']")));
		WebElement user = driver.findElement(By.xpath("//input[@id='userFormEmailField']"));
		user.sendKeys(username);
		WebElement password = driver.findElement(By.xpath("//input[@id='userFormPasswordField']"));
		password.sendKeys(pass);
		driver.findElement(By.xpath("//button[@id='userFormSignInButton']")).click();
	}

	/**
	 * This method will open a firefox browser and load the url that was given at instantiation.
	 *  The actor will load the page into memory, access the element it needs, and then perform an action on it.
	 */
	public void run() {
		//get a web browser driver and open the browser to the desired url
		//get the page
		this.driver = DiffHandler.openWithFirefox(url);
		System.out.println("Retrieved driver");
		signIn("test@test.com", "testtest");
		String pageSrc = driver.getPageSource();
		System.out.println("Retrieved page source");
		//create list of all possible actions
		Page page = new Page(driver, pageSrc, url, DateFormat.getDateInstance(), false);
		System.out.println("Built page instance.");
		
		ConcurrentNode<Page> currentPageNode = new ConcurrentNode<Page>(page);
		List<PageElement> visibleElements = currentPageNode.data.getVisibleElements(driver);
		System.out.println("Wrapped page instance in a graph node");
		
		for(PageElement elem : visibleElements){
			ConcurrentNode<PageElement> element = new ConcurrentNode<PageElement>(elem);
			currentPageNode.addOutput(element);
		}
		
		
		System.out.println("----------------------------------------------------");
		System.err.println("loaded up elements. there were " + currentPageNode.getOutputs().size());
		System.out.println("----------------------------------------------------");
		
		System.out.println("Element wrapped in a graph node");
		
		for(PageElement elem : visibleElements){
			for(String action : ActionFactory.getActions()){
				try{
					System.out.println("EXECUTING ACTION :"+ action+ " Now");
					ActionFactory.execAction(driver, elem, action);
					
					//execute the following if it is clicked
					//ConcurrentNode<String> actionNode = new ConcurrentNode<String>("click");

				}
				catch(StaleElementReferenceException e){
					System.err.println("A SYSTEM ERROR WAS ENCOUNTERED WHILE ACTOR WAS PERFORMING ACTION : "+
							action + ". ");
					System.err.println("ACTOR EXECUTED ACTION :: " +action);
				}
			}
		}
	}
}
