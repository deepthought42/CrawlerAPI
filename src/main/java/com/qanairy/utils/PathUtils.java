package com.qanairy.utils;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Redirect;
import com.qanairy.services.TestService;

public class PathUtils {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(TestService.class);

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
				
		for(int idx = path_objects.size()-1; idx >= 0; idx--){
			log.warn("checking path object    :::      "+path_objects.get(idx).getKey());
			if(path_objects.get(idx).getKey().contains("pagestate")){
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

	public static List<PathObject> reducePathObjects(List<PathObject> ordered_path_objects) {
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
			if(obj.getType().equals("PageState")){
				return ((PageState)obj);
			}
		}
		
		return null;
	}

	public static PageState getSecondToLastPageState(List<PathObject> path_objects) {
		assert(path_objects != null);
		
		int page_states_seen = 0;
		log.warn("path objects length while getting second to last page state ;: "+path_objects.size());
		for(int idx = path_objects.size()-1; idx >=0; idx--){
			if(path_objects.get(idx).getKey().contains("pagestate")){
				if(page_states_seen >= 1){
					return (PageState)path_objects.get(idx);
				}
				page_states_seen++;
			}
		}
		
		return null;
	}

	public static List<String> reducePathKeys(List<String> final_key_list) {
		//scrub path objects for duplicates
		List<String> reduced_path_keys = new ArrayList<>();
		String last_path_key = null;
		for(String key : final_key_list){
			if(!key.equals(last_path_key)){
				last_path_key = key;
				reduced_path_keys.add(key);
			}
		}
		return reduced_path_keys;
	}

	/**
	 * 
	 * @param pathObjects
	 * @return
	 * 
	 * @pre !pathObjects.isEmpty()
	 */
	public static String getFirstUrl(List<PathObject> pathObjects) {
		assert !pathObjects.isEmpty();
		
		PathObject obj = pathObjects.get(0);
		
		if(obj.getKey().contains("redirect")) {
			log.warn("first path object is a redirect");
			Redirect redirect = (Redirect)obj;
			return redirect.getStartUrl();
		}
		else if(obj.getKey().contains("pagestate")) {
			log.warn("first path object is a page state");
			PageState page_state = (PageState)obj;
			return page_state.getUrl();
		}
		return null;
	}
}
