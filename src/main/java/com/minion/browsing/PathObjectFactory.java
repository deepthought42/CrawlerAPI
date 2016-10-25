package com.minion.browsing;

import com.minion.browsing.actions.Action;

public class PathObjectFactory {
	public static PathObject<?> build(PathObject<?> path_obj){
		if(path_obj instanceof Page){
			return (Page)path_obj;
		}
		else if(path_obj instanceof PageElement){
			return (PageElement)path_obj;
		}
		else if(path_obj instanceof Action){
			return (Action)path_obj;
		}
		
		return null;
	}

}
