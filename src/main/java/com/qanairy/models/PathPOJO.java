package com.qanairy.models;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;

import com.qanairy.models.dao.PathDao;
import com.qanairy.models.dao.PathObjectDao;
import com.qanairy.models.dao.impl.PathDaoImpl;
import com.qanairy.models.dao.impl.PathObjectDaoImpl;
import com.qanairy.persistence.PageState;
import com.qanairy.persistence.Path;
import com.qanairy.persistence.PathObject;

/**
 * A set of vertex objects that form a sequential movement through a graph
 */
public class PathPOJO extends Path {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(Path.class);
	
    private String key;
	private boolean isUseful;
	private boolean spansMultipleDomains;
	private List<String> path = null;
	private List<PathObject> path_obj_list;
	
	/**
	 * Creates new instance of Path
	 */
	public PathPOJO(){
		this.isUseful = false;
		this.spansMultipleDomains = false;
		this.path = new ArrayList<String>();
		this.key = null;
		this.path_obj_list = new ArrayList<PathObject>();
	}

	/**
	 * Creates new instance of path setting it to the given path
	 * 
	 * @param current_path
	 */
	public PathPOJO(List<String> current_path){
		this.isUseful = false;
		this.path = current_path;
		this.key = null;
		this.path_obj_list = new ArrayList<PathObject>();
	}

	/**
	 * Creates new instance of path setting it to the given path
	 * 
	 * @param current_path
	 */
	public PathPOJO(String key, boolean isUseful, boolean spansMultipleDomains, List<String> current_path){
		this.isUseful = isUseful;
		this.path = current_path;
		this.key = key;
		this.spansMultipleDomains = spansMultipleDomains;
		this.path_obj_list = new ArrayList<PathObject>();
	}
		
	/**
	 * Gets the last Vertex in a path that is of type {@link PageState}
	 * 
	 * @return
	 */
	public PageState findLastPage(){
		List<String> path_obj_list = this.getPath();
		PageState page = null;

		PathObjectDao path_obj_dao = new PathObjectDaoImpl();
		for(String obj : path_obj_list){
			PathObject path_obj = path_obj_dao.find(key);
			if(path_obj != null && path_obj.getType().equals("PageState")){
				page = (PageState)path_obj;
			}
		}

		return page;
	}

	public boolean checkIfSpansMultipleDomains() {
		String domain = "";
		
		//iterate over path
		List<? extends PathObject> path_obj_list = this.getPathObjects();
		
		for(PathObject obj : path_obj_list){
			if(obj instanceof PageState){
				PageState page = (PageState)obj;
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

	public PageState firstPage() {
		PathObjectDao path_obj_dao = new PathObjectDaoImpl();
		for(String key : this.getPath()){
			PathObject path_obj = path_obj_dao.find(key);
			if(path_obj instanceof PageState){
				return (PageState)path_obj;
			}
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean equals(Path path){
		int thisPathLength = this.size();
		int comparatorPathLength = path.getPath().size();
				
		if(thisPathLength != comparatorPathLength){
			return false;
		}
		
		PathObjectDao path_obj_dao = new PathObjectDaoImpl();
		for(String key : this.getPath()){
			PathObject path_obj = path_obj_dao.find(key);
			if(!path_obj.getClass().getSimpleName().equals(comparatorPathNode.getClass().getSimpleName())){
				return false;
			}
			if(!path_obj.equals(comparatorPathNode)){
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
		Path clone_path = new PathPOJO();
		
		List<? extends PathObject> path_obj = path.getPathObjects();
		List<String> path_keys = new ArrayList<String>();
		for(PathObject obj : path_obj){
			clone_path.addToPath(obj.getKey());
			path_keys.add(obj.getKey());
		}
		
		clone_path.setPath(path_keys);
		clone_path.setKey(path.getKey());
		clone_path.setIsUseful(path.isUseful());
		clone_path.setSpansMultipleDomains(path.getIfSpansMultipleDomains());
		
		return clone_path;
	}
	
	public boolean getIfSpansMultipleDomains() {
		return spansMultipleDomains;
	}
	
	public void setSpansMultipleDomains(boolean isSpanningMultipleDomains) {
		this.spansMultipleDomains= isSpanningMultipleDomains;
	}
	
	public void setKey(String key) {
		this.key = key;
		
	}
	
	public String getKey() {
		return this.key;
	}
	
	/**
	 * Adds an object to path
	 * 
	 * @param obj
	 * @return
	 */
	@Override
	public boolean addToPath(String key){
		return this.getPath().add(key);
	}
	
	/**
	 * @return The {@link List} of {@link PathObject}s that comprise a path
	 */
	public List<String> getPath(){
		return this.path;
	}
	
	public void setPath( List<String> path){
		this.path = path;
	}
	
	public void setIsUseful(Boolean isUseful){
		this.isUseful = isUseful;
	}
	
	public Boolean isUseful(){
		return this.isUseful;
	}
	
	public int size(){
		return this.getPath().size();
	}

	@Override
	public void addPathObject(PathObject path_obj) {
		this.path_obj_list.add(path_obj);
	}

	@Override
	public List<? extends PathObject> getPathObjects() {
		return this.path_obj_list;
	}
}


