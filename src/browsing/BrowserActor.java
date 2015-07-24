package browsing;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
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

/*
 * NEEDED:
 * 		NAVIGATOR/CRAWLER -- crawl path to end
 * 		MAPCOLORER -- mark completely evaluated nodes as seen{1}, mapped{2}
 * 		NODE_BUILDER -- find all possible nodes that can be added and add them to the current node. 
 * 						for each new node added, create a new path with the new node as the last node on path
 * 
 * 
 * 
 * Retrieve path fom current path variable. 
 * Crawl path to end of chain.
 * If node at end of chain is a page, then add element to path
 * If node at end of chain is an element, then perform action and add action to path following element
 * 		If performing action results in change of page then add page to action. add page to path
 * If node at end of chain is an action, then add an element to the action node and to end of path.
 * 
 * if all elements for a page have been evaluated and added to the page node then mark page node as mapped {1}
 * If last element in path is an action node and all elements not including the elements preceding the action in the path 
 *    have been added to the action node then color node as mapped
 * 
 *    
 */
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
	private Path clonePath = null;
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
		
		
		this.path = path;
		ConcurrentNode<?> node = (ConcurrentNode<?>) path.getPath().getFirst(); 
		assert(((Page)node.getData()).getUrl() != null);

		this.url = ((Page)node.getData()).getUrl();
		
		System.out.println("BROWSER ACTOR :: PATH HAS "+ path.getPath().size() + " NODES; preparing to crawl");
		
		browser = new Browser(url);
/*
		if(path.getPath().size() > 0 && !node.getClass().equals("browsing.Page")){
			crawlPath();
		}
*/
		this.pathQueue = queue;
	}
	
	/**
	 * 
	 * @return PageNode
	 */
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
		
		do{
			long tStart = System.currentTimeMillis();
			this.pageNode = new ConcurrentNode<Page>(browser.getPage());
			if(this.path.getPath().isEmpty()){
				this.path.add(pageNode);
				System.out.println("PATH LENGTH :: "+this.path.getPath().size());
			}
			try{
				crawlPath();
			}catch(NoSuchElementException e){
				System.err.println("NO SUCH ELEMENT FOUND IN PATH. PATH IS EMPTY");
				e.printStackTrace();
			}
			
			System.out.println("EXPANDING NODE...");
			expandNodePath();
			System.out.println("NODE EXPANDED..");
			
			long tEnd = System.currentTimeMillis();
			long tDelta = tEnd - tStart;
			double elapsedSeconds = tDelta / 1000.0;
			
			System.out.println("-----ELAPSED TIME FOR CRAWL :: "+elapsedSeconds + "-----");
			System.out.println("#######################################################");
			System.out.println("THREADS STILL RUNNING :: "+Thread.activeCount());
			this.path = this.pathQueue.poll();
		}while(!this.pathQueue.isEmpty());
		this.browser.close();
	}
	
	/**
	 * Crawls the path for the current BrowserActor.
	 */
	private void crawlPath() throws NoSuchElementException{
		Iterator pathIterator = this.path.getPath().iterator();
		ActionFactory actionFactory = new ActionFactory(this.browser.getDriver());

		ConcurrentNode<?> entryNode = (ConcurrentNode<?>) pathIterator.next();
		String className = entryNode.getData().getClass().getCanonicalName();
		Page pageNode = null;
		//skip first node since we should have already loaded it during initialization
		while(pathIterator.hasNext()){
			ConcurrentNode<?> pathNode = (ConcurrentNode<?>) pathIterator.next();
			
			className = pathNode.getData().getClass().getCanonicalName();
			
			if(className.equals("browsing.Page")){
				pageNode = (Page)pathNode.getData();
				//verify current page matches current node data
				//if not mark as different
			}
			else if(className.equals("browsing.ElementAction")){
				ElementAction elemAction = (ElementAction)pathNode.getData();
				WebElement element = browser.getDriver().findElement(By.xpath(elemAction.getPageElement().getXpath()));
				//execute element action
				actionFactory.execAction(element, elemAction.getAction());
				
				Page newPage = new Page(browser.getDriver(), DateFormat.getDateInstance(), true);
				//if after performing action page is no longer equal do stuff
				System.out.println("NEW PAGE :: "+newPage);
				
				if(pageNode != null && !pageNode.equals(newPage)){
					System.out.println("PAGE NODE :: "+pageNode);
					System.out.println("Page has changed...adding new page to path");
					ConcurrentNode<Page> newPageNode = new ConcurrentNode<Page>(newPage);
					pathNode.addOutput(newPageNode);
					newPageNode.addInput(pathNode);
					this.path.add(newPageNode);
				}
				
				//else if after performing action styles on one or more of the elements is no longer equal then mark element as changed.
				//	An element that has changed cannot change again. If it does then the path is marked as dead
			}
		}
	}
	
	/**
	 * Finds all potential expansion nodes from current node
	 */
	private void expandNodePath(){
		System.out.println("SETTING UP EXPANSION VARIABLES..");
		ConcurrentNode<?> node = (ConcurrentNode<?>) this.path.getPath().getLast();
		String className = node.getData().getClass().getCanonicalName();
		ActionFactory actionFactory = new ActionFactory(this.browser.getDriver());
		String[] actions = ActionFactory.getActions();

		//if node is a page then find all potential elementActions that can be taken including different values
		//if node is an elementAction find all elementActions for the last seen page node that have not been seen
		//   since the page node was encountered and add them.
		if(className.equals("browsing.Page")){
			//System.out.println("FOUND PAGE NODE...Expanding now.");
			//verify current page matches current node data
			//if not mark as different
			Page page = (Page)node.getData();
			Page newPage = new Page(browser.getDriver(), DateFormat.getDateInstance(), false);
			if(!page.equals(newPage)){
				//System.out.println("PAGES DO NOT MATCH!!!!");
				return;
			}
			//System.out.print("CREATING ELEMENT ITERATOR...");
			//get all known possible compinations of PageElement actions and add them as potential expansions
			Iterator elementIterator = page.getElements().iterator();
			//System.out.println("SIZE :: "+page.getElements().size());
			while(elementIterator.hasNext()){
				PageElement elem = (PageElement) elementIterator.next();
				//System.out.println("ADDING ELEMENT WITH XPATH :: "+ elem.getXpath());
						
				for(int i = 0; i < actions.length; i++){
					ElementAction elemAction = new ElementAction(elem, actions[i]);
					//System.out.println("ADDING ACTION TO ELEMENT :: " + actions[i]);
					//Clone path then add ElementAciton to path and push path onto path queue					
					ConcurrentNode<ElementAction> elementAction = new ConcurrentNode<ElementAction>(elemAction);
					elementAction.addInput(node);
					node.addOutput(elementAction);
					
					putPathOnQueue(elementAction);
				}				
			}
		}
		else if(className.equals("browsing.ElementAction")){
			//System.out.println("FOUND ELMENT ACTION NODE...EXPANDING WIHT ELEMENT ACTIONS");
			ArrayList<ElementAction> elementActionSeenList = new ArrayList<ElementAction>();
			List<PageElement> elementActionAvailableList;
			//navigate path back to last seen page
			//for each ElementAction seen, record elementAction.
			Iterator descendingIterator = this.path.getPath().descendingIterator();
			Page page = null;
			
			while(descendingIterator.hasNext()){
				ConcurrentNode<?> descNode = (ConcurrentNode<?>) descendingIterator.next();
				
				if(descNode.getData().getClass().getCanonicalName().equals("browsing.Page")){
					page = (Page)descNode.getData();
					elementActionAvailableList = page.getElements();
					break;
				}
				else{
					elementActionSeenList.add((ElementAction)descNode.getData());
				}
			}
			
			//add each elementAction for last seen page excluding elementActions seen while finding page
			Iterator elementIterator = page.getElements().iterator();
			
			while(elementIterator.hasNext()){
				PageElement elem = (PageElement) elementIterator.next();
				for(int i = 0; i < actions.length; i++){
					ElementAction elemAction = new ElementAction(elem, actions[i]);
					System.out.println("TOTAL ELEMENT ACTIONS SEEN..."+elementActionSeenList.size());
					Iterator seenElementIterator = elementActionSeenList.iterator();
					boolean seen = false;
					while(seenElementIterator.hasNext()){
						if(((ElementAction)seenElementIterator.next()).equals(elemAction)){
							seen = true;
						}
					}
					if(!seen){
						//Clone path then add ElementAciton to path and push path onto path queue					
						ConcurrentNode<ElementAction> elementAction = new ConcurrentNode<ElementAction>(elemAction);
						elementAction.addInput(node);
						node.addOutput(elementAction);
					
						putPathOnQueue(elementAction);
					}
				}				
			}
		}
	}
	
	/**
	 * Adds the given {@link Path path} to the queue
	 * 
	 * @param path path to be added
	 */
	private boolean putPathOnQueue(ConcurrentNode<?> node){
		Path clonePath = Path.clone(path);
		clonePath.add(node);
		//System.out.println("CLONE PATH LENGTH :: "+clonePath.getPath().size());
		return this.pathQueue.add(clonePath);
	}
	
	
	/**
	 * Retrieves all elements on a page, and performs all known actions on each element.
	 * 	Generates a map consisting of the page nodes outputs being Elements and elements 
	 *  outputs being actions.
	 *  
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
			if(this.clonePath.getPath().size() > 1){
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
				System.out.println("%%%%   PATH NODE CRAWLED!");
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
				//there is no connection through inputs for nodes due to choice to keep graph directed
				if(element_idx != last_element_idx){
					elementNode = new ConcurrentNode<PageElement>(pageElement);
					pageNode.addOutput(elementNode);
					last_element_idx = element_idx;
				}
				
				ConcurrentNode<String> actionNode = new ConcurrentNode<String>(actions[action_idx]);
				elementNode.addOutput(actionNode);
				
				
				//if the page has changed the create a new page node and associate it with the current actionNode
				// dead end so that complex functionality can be built out later.
				if(!page.equals(newPage)){
					boolean previouslySeen = false;
					for(Page pageSeen : pagesSeen){
						if(pageSeen.equals(newPage)){
							previouslySeen = true;
							newPage = pageSeen;
						}
					}
					//add element, action and new page to path
					this.path.add(elementNode);
					this.path.add(actionNode);
					
					if(!previouslySeen){
						pagesSeen.add(newPage);
						actionNode.addOutput(new ConcurrentNode<Page>(newPage));
						this.path.add(new ConcurrentNode<Page>(newPage));
					}

					System.out.println(this.getName() + " :: PATH LENGTH === " + this.path.getPath().size());
					pathQueue.offer(this.path);
					
					//set new path with pageNode as starting point
					this.path = new Path(pageNode);
					
					System.out.println(this.getName() + " :: PAGE HAS CHANGED. GROWING GRAPH...");
					//Add new page to action output
					browser.getUrl(url);
					mapCrawler();
				}
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
			}
			catch(ElementNotVisibleException e){
				System.out.println(this.getName() + " :: ELEMENT IS NOT CURRENTLY VISIBLE.");
			}
			catch(InvalidSelectorException e){
				System.out.println(this.getName() + " :: INVALID SELECTOR");
			}
			catch(WebDriverException e){
				err = true;
				System.err.println(this.getName() + " :: problem accessing WebDriver instance");
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
