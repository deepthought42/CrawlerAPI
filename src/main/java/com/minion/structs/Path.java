package com.minion.structs;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minion.actors.BrowserActor;
import com.minion.browsing.ActionFactory;
import com.minion.browsing.IObjectValuationAccessor;
import com.minion.browsing.PathObject;
import com.minion.browsing.PathObjectFactory;
import com.minion.browsing.Page;
import com.minion.browsing.PageElement;
import com.minion.browsing.actions.Action;
import com.minion.persistence.IPath;
import com.minion.persistence.IPathObject;
import com.minion.persistence.IPersistable;
import com.minion.persistence.OrientConnectionFactory;

/**
 * A set of vertex objects that form a sequential movement through a graph
 * 
 * @author Brandon Kindred
 *
 */

public class Path implements IPersistable<IPath> {
    private static final Logger log = LoggerFactory.getLogger(BrowserActor.class);
	
    private String key;
	private boolean isUseful;
	private boolean spansMultipleDomains = false;
	
	private List<PathObject<?>> path = null;
	
	/**
	 * Creates new instance of Path
	 */
	public Path(){
		this.isUseful = false;
		this.spansMultipleDomains = false;
		this.path = new ArrayList<PathObject<?>>();
		this.key = this.generateKey();
	}

	/**
	 * Creates new instance of path setting it to the given path
	 * 
	 * @param current_path
	 */
	public Path(Path current_path){
		this.isUseful = false;
		this.path = new ArrayList<PathObject<?>>();
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
	public boolean add(PathObject<?> obj){
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
	public List<PathObject<?>> getPath(){
		return this.path;
	}
	
	public void setPath(List<PathObject<?>> path){
		this.path = path;
	}
	
	public void setIsUseful(boolean isUseful){
		this.isUseful = isUseful;
	}
	
	public boolean getIsUseful(){
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
		for(PathObject<?> vertex_obj : this.getPath()){
			cost += ((IObjectValuationAccessor)vertex_obj.getData()).getCost();
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
		for(PathObject<?> vertex_obj : this.getPath()){
			reward += ((IObjectValuationAccessor)vertex_obj.getData()).getReward();
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
		
		for(PathObject<?> obj : path.getPath()){
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
			PathObject<?> descNode = this.path.get(i);
			if(descNode.getData() instanceof Page){
				return (Page)descNode.getData();
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
		/*WebDriver webdriver = null;
		try {
			webdriver = new Browser(page.getUrl().toString()).getDriver();
		} catch (IOException e) {
			e.printStackTrace();
		}*/
		List<PageElement> page_elements = page.getElements();//  .getVisibleElements(webdriver, "");
	
		//iterate over all elements
		for(PageElement page_element : page_elements){
			//iterate over all actions
			for(String action : actions){
				Path action_path = Path.clone(path);
				Action action_obj = new Action(action);
				
				log.info("Constructing path object for expand path");
				action_path.add(new PathObject<PageElement>(page_element));

				action_path.add(new PathObject<Action>(action_obj));
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
				if(path.getPath().get(i).getData() instanceof Page 
						&& path.getPath().get(j).getData() instanceof Page
						&& path.getPath().get(i).getData().equals(path.getPath().get(j).getData()))
				{
					return true;
				}
			}			
		}
		return false;
	}

	public boolean getSpansMultipleDomains() {
		return spansMultipleDomains;
	}
	
	public void setSpansMultipleDomains(boolean isSpanningMultipleDomains) {
		this.spansMultipleDomains= isSpanningMultipleDomains;
	}

	private boolean checkIfSpansMultipleDomains() {
		log.info("Checking if path spans multiple domains");
		if(path.size() > 1 && !((Page)path.get(0).getData()).getUrl().toString().contains(this.getLastPage().getUrl().getHost())){
			log.info("Path leaves original domain");
			return true;
		}
		return false;
	}

	public IPath convertToRecord(OrientConnectionFactory connection) {
		IPath path = connection.getTransaction().addVertex("class:"+IPath.class.getCanonicalName()+","+UUID.randomUUID(), IPath.class);
		path.setKey(key);
		
		log.info("Starting conversion from path objects to their respective types");
		boolean first_pass = true;
		IPathObject persistablePathObj =  connection.getTransaction().addVertex("class:"+IPathObject.class.getCanonicalName()+","+UUID.randomUUID(), IPathObject.class);

		log.info("setting last_obj value to IPathObject");
		log.info("Starting iteration through path objects");
		for(PathObject<?> pathObj : this.getPath()){
			log.info("setting data for last object");
			persistablePathObj.setData(PathObjectFactory.build(pathObj).convertToRecord(connection));
			
			if(first_pass){
				//persistablePathObj.setData(pathObj);
				log.info("First object detected : "+persistablePathObj.getClass());
				//log.info(" : " +persistablePathObj.getData().getClass());
				path.setPath(persistablePathObj);
				
				//path.getPath().add(persistablePathObj);
				log.info("adding object to path");
				first_pass = false;
			}
			else{
				log.info("not first pass detected. getting IPathObject");
				IPathObject persistablePathObj_new = connection.getTransaction().addVertex("class:"+IPathObject.class.getCanonicalName()+","+UUID.randomUUID(), IPathObject.class);
				log.info("setting path object properties");
				persistablePathObj.setData(pathObj);
				log.info("setting next object in path");
				persistablePathObj.setNext(persistablePathObj_new);
				log.info("setting last object to latest path object");
				persistablePathObj = persistablePathObj_new;
			}
		}
				
		log.info("Path Size: " + this.getPath().size());
		path.setUsefulness(this.getIsUseful());
		
		log.info("Is spans multiple domains set : " + this.getSpansMultipleDomains());
		path.setSpansMultipleDomains(this.getSpansMultipleDomains());
		return path;
	}

	/**
	 * {@inheritDoc}
	 */
	public String generateKey() {
		String path_key = "";
		for(PathObject<?> path_obj : this.getPath()){
			path_key += ((IPersistable<?>) path_obj.getData()).generateKey() + ":"+hashCode()+":";
		}
		log.info("path_key generated : "+path_key);
		return path_key;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IPersistable<IPath> create() {
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		log.info("converting path to record");
		this.convertToRecord(orient_connection);
		log.info("path converted. now saving");
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
			connection.getTransaction().addVertex("class:"+IPath.class.getCanonicalName()+","+UUID.randomUUID(), IPath.class);
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

	public void setKey(String key) {
		this.key = key;
		
	}
	
	public String getKey() {
		return this.key;
	}

	public static Path convertFromRecord(IPath ipath) {
		Path path = new Path();
		log.info("converting path record to object");
		path.setIsUseful(ipath.isUseful());
		
		log.info("setting key");
		path.setKey(ipath.getKey());
			
		log.info("setting if spans multiple domains");
		path.setSpansMultipleDomains(ipath.isSpansMultipleDomains());
		
		List<PathObject<?>> path_object_list = new ArrayList<PathObject<?>>();
		
		IPathObject path_obj = ipath.getPath();
		
		log.info("building path object record");
		IPathObject obj = path_obj.getData();
		
		//PathObject obj = PathObjectFactory.buildRecord(path_obj);
		PathObject new_path_obj = new PathObject(obj);
		path_object_list.add(new_path_obj);
		//log.info("Setting path");
		//path.setPath(ipath.getPath());
		
		return path;
	}
}


