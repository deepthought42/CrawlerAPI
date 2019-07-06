package com.qanairy.utils;

import java.util.ArrayList;
import java.util.List;

import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;

public class PathUtils {

	/**
	 * Retrieves the last {@link PageState} in the given list of {@link PathObject}s
	 * 
	 * @param pathObjects list of {@link PathObject}s in sequential order
	 * 
	 * @return last page state in list
	 * 
	 * @pre pathObjects != null
	 */
	public static PageState getLastPageState(List<PathObject> pathObjects) {
		assert(pathObjects != null);
		
		PageState last_page_state = null;
		
		for(int idx = pathObjects.size()-1; idx >=0; idx--){
			if(pathObjects.get(idx) instanceof PageState){
				last_page_state = (PageState)pathObjects.get(idx);
				break;
			}
		}
		
		return last_page_state;
	}
	
	public static int getIndexOfLastElementState(List<String> path_keys){
		for(int element_idx=path_keys.size()-1; element_idx > 0; element_idx--){
			if(path_keys.get(element_idx).contains("elementstate")){
				return element_idx;
			}
		}

		return -1;
	}

	public static List<PathObject> orderPathObjects(List<String> path_keys, List<PathObject> path_objects) {
		List<PathObject> ordered_path_objects = new ArrayList<PathObject>();

		//Ensure Order path objects
		for(String path_obj_key : path_keys){
			for(PathObject obj : path_objects){
				if(obj.getKey().equals(path_obj_key)){
					ordered_path_objects.add(obj);
				}
			}
		}

		PathObject last_path_obj = null;
		List<PathObject> reduced_path_obj = new ArrayList<PathObject>();
		//scrub path objects for duplicates
		for(PathObject obj : ordered_path_objects){
			if(last_path_obj == null || !obj.getKey().equals(last_path_obj.getKey())){
				last_path_obj = obj;
				reduced_path_obj.add(obj);
			}
		}

		return reduced_path_obj;
	}
}
