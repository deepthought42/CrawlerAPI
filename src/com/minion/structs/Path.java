package com.minion.structs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.openqa.selenium.WebDriver;

import com.minion.actors.BrowserActor;
import com.minion.browsing.ActionFactory;
import com.minion.browsing.Browser;
import com.minion.browsing.IObjectValuationAccessor;
import com.minion.browsing.PathObject;
import com.minion.browsing.PathObjectFactory;
import com.minion.browsing.Page;
import com.minion.browsing.PageElement;
import com.minion.browsing.actions.Action;
import com.minion.persistence.IPath;
import com.minion.persistence.IPathObject;
import com.minion.persistence.IPersistable;
import com.minion.persistence.ITest;
import com.minion.persistence.OrientConnectionFactory;

/**
 * A set of vertex objects that form a sequential movement through a graph
 * 
 * @author Brandon Kindred
 *
 */
public class Path implements IPersistable<IPath> {
    private static final Logger log = Logger.getLogger(BrowserActor.class);
	
    private final String key;
	private boolean isUseful;
	private boolean spansMultipleDomains = false;
	public ArrayList<PathObject> path = null;

	/**
	 * Creates new instance of Path
	 */
	public Path(){
		this.isUseful = false;
		this.spansMultipleDomains = false;
		this.path = new ArrayList<PathObject>();
		this.key = this.generateKey();
	}

	/**
	 * Creates new instance of path setting it to the given path
	 * 
	 * @param current_path
	 */
	public Path(Path current_path){
		this.isUseful = false;
		this.path = new ArrayList<PathObject>();
		this.append(current_path);
		this.key = this.generateKey();
	}
	
	/**
	 * Appends all objects the the array containing the known path sequence
	 */
	public void append(Path appendablePath){
		this.path.addAll(this.path.size(), appendablePath.getPath());
		/*for(PathObject obj : appendablePath.getPath()){
			this.path.add(obj);
		}*/
		this.spansMultipleDomains = checkIfSpansMultipleDomains();
	}
		
	/**
	 * Adds an object to path and sets whether or not this path spans multiple domains
	 * 
	 * @param obj
	 * @return
	 */
	public boolean add(PathObject obj){
		boolean added_successfully = this.path.add(obj);
		if(added_successfully){
			this.spansMultipleDomains = checkIfSpansMultipleDomains();
		}
		return added_successfully; 
	}
	
	/**
	 * 
	 * @return
	 */
	public List<PathObject> getPath(){
		return this.path;
	}
	
	public void setIsUseful(boolean isUseful){
		this.isUseful = isUseful;
	}
	
	public boolean isUseful(){
		return this.isUseful;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean equals(Path path){
		int thisPathLength = this.path.size();
		int comparatorPathLength = path.getPath().size();
				
		if(thisPathLength != comparatorPathLength){
			System.out.println("PATHS ARE NOT EQUAL");
			return false;
		}
		for(int i = 0; i < thisPathLength; i++){
			Object thisPathNode = this.path.get(i);
			Object comparatorPathNode = path.getPath().get(i);
			
			if(!thisPathNode.getClass().getCanonicalName().equals(comparatorPathNode.getClass().getCanonicalName())){
				System.out.println("NODE CLASS NAMES ARE NOT EQUAL");
				return false;
			}
			if(thisPathNode != comparatorPathNode){
				System.out.println("NODE DATA NOT EQUAL.");
				return false;
			}
		}
		
		System.out.println("NODE PATHS ARE EQUAL");
		return true;		
	}
	
	/**
	 * Calculates the cost of traversing the path based on the cost of individual vertices within this path
	 * 
	 * @param graph
	 * @return
	 */
	public double calculateCost(){
		int cost=0;
		for(PathObject vertex_obj : this.getPath()){
			cost += ((IObjectValuationAccessor)vertex_obj.data()).getCost();
		}
		
		return cost;
	}
	
	/**
	 * Gets the estimated reward value for this path 
	 * 
	 * @param graph
	 * @return
	 */
	public double calculateReward(){
		int reward = 0;
		for(PathObject vertex_obj : this.getPath()){
			reward += ((IObjectValuationAccessor)vertex_obj.data()).getReward();
		}
		
		return reward;
	}
	
	/**
	 * Clone {@link Path} object
	 * 
	 * @param path
	 * @return
	 */
	public static Path clone(Path path){
		Path clonePath = new Path();
		
		for(PathObject obj : path.getPath()){
			clonePath.add(obj);
		}
		
		return clonePath;
	}
	
	/**
	 * Gets the last Vertex in a path that is of type {@link Page}
	 * 
	 * @return
	 */
	public Page getLastPage(){
		for(int i = this.path.size()-1; i >= 0; i--){
			PathObject descNode = this.path.get(i);
			if(descNode.data() instanceof Page){
				return (Page)descNode.data();
			}
		}
		return null;
	}
	
	/**
	 * Produces all possible element, action combinations that can be produced from the given path
	 * 
	 * @throws MalformedURLException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public static ArrayList<Path> expandPath(Path path)  {
		log.info( " EXPANDING PATH...");
		ArrayList<Path> pathList = new ArrayList<Path>();
		//Path new_path = Path.clone(path);
		
		Page page = path.getLastPage();
		if(page == null){
			return null;
		}
		//get last page
		String[] actions = ActionFactory.getActions();
		
		//get all elements for this page
		WebDriver webdriver = null;
		try {
			webdriver = new Browser(page.getUrl().toString()).getDriver();
		} catch (IOException e) {
			e.printStackTrace();
		}
		List<PageElement> page_elements = Page.getVisibleElements(webdriver, "");
	
		//iterate over all elements
		for(PageElement page_element : page_elements){
			//iterate over all actions
			for(String action : actions){
				Path action_path = Path.clone(path);
				Action action_obj = new Action(action);
								
				action_path.add(page_element);
				action_path.add(action_obj);
				pathList.add(action_path);
			}			
		}
		
		return pathList;
	}
	
	/**
	 * Checks if the path has 2 sequential elements that appear in more than 1 location
	 * 
	 * @param path
	 * @return true if sequence appears more than once
	 */
	public static boolean hasCycle(Path path){
		if(path.getPath().size() == 1){
			return false;
		}
		
		for(int i = path.getPath().size()-1; i > 0; i--){
			for(int j = i-1; j>= 0; j--){
				if(path.getPath().get(i).equals(path.getPath().get(j)) 
					&& path.getPath().get(i-1).equals(path.getPath().get(j-1))){
					return true;
				}
			}			
		}
		return false;
	}
	
	/**
	 * Checks if the path has the same page more than once. 
	 * 
	 * @param path
	 * @return true if sequence appears more than once
	 */
	public static boolean hasPageCycle(Path path){
		for(int i = path.getPath().size()-1; i > 0; i--){
			for(int j = i-1; j>= 0; j--){
				if(path.getPath().get(i).data() instanceof Page 
						&& path.getPath().get(j).data() instanceof Page
						&& path.getPath().get(i).data().equals(path.getPath().get(j).data()))
				{
					return true;
				}
			}			
		}
		return false;
	}

	public boolean isSpansMultipleDomains() {
		return spansMultipleDomains;
	}

	private boolean checkIfSpansMultipleDomains() {
		log.info("Checking if path spans multiple domains");
		if(path.size() > 1 && !((Page)path.get(0).data()).getUrl().toString().contains(this.getLastPage().getUrl().getHost())){
			log.info("Path leaves original domain");
			return true;
		}
		return false;
	}

	public IPath convertToRecord(OrientConnectionFactory connection) {
		IPath path = connection.getTransaction().addVertex(UUID.randomUUID(), IPath.class);
		path.setKey(key);
		
		log.info("Starting conversion from path objects to their respective types");
		boolean first_pass = true;
		IPathObject persistablePathObj = connection.getTransaction().addVertex(UUID.randomUUID(), IPathObject.class);

		IPathObject last_obj = persistablePathObj;
		for(PathObject pathObj : this.getPath()){
			last_obj.setData(PathObjectFactory.build(pathObj).convertToRecord(connection));
			
			if(first_pass){
				path.setPath(persistablePathObj);
				first_pass = false;
			}
			else{
				IPathObject persistablePathObj_new = connection.getTransaction().addVertex(UUID.randomUUID(), IPathObject.class);
				last_obj.setNext(persistablePathObj_new);
				last_obj = persistablePathObj_new;
			}
		}
				
		log.info("Path Size: " + this.getPath().size());
		path.setUsefulness(this.isUseful());
		
		log.info("Is spans multiple domains set : " + this.isSpansMultipleDomains());
		path.setSpansMultipleDomains(this.isSpansMultipleDomains());
		return path;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateKey() {
		String path_key = "";
		for(PathObject path_obj : this.getPath()){
			path_key += ((IPersistable<?>)path_obj.data()).generateKey() + ":"+hashCode()+":";
		}
		return path_key;
	}
		
	public String getKey() {
		return this.key;
	}

	@Override
	public IPersistable<IPath> create() {
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		
		this.convertToRecord(orient_connection);
		orient_connection.save();
		
		return this;
	}

	@Override
	public IPersistable<IPath> update(IPath existing_obj) {
		Iterator<IPath> test_iter = this.findByKey(this.generateKey()).iterator();
		int cnt=0;
		while(test_iter.hasNext()){
			test_iter.next();
			cnt++;
		}
		log.info("# of existing records with key "+this.getKey() + " :: "+cnt);
		
		OrientConnectionFactory connection = new OrientConnectionFactory();
		if(cnt == 0){
			connection.getTransaction().addVertex(UUID.randomUUID(), ITest.class);
			this.convertToRecord(connection);
		}
		
		connection.save();
		
		return this;
	}

	@Override
	public Iterable<IPath> findByKey(String generated_key) {
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		return orient_connection.getTransaction().getVertices("key", generated_key, IPath.class);
	}
}


