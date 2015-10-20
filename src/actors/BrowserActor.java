package actors;
import graph.Graph;
import graph.Vertex;

import java.net.MalformedURLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import learning.QLearn;
import memory.DataDefinition;
import memory.ObjectDefinition;
import memory.Persistor;
import observableStructs.ObservableHash;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.UnreachableBrowserException;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;

import browsing.ActionFactory;
import browsing.Browser;
import browsing.ElementAction;
import browsing.Page;
import browsing.PageAlert;
import browsing.PageElement;
import structs.Path;

/**
 * This threadable class is implemented to handle the interaction with a browser 
 * 
 * @author Brandon Kindred
 *
 */
public class BrowserActor extends Thread implements Actor{

	private static Random rand = new Random();
	private UUID uuid = null;
	private static WebDriver driver;
	private String url = null;
	private ObservableHash<Integer, Path> queueHash = null;
	private Graph graph = null;
	private Path path = null;
	private Browser browser = null;
	private ResourceManagementActor resourceManager = null;
	private WorkAllocationActor workAllocator = null;
	Persistor persistor = new Persistor();

	/**
	 * 
	 * @param url
	 * @throws MalformedURLException
	 */
	public BrowserActor(String url) throws MalformedURLException {
		this.url = url;
		browser = new Browser(url);
		this.path = new Path();
	}

	/**
	 * Creates instance of BrowserActor with given url for entry into website
	 * 
	 * @param url	url of page to be accessed
	 * @param queue observable path queue
	 * @throws MalformedURLException 
	 * 
	 * @pre queue != null
	 * @pre !queue.isEmpty()
	 */
	public BrowserActor(String url, 
						Path path,
						ObservableHash<Integer, Path> path_queue,
						Graph graph,
						ResourceManagementActor resourceManager, 
						WorkAllocationActor workAllocator) throws MalformedURLException {
		assert(path_queue != null);
		assert(path_queue.isEmpty());
		
		this.uuid = UUID.randomUUID();
		this.url = url;
		this.path = path;
		browser = new Browser(url);
		this.queueHash = path_queue;
		this.graph = graph;
		this.resourceManager = resourceManager;
		this.workAllocator = workAllocator;
	}
	
	/**
	 * Creates instance of browserActor with existing path to crawl.
	 * 
	 * @param queue ovservable path queue
	 * @param path	path to use to navigate to desired page
	 * @throws MalformedURLException 
	 * 
	 * @pre queue != null
	 * @pre !queue.isEmpty()
	 */
	public BrowserActor(ObservableHash<Integer, Path> queue, 
						Graph graph, 
						Path path, 
						ResourceManagementActor resourceManager, 
						WorkAllocationActor workAllocator) throws MalformedURLException {
		assert(queue != null);
		assert(queue.isEmpty());
		
		this.uuid = UUID.randomUUID();
		this.graph = graph;
		Vertex<?> node = graph.getVertices().get(path.getPath().get(0)); 
		assert(((Page)node.getData()).getUrl() != null);
		this.path = path;
		this.url = ((Page)node.getData()).getUrl().toString();
		
		//System.out.println(this.getName() + " BROWSER ACTOR :: PATH HAS "+ path.getPath().size() + " NODES IN PATH");
		browser = new Browser(url);

		this.queueHash = queue;
		this.resourceManager = resourceManager;
		this.workAllocator = workAllocator;
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
		resourceManager.punchIn(this);
		try{
			long tStart = System.currentTimeMillis();
			if(this.path.getPath().isEmpty()){
				System.out.println(this.getName() + " -> Path is empty. Adding to path");
				Vertex<Page> vertex = new Vertex<Page>(browser.getPage());
				graph.addVertex(vertex);
				int vertex_idx = graph.findVertexIndex(vertex);
				//need to add edge to vertex
				this.path.add(vertex_idx);
				Path path = Path.clone(this.path);
				putPathOnQueue(path);
			}
			else{
				System.out.println(this.getName() + " -> PATH IS NOT EMPTY. Working on path.");
				this.url = ((Page)(graph.getVertices().get(this.path.getPath().get(0))).getData()).getUrl().toString();
				System.out.println(this.getName() + " -> NEW URL :: " + this.url);
				browser.getDriver().get(this.url);
			}
			
			boolean successfulCrawl = crawlPath();
			int length_limit = 0;
			while(length_limit < 10){				
				if(successfulCrawl){
					System.out.println(this.getName() + " EXPANDING NODE...");
					try {
						path = expandNodePath();
						learn(path);
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
				}
				
				long tEnd = System.currentTimeMillis();
				long tDelta = tEnd - tStart;
				double elapsedSeconds = tDelta / 1000.0;
				
				System.out.println(this.getName() + " -----ELAPSED TIME RUNNING CRAWLER THROUGH CRAWL AND EXPANSION :: "
						+ elapsedSeconds + "-----");
				System.out.println(this.getName() + " #######################################################");
				this.path = workAllocator.retrieveNextPath();
				System.out.println(this.getName() + " -> PATH RETRIEVED.");
				//close all windows opened during crawl
				try{
					String baseWindowHdl = browser.getDriver().getWindowHandle();
					Set<String> handles = browser.getDriver().getWindowHandles();
					if(handles.size() > 1){
						for(String winHandle : handles){
							browser.getDriver().switchTo().window(winHandle);
						}
						browser.getDriver().close();
						browser.getDriver().switchTo().window(baseWindowHdl);
						System.out.println(this.getName() + " -> CLOSED POPUP WINDOW.");
					}
				}
				catch(NullPointerException e){}
				catch(UnhandledAlertException e){
					e.printStackTrace();
				}
				length_limit++;
			}
		}catch(OutOfMemoryError e){
			System.err.println(this.getName() + " -> Out of memory error");
			e.printStackTrace();
		}
		catch(NullPointerException e){
			System.err.println(this.getName() + " -> NULL POINTER EXCEPTION OCCURRED. --EXITING BROWSER ACTOR--");
			e.printStackTrace();
		}catch(NoSuchElementException e){
			System.err.println(this.getName() + " NO SUCH ELEMENT FOUND IN PATH. PATH IS EMPTY");
			e.printStackTrace();
		}
		catch(UnhandledAlertException e){
			System.err.println(this.getName() + " -> UNHANDLED ALERT EXCEPTION OCCURRED");
			try{
				Alert alert = browser.getDriver().switchTo().alert();
		        alert.accept();
			}
			catch(NoAlertPresentException nae){
				System.err.println(this.getName() + " -> Alert not present");
			}
		}
		catch(MalformedURLException e){
			System.err.println("URL FOR ONE OF PAGES IS MALFORMED");
		}
		this.browser.getDriver().quit();
		resourceManager.punchOut(this);
	}
	
	/**
	 * Crawls the path for the current BrowserActor.
	 * 
	 * @return
	 * @throws java.util.NoSuchElementException
	 * @throws UnhandledAlertException
	 * @throws MalformedURLException
	 */
	private boolean crawlPath() throws java.util.NoSuchElementException, UnhandledAlertException, MalformedURLException{
		Iterator<Integer> pathIterator = this.path.getPath().iterator();
		Page pageNode = null;
		PageElement last_element = null;
		//skip first node since we should have already loaded it during initialization
	
		while(pathIterator.hasNext()){
			int path_node_index = pathIterator.next();
			Vertex<?> pathNode = graph.getVertices().get(path_node_index);
						
			if(pathNode.getData() instanceof Page){
				System.out.println(this.getName() + " -> PAGE IN SEQUENCE.");
				pageNode = (Page)pathNode.getData();
				//if current page does not match current node data 
				if(!browser.getDriver().getPageSource().equals(pageNode.getSrc())){
					return false;
				}
			}
			else if(pathNode.getData() instanceof PageElement){
				System.out.println(this.getName() + " -> PAGE ELEMENT IN SEQUENCE.");
				last_element = (PageElement) pathNode.getData();
			}
			else if(pathNode.getData() instanceof String){
				System.out.println(this.getName() + " -> ACTION IN SEQUENCE.");
				boolean actionPerformedSuccessfully;
				String action = (String) pathNode.getData();
				browser.updatePage( DateFormat.getDateInstance());
				do{
					actionPerformedSuccessfully = performAction(last_element, action );	
				}while(!actionPerformedSuccessfully);
			}
			else if(pathNode.getData() instanceof PageAlert){
				System.err.println(this.getName() + " -> Handling Alert");
				PageAlert alert = (PageAlert)pathNode.getData();
				alert.performChoice(browser.getDriver());
			}
		}

		return true;
	}
	
	/**
	 * Expands path and implements reinforcement learning based on results of expansion
	 * 
	 * @throws MalformedURLException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public Path expandNodePath() throws MalformedURLException, IllegalArgumentException, IllegalAccessException {
		System.out.println("EXPANDING NODE");

		Vertex<?> page_vertex = getLastPageVertex();
		Class<?> className = page_vertex.getData().getClass();
		String[] actions = ActionFactory.getActions();
		
		if(className.equals(PageElement.class) || className.equals(Page.class) || className.equals(String.class)){
			Page page = ((Page)page_vertex.getData());
			PageElement chosen_pageElement = null;
			String chosen_action = null;
			double exploration_coef = rand.nextDouble();
			System.err.println("EXPLORATION COEFFICIENT :: "+ exploration_coef);
			
			if(exploration_coef > 0.5){
				System.out.println("&&&&&  GETTING BEST ELEMENT ACTION PAIR &&&&&&&&&");
				double[] element_probabilities = new double[page.getElements().size()];
				//get index of best estimated element
				getElementOfBestEstimatedIndex(page.getElements(), element_probabilities);
				chosen_pageElement = page.getElements().get(getBestIndex(element_probabilities));
						
				//get best known action given best chosen element
				double[] action_rewards = new double[actions.length];
				calculateActionProbabilities(action_rewards, chosen_pageElement);
				chosen_action = actions[getBestIndex(action_rewards)];
			}
			else{
				// Choose a random element-action pairing
				chosen_pageElement = page.elements.get(getRandomElementIndex(page.getElements().size()));
				chosen_action = actions[getRandomElementIndex(actions.length)];
				System.out.println("$$$$  RANDOM ACTION :: "+chosen_action);
				System.out.println("$$$$  RANDOM ELEMENT :: "+chosen_pageElement.tagName);
			}
			
			//add element and action to current path
			Vertex<PageElement> pageElementVertex = new Vertex<PageElement>(chosen_pageElement);
			Vertex<String> actionVertex = new Vertex<String>(chosen_action);
			
			if(pageElementVertex != null){
				graph.addVertex(pageElementVertex);
				graph.addEdge(page_vertex, pageElementVertex);
				graph.addVertex(actionVertex);
				graph.addEdge(pageElementVertex, actionVertex);
			}

			int page_elem_vertex_idx = graph.findVertexIndex(pageElementVertex);
			int action_vertex_idx = graph.findVertexIndex(actionVertex);

			Path new_path = Path.clone(path);
			new_path.add(page_elem_vertex_idx);
			new_path.add(action_vertex_idx);
			putPathOnQueue(new_path);
			return new_path;
		}
		else if(className.equals(PageAlert.class)){
			System.err.println(this.getName() + " -> Handling Alert from expanding Node.");
			PageAlert alert = (PageAlert)page_vertex.getData();
			alert.performChoice(browser.getDriver());
		}
		
		return path;
	}
	
	/**
	 * Reads path and performs learning tasks
	 * 
	 * @param path an {@link ArrayList} of graph vertex indices. Order matters
	 * @throws MalformedURLException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws NullPointerException
	 */
	public void learn(Path path) throws MalformedURLException, IllegalArgumentException, IllegalAccessException, NullPointerException{
		//REINFORCEMENT LEARNING
		
		System.out.println(this.getName() + " -> Initiating learning");
		//get last action
		//get last element
		//get last page
		String last_action = null;
		PageElement last_element = null;
		Page last_page = null;
				
		ArrayList<Integer> current_path = path.getPath();
		for(int idx = path.getPath().size()-1; idx >= 0; idx--){
			Vertex<?> vertex = graph.getVertices().get(path.getPath().get(idx));
			if(vertex.getData().getClass().equals(String.class)){
				last_action = (String) vertex.getData();
			}
			else if(vertex.getData().getClass().equals(PageElement.class)){
				last_element = (PageElement)vertex.getData();
			}
			else if(vertex.getData().getClass().equals(Page.class)){
				last_page = (Page)vertex.getData();
			}
		}		
		
		//get all objects for the chosen page_element
		DataDefinition mem = new DataDefinition(last_element);
		List<ObjectDefinition> best_definitions = mem.decompose();

		//Q-LEARNING VARIABLES
		final double learning_rate = .1;
		final double discount_factor = .1;
		double estimated_reward = 1.0;
		QLearn q_learn = new QLearn(learning_rate, discount_factor);
		
		//Reinforce probabilities for the component objects of this element if
		for(ObjectDefinition objDef : best_definitions){
			//find objDef in memory. If it exists then use value for memory, otherwise choose random value
			Iterable<com.tinkerpop.blueprints.Vertex> memory_vertex_iter = persistor.find(objDef);
			Iterator<com.tinkerpop.blueprints.Vertex> memory_iterator = memory_vertex_iter.iterator();

			Page current_page = new Page(browser.getDriver(), DateFormat.getDateInstance());
			com.tinkerpop.blueprints.Vertex v = null;
			if(memory_iterator.hasNext()){
				while(memory_iterator.hasNext()){
					//System.err.println(this.getName() + " -> Getting memory vertex");
					v = memory_iterator.next();
					retrieve_learn_update(v, current_page, last_page, q_learn, estimated_reward);
				}
			}
			else{
				v = persistor.addVertex(objDef);
				v.setProperty("value", objDef.getValue());
				v.setProperty("type", objDef.getType());
				retrieve_learn_update(v, current_page, last_page, q_learn, estimated_reward);
			}
		}
	}
	
	/**
	 * Finds the object in memory, reward is determined and learned from and object is put back into memory.
	 * 
	 * @param v
	 * @param current_obj
	 * @param goal_obj
	 * @param q_learn
	 * @return
	 */
	public double retrieve_learn_update(com.tinkerpop.blueprints.Vertex v, Object current_obj, Object goal_obj, QLearn q_learn, double estimated_reward){
		double old_value = 0.0;
		double reward = 0.0;
		double q_learn_val = 0.0;
		
		try{
			old_value = v.getProperty("probability");
		}catch(NullPointerException e){}
								
		if(current_obj.getClass().equals(goal_obj.getClass())){
			if(!current_obj.equals(goal_obj)){
				System.out.println(this.getName() + " -> OBJECT CLASSES DO NOT MATCH!!!!!");
				reward = 1;
			}

			q_learn_val = q_learn.calculate(old_value, reward, estimated_reward);
			//System.err.println(this.getName() + " --> Q Learn value :: " + q_learn_val);
			v.setProperty("probability", q_learn_val);
			persistor.graph.commit();
		}
		
		return q_learn_val;
	}
	
	/**
	 * 
	 * @param size
	 * @return
	 */
	public int getRandomElementIndex(int size){
		return rand.nextInt(size);
	}
	
	/**
	 * Calculates the rewards
	 * 
	 * @param action_rewards
	 * @param pageElement
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public void calculateActionProbabilities(double[] action_rewards, PageElement pageElement) throws IllegalArgumentException, IllegalAccessException{
		DataDefinition data = new DataDefinition(pageElement);
		List<ObjectDefinition> definitions = data.decompose();
		for(ObjectDefinition obj : definitions){
			Iterable<com.tinkerpop.blueprints.Vertex> memory_vertex_iter = persistor.find(obj);
			Iterator<com.tinkerpop.blueprints.Vertex> memory_iterator = memory_vertex_iter.iterator();
			
			while(memory_iterator.hasNext()){
				Iterator<Edge> edges = memory_iterator.next().getEdges(Direction.OUT).iterator();
				int action_idx = 0;
				while(edges.hasNext()){
					Edge edge = edges.next();
					
					String edgeLabel = edge.getLabel();
					double edgeValue = edge.getProperty("probability");
					action_idx = Arrays.binarySearch(ActionFactory.getActions(), edgeLabel);
					
					action_rewards[action_idx] = (action_rewards[action_idx] + edgeValue)/2.0;
				}
			}
		}
	}
	
	/**
	 * 
	 * @param element_probabilities
	 * @return
	 */
	public int getBestIndex(double[] element_probabilities){
		double p = -1.0;
		int idx = 0;
		int best_idx = 0;
		for(double prob : element_probabilities){
			if(prob > p){
				p = prob;
				best_idx = idx;
			}
			idx++;
		}
		System.out.println("BEST PROBABILITY :: " + p);
		return best_idx;
	}
	
	/**
	 * Finds index with highest value in given array
	 * 
	 * @param page
	 * @param element_probabilities
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public void getElementOfBestEstimatedIndex(ArrayList<PageElement> pageElements, double[] element_probabilities) throws IllegalArgumentException, IllegalAccessException{

		int elementIdx = 0;
		for(PageElement elem : pageElements){
			//find vertex for given element
			DataDefinition mem = new DataDefinition(elem);
			List<ObjectDefinition> raw_object_definitions = mem.decompose();
			
			int total_object_definitions = 0;
			double cumulative_probability = 0.0;
			for(ObjectDefinition objDef : raw_object_definitions){
				//find objDef in memory. If it exists then use value for memory, otherwise choose random value
				Iterable<com.tinkerpop.blueprints.Vertex> memory_vertex_iter = persistor.find(objDef);
				Iterator<com.tinkerpop.blueprints.Vertex> memory_iterator = memory_vertex_iter.iterator();
				
				int total_objects = 0;
				double cumulative_value = 0.0;
				while(memory_iterator.hasNext()){
					com.tinkerpop.blueprints.Vertex v = memory_iterator.next();
					
					double value = 0.0;
					try{
						value = v.getProperty("probability");
					}catch(NullPointerException e){
						v.setProperty("probability", .05);
						persistor.graph.commit();
						value = 0.05;
					}
					cumulative_value += value;
					total_objects++;
				}
				
				double probability = 0.0;
				if(total_objects > 0){
					probability = cumulative_value/(double)total_objects;
				}

				objDef.setProbability(probability);						
				
				total_object_definitions++;
				cumulative_probability += probability;
				
			}
			if(total_object_definitions > 0){
				element_probabilities[elementIdx] = cumulative_probability/(double)total_object_definitions;
			}
			System.err.println(this.getName() + " -> Object Definition probability :: "+element_probabilities[elementIdx]);
			//if vertex exists then
			if(raw_object_definitions.size() >= 0){
				System.err.println("RAW OBJECTS EXISTED in memory and WERE loaded");
				// check how likely it is that a vertex leads toward a goal
				// if it is better, then save new highest valued vertex for later, and continue
			}else{
				System.err.println("Raw objects did not exist in memory. OMG I'VE NEVER SEEN THIS BEFORE! WHAT DO I DO!?!");
				//generate random probability that given element leads toward a goal state
				//if probability that element is better than best known value then 
					//set element as best and continue
				element_probabilities[elementIdx] = rand.nextDouble();
				
			}
				
			elementIdx++;
		}
	}
	
	/**
	 * Adds the given {@link Vertex vertex} to the queue
	 * 
	 * @param path path to be added
	 * @pre path != null
	 */
	private ConcurrentLinkedQueue<Path> putPathOnQueue(Path path){
		assert path != null;
		return queueHash.put(path.getCost(this.graph), path);
	}
	
	
	/**
	 * Get the UUID for this Agent
	 */
	public UUID getActorId(){
		return uuid;
	}
	
	/**
	 * Executes the given {@link ElementAction element action} pair such that
	 * the action is executed against the element 
	 * 
	 * @param elemAction ElementAction pair
	 * @return whether action was able to be performed on element or not
	 */
	private boolean performAction(PageElement elem, String action) throws UnreachableBrowserException {
		ActionFactory actionFactory = new ActionFactory(this.browser.getDriver());
		boolean wasPerformedSuccessfully = true;
		
		try{
			WebElement element = browser.getDriver().findElement(By.xpath(elem.getXpath()));
			System.err.print(this.getName() + "PERFORMING ACTION .. ");
			actionFactory.execAction(element, action);
			
			System.err.println(this.getName() + " -> Performed action "+ action
					+ " On element with xpath :: "+elem.getXpath());
		}
		catch(StaleElementReferenceException e){
			
			 	System.out.println(this.getName()
					+ " :: STALE ELEMENT REFERENCE EXCEPTION OCCURRED WHILE ACTOR WAS PERFORMING ACTION : "
					+ action + ". ");
			//e.printStackTrace();
			wasPerformedSuccessfully = false;			
		}
		/*catch(UnreachableBrowserException e){
			System.err.println(this.getName() + " :: Browser is unreachable.");
			wasPerformedSuccessfully = false;
		}*/
		catch(ElementNotVisibleException e){
			System.out.println(this.getName() + " :: ELEMENT IS NOT CURRENTLY VISIBLE.");
		}
		catch(NoSuchElementException e){
			//System.err.println(this.getName() + " -> NO SUCH ELEMENT EXCEPTION");
			wasPerformedSuccessfully = false;
		}
		
		return wasPerformedSuccessfully;
	}
	
	/**
	 * 
	 * @return
	 */
	private Vertex<?> getLastPageVertex(){
		for(int i = this.path.getPath().size()-1; i >= 0; i--){
			 Vertex<?> descNode = graph.getVertices().get(this.path.getPath().get(i));
			System.out.println("VERTEX CLASS  : " + descNode.getData().getClass()  );
			if(descNode.getData() instanceof Page){
				return descNode;
			}
		}
		return null;
	}
}