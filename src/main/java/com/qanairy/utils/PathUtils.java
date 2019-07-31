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
	public static PageState getLastPageState(List<PathObject> path_objects) {
		assert(path_objects != null);
				
		for(int idx = path_objects.size()-1; idx >=0; idx--){
			if(path_objects.get(idx) instanceof PageState){
				return (PageState)path_objects.get(idx);
			}
		}
		
		return null;
	}
	
	public static int getIndexOfLastElementState(List<String> path_keys){
		for(int element_idx=path_keys.size()-1; element_idx >= 0; element_idx--){
			if(path_keys.get(element_idx).contains("elementstate")){
				return element_idx;
			}
		}

		return -1;
	}

	public static List<PathObject> orderPathObjects(List<String> path_keys, List<PathObject> path_objects) {
		List<PathObject> ordered_path_objects = new ArrayList<>();
		List<String> temp_path_keys = new ArrayList<>(path_keys);
		//Ensure Order path objects
		for(String path_obj_key : temp_path_keys){
			for(PathObject obj : path_objects){
				if(obj.getKey().equals(path_obj_key)){
					ordered_path_objects.add(obj);
				}
			}
		}

		PathObject last_path_obj = null;
		List<PathObject> reduced_path_obj = new ArrayList<>();
		//scrub path objects for duplicates
		for(PathObject obj : ordered_path_objects){
			if(last_path_obj == null || !obj.getKey().equals(last_path_obj.getKey())){
				last_path_obj = obj;
				reduced_path_obj.add(obj);
			}
		}

		return reduced_path_obj;
	}

	public static List<PathObject> reducePathObjects(List<String> path_keys, List<PathObject> ordered_path_objects) {
		//scrub path objects for duplicates
		List<PathObject> reduced_path_objs = new ArrayList<>();
		PathObject last_path_obj = null;
		for(PathObject obj : ordered_path_objects){
			if(last_path_obj == null || !obj.getKey().equals(last_path_obj.getKey())){
				last_path_obj = obj;
				reduced_path_objs.add(obj);
			}
		}
				
		return reduced_path_objs;
	}

	public static PageState getFirstPage(List<PathObject> ordered_path_objects) {
		//find first page
		for(PathObject obj : ordered_path_objects){
			if(obj instanceof PageState){
				return ((PageState)obj);
			}
		}
		
		return null;
	}

	public static PageState getSecondToLastPageState(List<PathObject> path_objects) {
		assert(path_objects != null);
		
		PageState page_state = null;
		int page_states_seen = 0;
		
		for(int idx = path_objects.size()-1; idx >=0; idx--){
			if(path_objects.get(idx) instanceof PageState){
				if(page_states_seen >= 1){
					page_state = (PageState)path_objects.get(idx);
					break;
				}
				page_states_seen++;
			}
		}
		
		return page_state;
	}

	public static List<String> reducePathKeys(List<String> final_key_list) {
		//scrub path objects for duplicates
		List<String> reduced_path_keys = new ArrayList<>();
		String last_path_key = null;
		for(String key : final_key_list){
			if(last_path_key == null || !key.equals(last_path_key)){
				last_path_key = key;
				reduced_path_keys.add(key);
			}
		}
				
		return reduced_path_keys;
	}
}
