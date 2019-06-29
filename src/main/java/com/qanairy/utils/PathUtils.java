package com.qanairy.utils;

import java.util.List;

import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;

public class PathUtils {

	public static PageState getLastPageState(List<PathObject> pathObjects) {
		PageState last_page_state = null;
		
		for(int idx = pathObjects.size()-1; idx >=0; idx++){
			if(pathObjects.get(idx) instanceof PageState){
				last_page_state = (PageState)pathObjects.get(idx);
				break;
			}
		}
		
		return last_page_state;
	}

}
