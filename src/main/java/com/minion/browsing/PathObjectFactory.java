package com.minion.browsing;

import com.minion.browsing.actions.Action;
import com.minion.persistence.IPersistable;

public class PathObjectFactory {
	public static IPersistable<?> build(PathObject<?> path_obj){
		if(path_obj.getData() instanceof Page){
			return (Page)path_obj.getData();
		}
		else if(path_obj.getData() instanceof PageElement){
			return (PageElement)path_obj.getData();
		}
		else if(path_obj.getData() instanceof Action){
			return (Action)path_obj.getData();
		}
		
		return null;
	}
}
