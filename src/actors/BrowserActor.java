package actors;
import graph.Graph;
import graph.Vertex;

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import learning.QLearn;
import memory.DataDecomposer;
import memory.MemoryState;
import memory.ObjectDefinition;
import memory.Persistor;
import memory.Vocabulary;
import observableStructs.ObservableHash;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.UnreachableBrowserException;

import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
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
	private String url = null;
	private ObservableHash<Integer, Path> queueHash = null;
	private Graph graph = null;
	private Path path = null;
	private Browser browser = null;
	private ResourceManagementActor resourceManager = null;
	private WorkAllocationActor workAllocator = null;
	private Persistor persistor = new Persistor();
	private ArrayList<Vocabulary> vocabularies = null;
	
	//temporary list for vocab labels into it can be determined how best to handle them
		private String[] vocabLabels = {"html"};
	//SHOULD BE CHANGED!!!
	
	/**
	 * 
	 * @param url
	 * @throws IOException 
	 */
	public BrowserActor(String url) throws IOException {
		this.url = url;
		browser = new Browser(url);
		this.path = new Path();
	}

	/**
	 * Creates instance of BrowserActor with given url for entry into website
	 * 
	 * @param url	url of page to be accessed
	 * @param queue observable path queue
	 * @throws IOException 
	 * @pre queue != null
	 * @pre !queue.isEmpty()
	 */
	public BrowserActor(String url, 
						Path path,
						ObservableHash<Integer, Path> path_queue,
						Graph graph,
						ResourceManagementActor resourceManager, 
						WorkAllocationActor workAllocator) throws IOException {
		assert(path_queue != null);
		assert(path_queue.isEmpty());
		
		this.uuid = UUID.randomUUID();
		this.url = url;
		this.path = Path.clone(path);
		this.browser = new Browser(url);
		this.queueHash = path_queue;
		this.graph = graph;
		this.resourceManager = resourceManager;
		this.workAllocator = workAllocator;
		this.vocabularies = this.loadVocabularies(vocabLabels);
	}
	
	/**
	 * Creates instance of browserActor with existing path to crawl.
	 * 
	 * @param queue ovservable path queue
	 * @param path	path to use to navigate to desired page
	 * @throws IOException 
	 * @pre queue != null
	 * @pre !queue.isEmpty()
	 */
	public BrowserActor(ObservableHash<Integer, Path> queue, 
						Graph graph, 
						Path path, 
						ResourceManagementActor resourceManager, 
						WorkAllocationActor workAllocator) throws IOException {
		assert(queue != null);
		assert(queue.isEmpty());
		
		this.uuid = UUID.randomUUID();
		this.graph = graph;
		Vertex<?> node = graph.getVertices().get(path.getPath().get(0));
		assert(((Page)node.getData()).getUrl() != null);
		this.path = Path.clone(path);
		this.url = ((Page)node.getData()).getUrl().toString();
		
		//System.out.println(this.getName() + " BROWSER ACTOR :: PATH HAS "+ path.getPath().size() + " NODES IN PATH");
		browser = new Browser(url);

		this.queueHash = queue;
		this.resourceManager = resourceManager;
		this.workAllocator = workAllocator;
		this.vocabularies = this.loadVocabularies(vocabLabels);
	}

	/**
	 * This method will open a firefox browser and load the url that was given at instantiation.
	 *  The actor will load the page into memory, access the element it needs, and then perform an action on it.
	 */
	public void run() {
		//inform resource manager that worker is running
		resourceManager.punchIn(this);
		try{
			do{
				long tStart = System.currentTimeMillis();
				if(this.path.getPath().isEmpty()){
					System.out.println(this.getName() + " -> Path is empty. Adding to path");
					Vertex<?> vertex = new Vertex<Page>(browser.getPage());
					graph.addVertex(vertex);
					int vertex_idx = graph.findVertexIndex(vertex);
					//need to add edge to vertex
					this.path.add(vertex_idx);
					putPathOnQueue(path);
				}
				else{
					System.out.println(this.getName() + " -> PATH IS NOT EMPTY. Working on path.");
					this.url = ((Page)(graph.getVertices().get(this.path.getPath().get(0))).getData()).getUrl().toString();
					System.out.println(this.getName() + " -> NEW URL :: " + this.url);
					browser.getDriver().get(this.url);
				}
			
				boolean successfulCrawl = crawlPath();
				System.out.println("Successful crawl : "+ successfulCrawl);
				
				int length_limit = 0;
				do{
					System.out.println(this.getName() + " EXPANDING NODE...");
					try {
						Path path = expandNodePath();
						if(path != null){
							System.out.println(this.getName() + " -> EXPANDED PATH IS DEFINED AS :: " + path.getPath());
							this.path = path;
							
							//Get last page, element, action sequence.
							String last_action = null;
							PageElement last_element = null;
							Page last_page = null;
							Iterator<Integer> pathIterator = path.getPath().iterator();
							while(pathIterator.hasNext() 
									&& (last_action == null 
									|| last_element == null 
									|| last_page == null)){
								Vertex<?> vertex = graph.getVertices().get(pathIterator.next());
								if(last_action == null && vertex.getData() instanceof String){
									//get last action
									last_action = (String) vertex.getData();
								}
								else if(last_element == null && vertex.getData() instanceof PageElement){
									//get last element
									last_element = (PageElement)vertex.getData();
								}
								else if(last_page == null && vertex.getData() instanceof Page){
									//get last page
									last_page = (Page)vertex.getData();
								}
							}	
							
							learn(last_page, last_element, last_action);
						}					
					} catch (IllegalArgumentException e) {
						System.err.println(this.getName() + " -> Error Reading Record");
						//e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
					
					long tEnd = System.currentTimeMillis();
					long tDelta = tEnd - tStart;
					double elapsedSeconds = tDelta / 1000.0;
					
					System.out.println(this.getName() + " ----- ELAPSED TIME RUNNING CRAWLER THROUGH CRAWL AND EXPANSION :: "
							+ elapsedSeconds + "-----");
					System.out.println(this.getName() + " #######################################################");
					this.path = workAllocator.retrieveNextPath();
						System.out.println(this.getName() + " -> PATH ARRAY IS DEFINED AS :: " + path.getPath());
					
						for(int idx : path.getPath()){
							System.out.print(graph.getVertices().get(idx).getData().getClass().getCanonicalName() + " , ");
						}
						System.out.println(this.getName() + " -> PATH RETRIEVED.");
					
					//close all windows opened during crawl
					String baseWindowHdl = browser.getDriver().getWindowHandle();
					Set<String> handles = browser.getDriver().getWindowHandles();
					if(handles.size() > 1){
						for(String winHandle : handles){
							browser.getDriver().switchTo().window(winHandle);
							browser.getDriver().close();
							browser.getDriver().switchTo().window(baseWindowHdl);
							System.out.println(this.getName() + " -> CLOSED POPUP WINDOW.");
						}						
					}
					
					length_limit++;
				}while(length_limit < 2);
				
				path = Path.clone(workAllocator.retrieveNextPath());
			}while(true);
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
		} catch (java.util.NoSuchElementException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
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
	 * @throws IOException 
	 */
	private boolean crawlPath() throws java.util.NoSuchElementException, UnhandledAlertException, IOException{
		Iterator<Integer> pathIterator = this.path.getPath().iterator();
		
		//Page pageNode = null;
		PageElement last_element = null;
		//skip first node since we should have already loaded it during initialization
	
		while(pathIterator.hasNext()){
			int path_node_index = pathIterator.next();
			Vertex<?> pathNode = graph.getVertices().get(path_node_index);
						
			if(pathNode.getData() instanceof Page){
				//pageNode = (Page)pathNode.getData();
				//if current page does not match current node data 
			}
			else if(pathNode.getData() instanceof PageElement){
				last_element = (PageElement) pathNode.getData();
			}
			//String is action in this context
			else if(pathNode.getData() instanceof String){
				boolean actionPerformedSuccessfully;
				String action = (String) pathNode.getData();
				browser.updatePage( DateFormat.getDateInstance());
				int attempts = 0;
				do{
					actionPerformedSuccessfully = performAction(last_element, action );
					attempts++;
				}while(!actionPerformedSuccessfully && attempts < 50);
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
		System.out.println(this.getName() + " -> EXPANDING NODE");
		Path new_path = null;
		Vertex<?> page_vertex = getLastPageVertex();
		if(page_vertex == null){
			return null;
		}
		Class<?> className = page_vertex.getData().getClass();
		String[] actions = ActionFactory.getActions();
		
		if(className.equals(Page.class)){
			Page page = ((Page)page_vertex.getData());
			PageElement chosen_pageElement = null;
			String chosen_action = null;
			double exploration_coef = rand.nextDouble();
			System.err.println("EXPLORATION COEFFICIENT :: "+ exploration_coef);
			
			if(exploration_coef > 0.7){
				System.out.println("&&&&&  GETTING BEST ELEMENT ACTION PAIR &&&&&&&&&");
				
				//get index of best estimated element
				ArrayList<HashMap<String, Double>> estimated_probs = getEstimatedElementProbabilities(page.getElements());
				System.out.println("TOTAL ELEMENTS :: "+page.getElements().size());
				System.out.println("TOTAL PROBAILITIES :: " + estimated_probs.size());
				int idx = getBestIndex(estimated_probs);
				System.out.println("BEST IDX :: " + idx);
				chosen_pageElement = page.getElements().get(idx);
						
				//get best known action given best chosen element
				HashMap<String, Double> action_rewards = calculateActionProbabilities(chosen_pageElement);
				chosen_action = getBestAction(action_rewards);
			}
			else if(exploration_coef > .35){
				//get index of best estimated element
				ArrayList<HashMap<String, Double>> element_probabilities = getEstimatedElementProbabilities(page.getElements());
				System.out.println(this.getName() + " -> ESTIMATED PROBABILITIES LOADED");
				int random_index = rand.nextInt(element_probabilities.size()/2);
				
				chosen_pageElement = page.getElements().get(random_index);
						
				//get best known action given best chosen element
				HashMap<String, Double> action_rewards = calculateActionProbabilities(chosen_pageElement);
				chosen_action = getBestAction(action_rewards);
			}
			//create another else if that explores the top 10 when confidence is between .4 and .7
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
			
			//ENSURE THAT PAGE_ELEMNT VERTEX AND ACTION HAVEN'T BEEN PERFORMED TOGETHER BEFORE IN SEQUENCE
			new_path = Path.clone(path);
			boolean alreadySeen = false;
			for(int i =0; i < new_path.getPath().size()-1; i++){
				if(new_path.getPath().get(i)==graph.findVertexIndex(pageElementVertex)
					&& new_path.getPath().get(i+1)==graph.findVertexIndex(actionVertex)){
					System.out.println("VERTEX AND ACTION COMBO HAVE ALREADY BEEN DONE.");
					alreadySeen = true;
				}
			}
			/*if(alreadySeen){
				//DECIDE ON IF I SHOULD DO IT ANYWAY. FOR NOW DON'T BOTHER DECIDING, JUST SKIP

				return null;
			}*/

			//Add element and action vertices to path graph
			if(pageElementVertex != null){
				graph.addVertex(pageElementVertex);
				graph.addEdge(page_vertex, pageElementVertex);
				graph.addVertex(actionVertex);
				graph.addEdge(pageElementVertex, actionVertex);
			}

			int page_elem_vertex_idx = graph.findVertexIndex(pageElementVertex);
			int action_vertex_idx = graph.findVertexIndex(actionVertex);

			new_path.add(page_elem_vertex_idx);
			new_path.add(action_vertex_idx);
			putPathOnQueue(new_path);
		}
		else if(className.equals(PageAlert.class)){
			System.err.println(this.getName() + " -> Handling Alert from expanding Node.");
			PageAlert alert = (PageAlert)page_vertex.getData();
			alert.performChoice(browser.getDriver());
		}
		
		return new_path;
	}
	
	/**
	 * Reads path and performs learning tasks
	 * 
	 * @param path an {@link ArrayList} of graph vertex indices. Order matters
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws NullPointerException
	 * @throws IOException 
	 */
	public void learn(Page last_page, PageElement last_element, String last_action) throws IllegalArgumentException, IllegalAccessException, NullPointerException, IOException{
		//REINFORCEMENT LEARNING
		System.out.println(this.getName() + " -> Initiating learning");

		MemoryState memState = new MemoryState(last_page.hashCode());
		com.tinkerpop.blueprints.Vertex state_vertex = null;
		try{
			state_vertex = memState.createAndLoadState(last_page, null, persistor);
		}catch(IllegalArgumentException e){}
		Page current_page = browser.getPage();

		double actual_reward = 0.0;
	
		if(!last_page.equals(current_page)){
			actual_reward = 1.0;
			
			com.tinkerpop.blueprints.Vertex new_state_vertex = null;
			MemoryState new_memory_state = new MemoryState(current_page.hashCode());
			
			new_state_vertex = new_memory_state.createAndLoadState(current_page, state_vertex, persistor);

			//add it to in memory map. This should be changed to use some sort of caching
			Vertex<?> vertex = new Vertex<Page>(current_page);
			graph.addVertex(vertex);
			int idx = graph.findVertexIndex(vertex);
			path.add(idx);
			
			putPathOnQueue(path);
			//add new edge to memory
			
			if(!state_vertex.equals(new_state_vertex)){
				System.out.println("Adding GOES_TO transition");
				Edge e = persistor.addEdge(state_vertex, new_state_vertex, "TRANSITION", "GOES_TO");
				e.setProperty("action", last_action);
				e.setProperty("xpath", last_element.xpath);
			}
			System.err.println("SAVING NOW...");
			persistor.save();
		}
		else{
			//nothing changed so there was no reward for that combination. We want to remember this in the future
			// so we set it to a negative value to simulate regret
			actual_reward = -1.0;
		}
		
		//get all objects for the chosen page_element
		DataDecomposer mem = new DataDecomposer(last_element);
		List<ObjectDefinition> best_definitions = mem.decompose();
		System.err.println("TOTAL BEST DEFINTIONS :: " + best_definitions.size());
		//Q-LEARNING VARIABLES
		final double learning_rate = .08;
		final double discount_factor = .08;
		
		//machine learning algorithm should produce this value
		double estimated_reward = 1.0;
		
		QLearn q_learn = new QLearn(learning_rate, discount_factor);
		double computed_actual_reward = actual_reward;
		//Reinforce probabilities for the component objects of this element
		for(ObjectDefinition objDef : best_definitions){
			HashMap<String, Double> action_map = objDef.getActions();
			
			//NEED TO LOOK UP OBJECT DEFINITION IN MEMORY, IF IT EXISTS, THEN IT SHOULD BE LOADED AND USED, 
			//IF NOT THEN IT SHOULD BE CREATED POPULATED AND SAVED
			Iterator<com.tinkerpop.blueprints.Vertex> v_mem_iter = persistor.find(objDef).iterator();
			com.tinkerpop.blueprints.Vertex memory_vertex = null;
			if(v_mem_iter.hasNext()){
				memory_vertex = v_mem_iter.next();
				action_map = memory_vertex.getProperty("actions");
				if(action_map == null){
					action_map = objDef.getActions();
				}
			}
			double last_reward = 0.0;

			if(action_map.containsKey(last_action)){
				System.out.println("Last action : "+last_action + " exists in action_map for object");
				last_reward = action_map.get(last_action);
			}
			
			System.err.println("last reward : "+last_reward);
			System.err.println("actual_reward : "+actual_reward);
			System.err.println("estimated_reward : "+estimated_reward);
			
			double q_learn_val = q_learn.calculate(last_reward, actual_reward, estimated_reward );
			action_map.put(last_action, q_learn_val);
			System.err.println(this.getName() + " -> ADDED LAST ACTION TO ACTION MAP :: "+last_action+"...Q LEARN VAL : "+q_learn_val);

			
			objDef.setActions(action_map);
			com.tinkerpop.blueprints.Vertex v = objDef.findAndUpdateOrCreate(persistor);
		}
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
	public HashMap<String, Double> calculateActionProbabilities(PageElement pageElement) throws IllegalArgumentException, IllegalAccessException{
		DataDecomposer data = new DataDecomposer(pageElement);
		List<ObjectDefinition> definitions = data.decompose();
		System.out.println(this.getName() + " -> GETTING BEST ACTION PROBABILITY...");
		HashMap<String, Double> cumulative_action_map = new HashMap<String, Double>();
		
		for(ObjectDefinition obj : definitions){
			Iterable<com.tinkerpop.blueprints.Vertex> memory_vertex_iter = persistor.find(obj);
			Iterator<com.tinkerpop.blueprints.Vertex> memory_iterator = memory_vertex_iter.iterator();
			
			while(memory_iterator.hasNext()){
				com.tinkerpop.blueprints.Vertex mem_vertex = memory_iterator.next();
				HashMap<String, Double> action_map = mem_vertex.getProperty("actions");
				double probability = 0.0;
				if(action_map != null){
					for(String action: action_map.keySet()){
						if(cumulative_action_map.containsKey(action)){
							probability += cumulative_action_map.get(action);
						}
						
						cumulative_action_map.put(action, probability);
					}
				}
				else{
					for(String action: pageElement.getActions()){						
						cumulative_action_map.put(action, probability);
					}
				}
			}
		}
		return cumulative_action_map;
	}
	
	/**
	 * 
	 * @param element_probabilities
	 * @return
	 */
	public int getBestIndex(double[] element_probabilities){
		double p = -1.0;
		int idx = 0;
		int best_idx = -1;
		for(double prob : element_probabilities){
			if(prob > p){
				p = prob;
				best_idx = idx;
			}
			idx++;
		}
		System.out.println("BEST PROBABILITY :: " + p + " at index "+best_idx);
		return best_idx;
	}

	/**
	 * 
	 * @param element_probabilities
	 * @return
	 */
	public String getBestAction(HashMap<String, Double> action_probabilities){
		double p = -1.0;
		String best_action = "click";
		for(String action : action_probabilities.keySet()){
			double action_prob = action_probabilities.get(action);
			if(action_prob > p){
				p = action_prob;
				best_action = action;
			}
		}
		System.out.println("BEST PROBABILITY :: " + p);
		return best_action;
	}
	
	/**
	 * Gets the index for the element that has an action probability of state change greater than the rest
	 * 
	 * @param element_probabilities
	 * @return
	 */
	public int getBestIndex(ArrayList<HashMap<String, Double>> element_probabilities){
		double p = -1.0;
		int idx = 0;
		int best_idx = 0;
		
		for(HashMap<String, Double> action_map : element_probabilities){
			for(String action: action_map.keySet()){
				if(action_map.get(action) > p){
					p = action_map.get(action);
					best_idx = idx;
				}
			}
			idx++;
		}
		System.out.println("BEST PROBABILITY :: " + p + " at index "+best_idx);
		return best_idx;
	}
	
	/**
	 * Calculate all estimated element probabilities
	 * 
	 * @param page
	 * @param element_probabilities
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public ArrayList<HashMap<String, Double>> getEstimatedElementProbabilities(ArrayList<PageElement> pageElements) throws IllegalArgumentException, IllegalAccessException{
		ArrayList<HashMap<String, Double>> element_action_map_list = new ArrayList<HashMap<String, Double>>(0);
				
		for(PageElement elem : pageElements){
			HashMap<String, Double> full_action_map = new HashMap<String, Double>(0);
			//find vertex for given element
			DataDecomposer mem = new DataDecomposer(elem);
			List<ObjectDefinition> raw_object_definitions = mem.decompose();
			List<com.tinkerpop.blueprints.Vertex> object_definition_list
				= ObjectDefinition.findAll(raw_object_definitions, persistor);
					
			//iterate over set to get all actions for object definition list
			for(com.tinkerpop.blueprints.Vertex v : object_definition_list){
				HashMap<String, Double> action_map = v.getProperty("actions");
				if(action_map != null && !action_map.isEmpty()){
					for(String action : action_map.keySet()){
						if(!full_action_map.containsKey(action)){
							//If it doesn't yet exist, then seed it with a random variable
							full_action_map.put(action, rand.nextDouble());
						}
						else{
							
							double action_sum = full_action_map.get(action) + action_map.get(action);
							full_action_map.put(action, action_sum);
						}
					}
				}
			}
			
			for(String action : full_action_map.keySet()){
				double probability = 0.0;
				probability = full_action_map.get(action)/(double)object_definition_list.size();

				//cumulative_probability[action_idx] += probability;
				full_action_map.put(action, probability);
			}
			element_action_map_list.add(full_action_map);
		}
		
		return element_action_map_list;
	}

	
	/**
	 * Calculate all estimated element probabilities
	 * 
	 * @param page
	 * @param element_probabilities
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public void getEstimatedElementProbabilities(ArrayList<PageElement> pageElements, double[] element_probabilities) throws IllegalArgumentException, IllegalAccessException{
		int elementIdx = 0;
		
		for(PageElement elem : pageElements){
			//find vertex for given element
			DataDecomposer mem = new DataDecomposer(elem);
		
			List<ObjectDefinition> raw_object_definitions = mem.decompose();
			List<com.tinkerpop.blueprints.Vertex> object_definition_list
				= ObjectDefinition.findAll(raw_object_definitions, persistor);
			
			int total_object_definitions = 0;
			double cumulative_probability = 0.0;
			for(com.tinkerpop.blueprints.Vertex v : object_definition_list){
				int total_objects = 0;
				double cumulative_value = 0.0;

					double value = 0.0;

					//try{
						persistor.save();
					//}catch(OConcurrentModificationException e1){
					//	ObjectDefinition obj = new ObjectDefinition((Integer)v.getProperty("identifier"), v.getProperty("value").toString(), v.getProperty("type").toString());
					//	persistor.find(obj);
						//System.out.println(this.getName() + " -> OBJECT :: "+obj.getValue()+ " : "+obj.getType()+" -> HAS BEEN FOUND");
					//	persistor.save();
					//}
					cumulative_value += value;
					total_objects++;
				
				double probability = 0.0;
				if(total_objects > 0){
					probability = cumulative_value/(double)total_objects;
				}
				
				total_object_definitions++;
				cumulative_probability += probability;
			}
			if(total_object_definitions > 0){
				element_probabilities[elementIdx] = cumulative_probability/(double)total_object_definitions;
			}
			else{
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
		
		//Cost is divided by 3 because we only care about an action which is always between a 
		// page element vertex and page vertex
		int cost = path.calculateCost(this.graph)/3;
		int actualReward = path.getActualReward(this.graph);

		int value = actualReward - cost;
		System.out.println("THE VALUE OF THE PATH IS :: "+value+ " ;       COST : "+cost);
		if(value <= 0){
			//put path on queue for bad paths
			return null;
		}
		//Ensure that path is not already in queue
		boolean pathsMatch = true;
		try{
			for(Path queue_path : queueHash.getQueueHash().get(value)){
				if(queue_path.getPath().size() == path.getPath().size()){
					for(int idx=0; idx < path.getPath().size(); idx++){
						if(path.getPath().get(idx) != queue_path.getPath().get(idx)){
							pathsMatch = false;
						}
					}
				}
			}
		}catch(NullPointerException e){}
		
		if(pathsMatch){
			return queueHash.put(value, path);
		}
		else{
			return null;
		}
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
			
			 	System.err.println(this.getName()
					+ " :: STALE ELEMENT REFERENCE EXCEPTION OCCURRED WHILE ACTOR WAS PERFORMING ACTION : "
					+ action + ". ");
			//e.printStackTrace();
			wasPerformedSuccessfully = false;			
		}
		catch(ElementNotVisibleException e){
			System.err.println(this.getName() + " :: ELEMENT IS NOT CURRENTLY VISIBLE.");
		}
		catch(NoSuchElementException e){
			System.err.println(this.getName() + " -> NO SUCH ELEMENT EXCEPTION WHILE PERFORMING "+action);
			wasPerformedSuccessfully = false;
		}
		
		return wasPerformedSuccessfully;
	}
	
	/**
	 * Retrieves all {@linkplain Vocabulary vocabularies} that are required by the agent 
	 * 
	 * @param vocabLabels
	 * @return
	 */
	public ArrayList<Vocabulary> loadVocabularies(String[] vocabLabels){
		ArrayList<Vocabulary> vocabularies = new ArrayList<Vocabulary>();
		for(String label : vocabLabels){			
			vocabularies.add(Vocabulary.load(label));
		}
		return vocabularies;		
	}
	
	/**
	 * Gets the last Vertex in a path that is of type {@link Page}
	 * @return
	 */
	private Vertex<?> getLastPageVertex(){
		for(int i = this.path.getPath().size()-1; i >= 0; i--){
			Vertex<?> descNode = graph.getVertices().get(this.path.getPath().get(i));
			System.out.println(this.getName() + " -> VERTEX CLASS  : " + descNode.getData().getClass()  );
			if(descNode.getData() instanceof Page){
				System.err.println("PAGE VERTEX FOUND AND RETURNED");
				return descNode;
			}
		}
		return null;
	}
}