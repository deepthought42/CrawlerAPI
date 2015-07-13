package browsing;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.concurrent.ConcurrentHashMap;

import observableStructs.ObservableQueue;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.InvalidSelectorException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.UnreachableBrowserException;
import org.openqa.selenium.support.ui.WebDriverWait;

import structs.Path;


/**
 * An this threadable class is implemented to handle the interaction with a browser 
 * @author Brandon Kindred
 *
 */
public class BrowserActor extends Thread{

	private static WebDriver driver;
	private String url = null;
	private ObservableQueue<Path> pathQueue = null;
	private ConcurrentNode<Page> pageNode = null;
	private Path path = null;
	private List<Page> pagesSeen = new ArrayList<Page>();
	private Browser browser = null;
	
	public BrowserActor(String url) {
		this.url = url;
		browser = new Browser(url);
		this.path = new Path();
	}

	/**
	 * Creates instance of BrowserActor with given url for entry into website
	 * 
	 * @param url	url of page to be accessed
	 * @param queue observable path queue
	 * 
	 * @pre queue != null
	 * @pre !queue.isEmpty()
	 */
	public BrowserActor(String url, ObservableQueue<Path> queue) {
		assert(queue != null);
		assert(queue.isEmpty());
		this.url = url;
		browser = new Browser(url);
		this.pathQueue = queue;
		this.path = new Path();
	}
	
	/**
	 * Creates instance of browserActor with existing path to crawl.
	 * 
	 * @param queue ovservable path queue
	 * @param path	path to use to navigate to desired page
	 * 
	 * @pre queue != null
	 * @pre !queue.isEmpty()
	 */
	public BrowserActor(ObservableQueue<Path> queue, Path path) {
		assert(queue != null);
		assert(queue.isEmpty());
		
		ConcurrentNode<?> node = (ConcurrentNode<?>) path.getPath().poll(); 
		assert(((Page)node.getData()).getUrl() != null);
		
		System.out.println("BROWSER ACTOR :: PATH HAS "+ path.getPath().size() + " NODES; preparing to crawl");

		this.path = path;
		this.url = ((Page)node.getData()).getUrl();
		
		browser = new Browser(url);

		if(path.getPath().size() > 0){
			//	find first page in path
		}

		this.pathQueue = queue;
	}
	
	public ConcurrentNode<Page> getPageNode(){
		return this.pageNode;
	}
	
	public void signIn(String username, String pass){
		driver.findElement(By.xpath("//a[@id='signInLink']")).click();
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

		System.out.println(this.getName() + " :: RETRIEVING DRIVER...");
		//signIn("test@test.com", "testtest");

		System.out.println(this.getName() + " :: Built page instance.");
		//TODO :: LOAD PAGE NODE FROM MEMORY FOR GIVEN URL.
		// IF IT EXISTS CHECK IF IT HAS BEEN MAPPED. IF IT HAS NOT THEN MAP IT
		// ELSE CRAWL MAP TO FIND NEW PAGES TO MAP
		this.pageNode = new ConcurrentNode<Page>(browser.getPage());

		if(this.path.getPath().isEmpty()){
			this.path.add(pageNode);
		}

		//boolean offerAccepted = pathQueue.offer(new Path(pageNode));
		
		//System.out.println(this.getName() + " :: OFFER ACCEPTED? :::  " + offerAccepted);
		System.out.println("------------------------------------------------------------");
		System.out.println(this.getName() + " :: Wrapped page instance in a graph node");
		
		System.out.println("----------------------------------------------------");
		System.out.println(this.getName() + " :: loaded up elements. there were " + this.pageNode.data.getElements().size());
		System.out.println("----------------------------------------------------");
		if(this.pageNode.getOutputs() != null & this.pageNode.getOutputs().isEmpty()){
			long tStart = System.currentTimeMillis();
			pageCrawler(this.pageNode);
			long tEnd = System.currentTimeMillis();
			long tDelta = tEnd - tStart;
			double elapsedSeconds = tDelta / 1000.0;
			
			System.out.println("-----ELAPSED TIME FOR CRAWL :: "+elapsedSeconds + "-----");
			System.out.println(this.getName() + " :: FINISHED EXECUTING ALL ACTIONS FOR THIS PAGE");
			System.out.println("#######################################################");
		}
		System.out.println(this.getName() + " :: CRAWLING PAGE COMPLETE...");
		System.out.println(this.getName() + " :: NUMBER OF NEW PAGES :: "+pagesSeen.size());
		System.out.println(this.getName() + " :: Number of elements in page output :: " + pageNode.getOutputs().size());
		
		//else{
			System.out.println("Page node already has outputs. lets crawl them!");
			//browser = new Browser(url);
			//mapCrawler(browser, url, this.pageNode);
			
			ConcurrentHashMap<ConcurrentNode<?>, Double> map = pageNode.getOutputs();
			System.out.println("Map created. There were " + map.size() + " output links for this node");
		//}
		this.browser.close();
	}
	
	/**
	 * Retrieves all elements on a page, and performs all known actions on each element.
	 * 	Generates a map consisting of the page nodes outputs being Elements and elements 
	 *  outputs being actions.
	 *  
	 *  TODO :: Remove sign in from crawler. It should be abstracted away.
	 *  
	 * @param browser A Browser instance
	 * @param pageNode The network node of type Page that is to be crawled
	 */
	private void pageCrawler(ConcurrentNode<Page> pageNode){
		System.out.println("Commencing page crawl...");
		int element_idx = 0;
		int last_element_idx = -1;
		ConcurrentNode<PageElement> elementNode = null;
		int action_idx = 0;
		String[] actions = ActionFactory.getActions();
		//System.out.println("Starting iteration over elements");
		Page page = pageNode.getData();
		page.getVisibleElements(browser.getDriver(), page.getElements(), "//body");
		while(element_idx < pageNode.getData().getElements().size()){
			if(this.path.getPath().size() > 1){
				System.out.println("%%%    PATH SIZE IS GREATER THAN 1...CRAWLING PATH");
				pageNode = mapCrawler();
				page = pageNode.getData();
				page.getVisibleElements(browser.getDriver(), page.getElements(), "//body");
				pagesSeen.add(page);
				boolean previouslySeen = false;

				for(Page pageSeen : pagesSeen){
					if(pageSeen.equals(page)){
						previouslySeen = true;
					}
				}
				if(!previouslySeen){
					pagesSeen.add(page);
				}
				System.out.println("%%%%   PATH CRAWLED!");
			}
			boolean err = false;
			try{
				PageElement pageElement = page.getElements().get(element_idx);
				WebElement elem = browser.getDriver().findElement(By.xpath(pageElement.getXpath()));
				//JavascriptExecutor javascriptDriver = (JavascriptExecutor)driver;
		        //javascriptDriver.executeScript("arguments[0].style.border='3px solid red'", elem);
								
				//execute the following if there is no problem executing action
				(new ActionFactory(browser.getDriver())).execAction(elem , actions[action_idx]);
				Page newPage = null;
				while(newPage == null){
					try{
						newPage = new Page(browser.getDriver(), DateFormat.getDateInstance(), false);
					}
					catch(UnhandledAlertException e){
						try{
							browser.HandleAlert(driver, new WebDriverWait(driver, 2000) );
						}catch(NullPointerException e1){
							
						}
					}
				}
				//check if element already has been added. If it has then skip
				// adding element to page and just add action to element
				
				//add action node to current element node
				//add current element node as input to the action node
				
				if(element_idx != last_element_idx){
					elementNode = new ConcurrentNode<PageElement>(pageElement);
					pageNode.addOutput(elementNode);
					elementNode.addInput(pageNode);
					last_element_idx = element_idx;
					
				}
				
				ConcurrentNode<String> actionNode = new ConcurrentNode<String>(actions[action_idx]);
				elementNode.addOutput(actionNode);
				actionNode.addInput(elementNode);
				
				//if the page has changed the create a new page node and associate it with the current actionNode
				// dead end so that complex functionality can be built out later.
				if(!page.equals(newPage)){
					boolean previouslySeen = false;
					for(Page pageSeen : pagesSeen){
						if(pageSeen.equals(newPage)){
							previouslySeen = true;
						}
					}
					if(!previouslySeen){
						pagesSeen.add(newPage);

						//add element, action and new page to path
						this.path.add(elementNode);
						this.path.add(actionNode);
						this.path.add(new ConcurrentNode<Page>(newPage));
						System.out.println(this.getName() + " :: PATH LENGTH === " + this.path.getPath().size());
						pathQueue.offer(this.path);
						
						this.path = new Path();
						path.add(pageNode);
					}
					System.out.println(this.getName() + " :: PAGE HAS CHANGED. GROWING GRAPH...");
					//Add new page to action output
					actionNode.addOutput(new ConcurrentNode<Page>(newPage));
					browser.getUrl(url);
					mapCrawler();
				}	
				else{
					//System.out.println("PAGE DIDN'T CHANGED. DEAD ENDING FOR NOW.");
					//Since there was no change we want to dead end here. This will allow for later passes to 
					//	build out more complex functionality recognition by chaining element action sequences.
				}
				
				//browser = new Browser(url, pageNode.getData());

				//signIn("test@test.com", "testtest");
			}
			catch(StaleElementReferenceException e){
				System.err.println(this.getName() + " :: A SYSTEM ERROR WAS ENCOUNTERED WHILE ACTOR WAS PERFORMING ACTION : "+
						actions[action_idx] + ". ");
				//e.printStackTrace();
				browser.getUrl(url);
			}
			catch(UnreachableBrowserException e){
				System.err.println(this.getName() + " :: Browser is unreachable.");
				err = true;
				break;
				//driver.close();
				//driver = Browser.openWithFirefox(url);
				//browser = new Browser(url, pageNode.getData());
				
				//e.printStackTrace();
			}
			catch(ElementNotVisibleException e){
				System.out.println(this.getName() + " :: ELEMENT IS NOT CURRENTLY VISIBLE.");
			}
			catch(InvalidSelectorException e){
				System.out.println(this.getName() + " :: INVALID SELECTOR");
				//e.printStackTrace();
			}
			catch(WebDriverException e){
				err = true;
				System.err.println(this.getName() + " :: problem accessing WebDriver instance");
				//e.printStackTrace();
				//browser.getUrl(url);
				//browser = new Browser(url, pageNode.getData());
				//signIn("test@test.com", "testtest");
			}
			
			if(!err){
				if(action_idx >= actions.length-1){
					action_idx = 0;
					element_idx++;
				}
				else{
					action_idx++;
				}
			}
			System.out.println(this.getName() + " :: PERFORMING ACTION :: "+ actions[action_idx] + " on element at index :: "+element_idx);
			
			//if element has no actions connected to it remove its connection from page.
		}
	}
	
	/**
	 * Crawls path to from start to end node
	 * 
	 * @param path {@link Path path} to crawl
	 */
	private ConcurrentNode<Page> mapCrawler(){
		//crawl path to end node then resume page crawling.
		
		Page currentPage = null;
		PageElement currentElement = null;
		WebElement elem = null;
		String currentAction = null;
		Object lastNode = null;
		
		//iterate over elements to navigate to last node in path
		Iterator pathIter = this.path.getPath().iterator();
		
		while(pathIter.hasNext()){
			Object o = ((ConcurrentNode<?>)pathIter.next()).getData();
			
			if(o.getClass().getName().equals("browsing.Page")){
				currentPage = (Page)o;
			}
			else if(o.getClass().getName().equals("browsing.PageElement")){
				currentElement = (PageElement)o;
				elem = browser.getDriver().findElement(By.xpath(currentElement.getXpath()));
			}
			else if(o.getClass().getName().equals("String")){
				currentAction = (String)o;
				(new ActionFactory(browser.getDriver())).execAction(elem , currentAction);
			}
			lastNode = o;
		}
		
		System.out.println(this.getName() + " :: Done iterating over path. Now on element with class "+lastNode.getClass().getName() +"; data = " + lastNode.toString());
		return new ConcurrentNode<Page>(browser.getPage()); 
	}
	
	
}
