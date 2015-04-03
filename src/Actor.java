import java.text.DateFormat;
import java.util.List;

import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;


public class Actor implements Runnable {

	private String url = null;
	private WebElement element = null;
	private String action = null;
	
	public Actor(String url) {
		this.url = url;
	}

	public Actor(String url, WebElement element, String action) {
		this.url = url;
		this.element = element;
		this.action = action;
	}

	/**
	 * This method will open a firefox browser and load the url that was given at instantiation.
	 *  The actor will load the page into memory, access the element it needs, and then perform an action on it.
	 */
	public void run() {
		//get a web browser driver and open the browser to the desired url
		//get the page
		WebDriver driver = DiffHandler.openWithFirefox(url);
		System.out.println("Retrieved driver");
		String pageSrc = driver.getPageSource();
		System.out.println("Retrieved page source");
		//create list of all possible actions
		Page page = new Page(driver, pageSrc, url, DateFormat.getDateInstance(), false);
		System.out.println("Built page instance.");
		
		ConcurrentNode<Page> currentPageNode = new ConcurrentNode<Page>(page);
		List<PageElement> visibleElements = currentPageNode.data.getVisibleElements(driver);
		System.out.println("Wrapped page instance in a graph node");
		
		for(PageElement elem : visibleElements){
			System.out.println("Element retreived from list");

			ConcurrentNode<PageElement> element = new ConcurrentNode<PageElement>(elem);
			currentPageNode.addOutput(element);
			
			System.out.println("Element wrapped in a graph node");
			ConcurrentNode<String> actionNode = new ConcurrentNode<String>("click");
			
			try{
				ActionFactory.execAction(driver, elem, actionNode.data);
			}
			catch(StaleElementReferenceException e){
				System.err.println("A SYSTEM ERROR WAS ENCOUNTERED WHILE ACTOR WAS PERFORMING ACTION : "+
						actionNode.data + ". ");
			}
			
			System.err.println("ACTOR EXECUTED ACTION :: " +actionNode.data);
		}
	}
}
