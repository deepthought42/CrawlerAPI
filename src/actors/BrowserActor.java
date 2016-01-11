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
import java.util.UUID;

import memory.DataDecomposer;
import memory.ObjectDefinition;
import memory.Persistor;
import memory.Vocabulary;
import observableStructs.ObservableHash;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.UnreachableBrowserException;

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
	private GraphObserver graphObserver = null;
	private Path path = null;
	private Browser browser = null;
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
						Path path) throws IOException {
		
		this.uuid = UUID.randomUUID();
		this.url = url;
		this.path = Path.clone(path);
		this.browser = new Browser(url);
		this.vocabularies = this.loadVocabularies(vocabLabels);
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
						GraphObserver graphObserver) throws IOException {
		
		this.uuid = UUID.randomUUID();
		this.url = url;
		this.path = Path.clone(path);
		this.browser = new Browser(url);
		this.graphObserver = graphObserver;
		this.vocabularies = this.loadVocabularies(vocabLabels);
	}
	
	public BrowserActor(Path path){
		this.path = Path.clone(path);
	}

	/**
	 * Starts thread which either adds a page 
	 */
	public void run(){
		try {
			if(this.path.getPath().isEmpty()){
				Vertex<Page> page_vertex = new Vertex<Page>(browser.getPage());
				this.graphObserver.getGraph().addVertex(page_vertex);
				int vertex_idx = this.graphObserver.getGraph().findVertexIndex(page_vertex);
				this.path.add(vertex_idx);
			}
			else{
				boolean successfulCrawl = this.crawlPath();
			}
		} catch (UnhandledAlertException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (java.util.NoSuchElementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Page current_page = null;
		try {
			current_page = browser.getPage();
			WorkAllocationActor.registerCrawlResult(this.path, (Page)this.path.getLastPageVertex(graphObserver.getGraph()).getData(), current_page, graphObserver);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}		
		
		this.browser.getDriver().quit();
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
			Vertex<?> pathNode = graphObserver.getGraph().getVertices().get(path_node_index);
						
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
	 * Adds the given {@link Vertex vertex} to the queue
	 * 
	 * @param path path to be added
	 * @pre path != null
	 */
	/*private ConcurrentLinkedQueue<Path> putPathOnQueue(Path path){
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
	}*/
	
	
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
	/*
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
	*/
}