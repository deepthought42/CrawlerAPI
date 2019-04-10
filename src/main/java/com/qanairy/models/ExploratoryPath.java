package com.qanairy.models;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A set of vertex objects that form a sequential movement through a graph
 */
public class ExploratoryPath {
	private static Logger log = LoggerFactory.getLogger(ExploratoryPath.class);
	
	private List<String> path_keys;
	private List<PathObject> path_objects;
	private List<Action> possible_actions;

	/**
	 * Creates new instance of path setting it to the given path
	 * 
	 * @param current_path
	 */
	public ExploratoryPath(List<String> path_keys, List<PathObject> current_path, List<Action> actions){
		setPathKeys(path_keys);
		setPathObjects(current_path);
		setPossibleActions(actions);
	}

		
	/**
	 * Adds an object to path and sets whether or not this path spans multiple domains
	 * 
	 * @param obj
	 * @return
	 */
	public boolean add(PathObject obj){
		return getPathObjects().add(obj);
	}
	
	public int size(){
		return getPathObjects().size();
	}
	
	/**
	 * Gets the last Vertex in a path that is of type {@link PageState}
	 * 
	 * @return
	 */
	public PageState findLastPage(){
		List<PathObject> path_obj_list = getPathObjects();
		PageState page = null;

		for(PathObject obj : path_obj_list){
			if(obj instanceof PageState){
				page = (PageState)obj;
			}
		}

		return page;
	}
	
	/**
	 * Checks if the path has 2 pages that are the equal
	 * 
	 * @param path
	 * 
	 * @pre path_key_list != null
	 * @pre !path_key_list.isEmpty()
	 * @pre page != null
	 * 
	 * @return true if sequence appears more than once
	 */
	public static boolean hasCycle(List<PageState> path_objects, PageState page, boolean isSinglePage){
		assert page != null;
	
		if(isSinglePage){
			return false;
		}
		
		//extract all pages
		//iterate through pages to see if any match
		log.info("Checking if exploratory path has a cycle");
		for(PageState path_obj : path_objects){
			if(path_obj.equals(page)){
				return true;
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
		//if path object is of type ElementState
		//	then load path object
		//		check if path object leads to an action that exists in the paths possible actions list
		//		If there exists an action that matches a possible action 
		//			then get next path object
		//				if path object is of type ElementState
		//					then load path object
		//						check if path object leads to an action that exists in the paths possible actions list
		/*for(PathObject path_obj : path.getPathObjects()){
			if(path_obj instanceof ElementState){
				ElementStateDao page_elem_dao = new ElementStateDaoImpl();
				OrientConnectionFactory connection = new OrientConnectionFactory();
				ElementState page_elem = page_elem_dao.find(path_obj.getKey());
				if(page_elem != null){
					List<Action> actions = path.getPossibleActions();
					ElementState ipage_elem = page_elem_dao.save(page_elem);
					Iterator<PathEdge> path_edge_iter = ipage_elem.getPathEdges().iterator();
					while(path_edge_iter.hasNext()){
						PathEdge edge = path_edge_iter.next();
						PathObject path_object_out = edge.getPathObjectIn();
						PathObjectDao path_obj_repo = new PathObjectDaoImpl();
						PathObject new_path_obj = path_obj_repo.find(path_object_out.getKey());						
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
		*/
		return false;
	}
	
	/**
	 * Checks if the path has the same page more than once. 
	 * 
	 * @param path
	 * @return true if sequence appears more than once
	 */
	public static boolean hasPageCycle(List<PathObject> path){
		for(int i = path.size()-1; i > 0; i--){
			for(int j = i-1; j>= 0; j--){
				if(path.get(i) instanceof PageState 
						&& path.get(j) instanceof PageState
						&& path.get(i).equals(path.get(j)))
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
		List<PathObject> path_obj_list = getPathObjects();
		
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
		
		for(PathObject obj : getPathObjects()){
			if(obj instanceof PageState){
				return (PageState)obj;
			}
		}
		return null;
	}

	/**
	 * Adds an object to path
	 * 
	 * @param obj
	 * @return
	 */
	public boolean addToPathKeys(String key){
		return this.getPathKeys().add(key);
	}
	
	public void addPathObject(PathObject path_obj) {
		this.path_objects.add(path_obj);
	}

	public List<PathObject> getPathObjects() {
		return this.path_objects;
	}

	public void setPathObjects(List<PathObject> path_objects) {
		this.path_objects = path_objects;
	}
	
	public List<Action> getPossibleActions() {
		return possible_actions;
	}

	public void setPossibleActions(List<Action> possible_actions) {
		this.possible_actions = possible_actions;
	}

	public List<String> getPathKeys() {
		return path_keys;
	}

	public void setPathKeys(List<String> path_keys) {
		this.path_keys = path_keys;
	}
	
	/**
	 * Clone {@link Path} object
	 * 
	 * @param path
	 * @return
	 */
	public static ExploratoryPath clone(ExploratoryPath path){		
		List<PathObject> path_objects = new ArrayList<PathObject>(path.getPathObjects());
		List<String> path_keys = new ArrayList<String>(path.getPathKeys());
		List<Action> possible_actions = new ArrayList<Action>(path.getPossibleActions());
		
		return new ExploratoryPath(path_keys, path_objects, possible_actions);
	}
	

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object o){
		if(o instanceof ExploratoryPath){
			ExploratoryPath path = (ExploratoryPath)o;
			List<PathObject> comparator_nodes = new ArrayList<PathObject>(path.getPathObjects());
			for(PathObject obj : getPathObjects()){
				int idx = 0;
				for(PathObject comparator_obj : path.getPathObjects()){
					if(comparator_obj.equals(obj)){
						comparator_nodes.remove(idx);
						break;
					}
					idx++;
				}
			}
			return comparator_nodes.isEmpty();
		}
		return false;
	}
}


