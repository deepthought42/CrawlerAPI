package com.qanairy.utils;

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
}
