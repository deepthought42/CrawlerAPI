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
import com.minion.browsing.actions.Action;
import com.minion.persistence.DataAccessObject;
import com.minion.persistence.IPage;
import com.minion.persistence.IPath;
import com.minion.persistence.IPathObject;
import com.minion.persistence.IPersistable;
import com.minion.persistence.ITest;
import com.minion.persistence.OrientConnectionFactory;
import com.minion.persistence.edges.IPathEdge;
import com.qanairy.models.Page;
import com.qanairy.models.PageElement;
import com.qanairy.models.PathObject;


/**
 * A set of vertex objects that form a sequential movement through a graph
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
	public Path(List<PathObject<?>> current_path){
		this.isUseful = false;
		this.path = current_path;
		this.key = this.generateKey();
	}

		
	/**
	 * Adds an object to path and sets whether or not this path spans multiple domains
	 * 
	 * @param obj
	 * @return
	 */
	public boolean add(PathObject<?> obj){
		return this.getPath().add(obj);
	}
	
	/**
	 * @return The {@link List} of {@link PathObject}s that comprise a path
	 */
	public List<PathObject<?>> getPath(){
		return this.path;
	}
	
	public void setPath( List<PathObject<?>> path){
		this.path = path;
	}
	
	public void setIsUseful(boolean isUseful){
		this.isUseful = isUseful;
	}
	
	public boolean getIsUseful(){
		return this.isUseful;
	}
	
	public int size(){
		return this.getPath().size();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean equals(Path path){
		int thisPathLength = this.size();
		int comparatorPathLength = path.size();
				
		if(thisPathLength != comparatorPathLength){
			return false;
		}
		
		List<PathObject<?>> comparatorPathNode = path.getPath();
		for(PathObject<?> obj : this.getPath()){
			if(!obj.getClass().getCanonicalName().equals(comparatorPathNode.getClass().getCanonicalName())){
				return false;
			}
			if(!obj.equals(comparatorPathNode)){
				return false;
			}
		}
		
		return true;		
	}
	
	/**
	 * Clone {@link Path} object
	 * 
	 * @param path
	 * @return
	 */
	public static Path clone(Path path){
		Path clonePath = new Path();
		
		List<PathObject<?>> path_obj = path.getPath();
		List<PathObject<?>> clone_list = new ArrayList<PathObject<?>>();
		for(PathObject<?> obj : path_obj){
			PathObject<?> path_obj_clone = obj.clone();
			clone_list.add(path_obj_clone);
		}
		
		clonePath.setPath(clone_list);
		clonePath.setKey(path.getKey());
		clonePath.setIsUseful(path.getIsUseful());
		clonePath.setSpansMultipleDomains(path.getSpansMultipleDomains());
		
		return clonePath;
	}
	
	/**
	 * Gets the last Vertex in a path that is of type {@link Page}
	 * 
	 * @return
	 */
	public Page findLastPage(){
		log.info("getting last page");
		List<PathObject<?>> path_obj_list = this.getPath();
		Page page = null;

		for(PathObject<?> obj : path_obj_list){
			if(obj instanceof Page){
				log.info("last page acquired");
				page = (Page)obj;
			}
		}

		return page;
	}
	
	/**
	 * Checks if the path has 2 sequential elements that appear in more than 1 location
	 * 
	 * @param path
	 * @return true if sequence appears more than once
	 */
	public static boolean hasCycle(Path path){
		if(path.size() == 1){
			return false;
		}
		
		List<PathObject<?>> path_obj_list = path.getPath();
		Page page = null;
		for(PathObject<?> path_obj : path_obj_list){

			for(PathObject<?> path_obj2 : path_obj_list){

				if(path_obj	 instanceof Page){
					log.info("last page acquired");
					page = (Page)path_obj;
					
					if(path_obj.equals(path)){
						return true;
					}
				}
				path_obj = path_obj.getNext();
			}while(path_obj.getNext() != null);
		}
		for(int i = path.size()-1; i > 0; i--){
			for(int j = i-1; j>= 0; j--){
				if(path.getPath().equals(path.getPath()) 
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
				if(path.getPath().get(i) instanceof Page 
						&& path.getPath().get(j) instanceof Page
						&& path.getPath().get(i).equals(path.getPath().get(j)))
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
		log.info("checking path for domains :: "+path.getClass().getName());
		log.info("Last page url :: " + this.findLastPage().getUrl());
		String domain = "";
		
		//iterate over path
		List<PathObject<?>> path_obj_list = this.getPath();
		
		for(PathObject<?> obj : path_obj_list){
			if(obj instanceof Page){
				Page page = (Page)obj;
				String curr_domain = page.getUrl().toString();
				if(domain.isEmpty()){
					domain = curr_domain;
				}
				else if(!domain.equals(curr_domain)){
					return true;
				}
			}
		}
		
		//if path domains change then return true
		return false;
	}

	public IPath convertToRecord(OrientConnectionFactory connection) {
		this.setKey(this.generateKey());
		Iterable<IPath> paths = (Iterable<IPath>) DataAccessObject.findByKey(this.getKey(), connection, IPath.class);
		
		int cnt = 0;
		Iterator<IPath> iter = paths.iterator();
		IPath path = null;
		while(iter.hasNext()){
			iter.next();
			cnt++;
		}
		log.info("# of existing Path records with key "+this.getKey() + " :: "+cnt);
		
		if(cnt == 0){
			path = connection.getTransaction().addVertex("class:"+IPath.class.getCanonicalName()+","+UUID.randomUUID(), IPath.class);
			path.setKey(this.getKey());
		}
		else{
			path = paths.iterator().next();
		}

		boolean first_pass = true;
		IPathObject last_path_obj = null;

		for(PathObject<?> obj: this.getPath()){
			log.info("setting data for last object");
			if(obj == null){
				break;
			}
			
			IPathObject persistablePathObj = obj.convertToRecord(connection);
			if(first_pass){
				log.info("First object detected : "+persistablePathObj.getClass());
				path.setPath(persistablePathObj);
				
				log.info("adding object to path");
				first_pass = false;
			}
			else{
				log.info("setting next object in path using IPathEdge");
				IPathEdge path_edge = last_path_obj.addPathEdge(persistablePathObj);
				
				log.info("Setting path key on IPathEdge");
				path_edge.setPathKey(this.generateKey());
			}
			last_path_obj = persistablePathObj;
		}
		
		path.setUsefulness(this.getIsUseful());
		
		log.info("Is spans multiple domains set : " + this.getSpansMultipleDomains());
		path.setSpansMultipleDomains(this.getSpansMultipleDomains());
		return path;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateKey() {
		String path_key = "";

		for(PathObject<?> obj : this.getPath()){
			if(obj == null){
				break;
			}
			path_key += obj.generateKey() + ":"+hashCode()+":";
		}

		//this.key = path_key;
		return path_key;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IPath create() {
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		log.info("converting path to record");
		IPath path = this.convertToRecord(orient_connection);
		
		log.info("path converted. now saving");
		orient_connection.save();
		
		return path;
	}

	@Override
	public IPath update() {
		OrientConnectionFactory connection = new OrientConnectionFactory();
		IPath path = this.convertToRecord(connection);
		
		connection.save();
		
		return path;
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
		
		String path_key = ipath.getKey();
		log.info("Path key for path Object :: "+path_key);
		
		log.info("setting key");
		path.setKey(path_key);
			
		log.info("setting if spans multiple domains");
		path.setSpansMultipleDomains(ipath.isSpansMultipleDomains());
		
		log.info("getting initial path vertex in path");
		IPathObject path_obj = ipath.getPath();
		
		//Page page = new Page();
		Iterator<IPage> ipage = (Iterator<IPage>) DataAccessObject.findByKey(ipath.getPath().getKey(), IPage.class).iterator();
		//path.setPath(new ArrayList<PathObject<?>>());
		log.info("page found");
		Page page = Page.convertFromRecord(ipage.next());
		path.getPath().add(page);
		page.setType(Page.class.getSimpleName());

		int count = 0;
		while(path_obj.getNext() != null){
			log.info("Path object is being observed "+path_obj);
			int matching_edge_cnt = 0;
			Iterator<IPathEdge> path_edge = path_obj.getPathEdges().iterator();
			
			while(path_edge.hasNext()){
				IPathEdge next_path_edge = path_edge.next();
				String key = next_path_edge.getPathKey();
				log.info("Observing edge with key " + key);

				if(key.equals(path_key)){
					log.info("Edge with path key located");
					IPathObject path_obj_out = next_path_edge.getPathObjectOut();
					
					log.info("looping through  page elements and adding them to path object " + count);
					PathObject<?> this_path_obj = PathObject.convertFromRecord(path_obj_out);
					log.info("retrieved path object : " + this_path_obj);
					path.add(this_path_obj);
					matching_edge_cnt++;
					break;
				}
			}

			if(matching_edge_cnt == 0){
				break;
			}
			PathObject<?> this_path_obj = PathObject.convertFromRecord(path_obj.getNext());
			log.info("retrieved path object : " + this_path_obj);
			path.add(this_path_obj);
			path_obj = path_obj.getNext();
			
			count++;
		}

		log.info("PATH OBJECT NEXT :: "+path_obj.getNext());
		/*while(path_obj != null && path_obj.getNext() != null){
			log.info("looping through  page elements and adding them to path object");
			PathObject<?> this_path_obj = PathObject.convertFromRecord(path_obj.getNext());
			log.info("retrieved path object : " + this_path_obj);
			path.add(this_path_obj);
			path_obj = path_obj.getNext();
		}
		*/
		//log.info("path object type : " + path_obj.getType());
		//log.info("path object canonical class name : " + path_obj.getClass().getCanonicalName());
		log.info("building path object record");
		
		return path;
	}

	public Page getFirstPage() {
		
		for(PathObject<?> obj : this.getPath()){
			if(obj instanceof Page){
				return (Page)obj;
			}
		}
		return null;
	}
}


