package com.qanairy.models;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;

import com.qanairy.models.dto.PageElementRepository;
import com.qanairy.models.dto.PathObjectRepository;
import com.qanairy.persistence.IPageElement;
import com.qanairy.persistence.IPathObject;
import com.qanairy.persistence.OrientConnectionFactory;
import com.qanairy.persistence.edges.IPathEdge;


/**
 * A set of vertex objects that form a sequential movement through a graph
 */
public class ExploratoryPath extends Path{
	private static Logger log = LoggerFactory.getLogger(ExploratoryPath.class);
	
	private List<Action> possible_actions = null;
	
	/**
	 * Creates new instance of Path
	 */
	public ExploratoryPath(){
		super();
	}

	/**
	 * Creates new instance of path setting it to the given path
	 * 
	 * @param current_path
	 */
	public ExploratoryPath(List<PathObject> current_path, List<Action> actions){
		super(current_path);
		this.setPossibleActions(actions);
	}

		
	/**
	 * Adds an object to path and sets whether or not this path spans multiple domains
	 * 
	 * @param obj
	 * @return
	 */
	public boolean add(PathObject obj){
		return this.getPath().add(obj);
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
		
		List<PathObject> comparatorPathNode = path.getPath();
		for(PathObject obj : this.getPath()){
			if(!obj.getClass().getSimpleName().equals(comparatorPathNode.getClass().getSimpleName())){
				return false;
			}
			if(!obj.equals(comparatorPathNode)){
				return false;
			}
		}
		
		return true;		
	}
	
	/**
	 * Gets the last Vertex in a path that is of type {@link Page}
	 * 
	 * @return
	 */
	public Page findLastPage(){
		List<PathObject> path_obj_list = this.getPath();
		Page page = null;

		for(PathObject obj : path_obj_list){
			if(obj instanceof Page){
				page = (Page)obj;
			}
		}

		return page;
	}

	/**
	 * Checks if the path has 2 pages that are the equal
	 * 
	 * @param path
	 * @return true if sequence appears more than once
	 */
	public static boolean hasCycle(Path path, Page page){
		if(path.size() == 1){
			return false;
		}
		
		//extract all pages
		//iterate through pages to see if any match
		
		System.err.println("Checking path for cycle .... ");
		for(PathObject path_obj : path.getPath()){			
			if(path_obj instanceof Page){
				if(((Page)path_obj).equals(page)){
					System.err.println("PATH HAS A CYCLE      !!!!!!!!!!!!!!    ");
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
	public static boolean hasExistingElementActionSequence(ExploratoryPath path){
		if(path.size() == 1){
			return false;
		}
		
		//iterate through path
		//if path object is of type PageElement
		//	then load path object
		//		check if path object leads to an action that exists in the paths possible actions list
		//		If there exists an action that matches a possible action 
		//			then get next path object
		//				if path object is of type PageElement
		//					then load path object
		//						check if path object leads to an action that exists in the paths possible actions list
		for(PathObject path_obj : path.getPath()){
			if(path_obj instanceof PageElement){
				PageElementRepository page_elem_repo = new PageElementRepository();
				OrientConnectionFactory connection = new OrientConnectionFactory();
				PageElement page_elem = page_elem_repo.find(connection, page_elem_repo.generateKey((PageElement)path_obj));
				if(page_elem != null){
					List<Action> actions = path.getPossibleActions();
					IPageElement ipage_elem = page_elem_repo.save(connection, page_elem);
					Iterator<IPathEdge> path_edge_iter = ipage_elem.getPathEdges().iterator();
					while(path_edge_iter.hasNext()){
						IPathEdge edge = path_edge_iter.next();
						IPathObject path_object_out = edge.getPathObjectIn();
						PathObjectRepository path_obj_repo = new PathObjectRepository();
						PathObject new_path_obj = path_obj_repo.load(path_object_out);						
						if(new_path_obj.getType().equals("Action")){
							for(Action action : actions){
								if(((Action)new_path_obj).getName().equals(action.getName()) && ((Action)new_path_obj).getValue().equals(action.getValue())){
									return true;
								}
							}
						}
					}
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

	public boolean checkIfSpansMultipleDomains() {
		log.debug("Last page url :: " + this.findLastPage().getUrl());
		String domain = "";
		
		//iterate over path
		List<PathObject> path_obj_list = this.getPath();
		
		for(PathObject obj : path_obj_list){
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

	public Page firstPage() {
		
		for(PathObject obj : this.getPath()){
			if(obj instanceof Page){
				return (Page)obj;
			}
		}
		return null;
	}

	public List<Action> getPossibleActions() {
		return possible_actions;
	}

	public void setPossibleActions(List<Action> possible_actions) {
		this.possible_actions = possible_actions;
	}
}


