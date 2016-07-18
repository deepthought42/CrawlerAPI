package com.minion.browsing;

import com.minion.browsing.actions.Action;
import com.minion.persistence.IPersistable;

public class PathObjectFactory {
	public static IPersistable<?> build(PathObject path_obj){
		if(path_obj.data() instanceof Page){
			return (Page)path_obj.data();
		}
		else if(path_obj.data() instanceof PageElement){
			return (PageElement)path_obj.data();
		}
		else if(path_obj.data() instanceof Action){
			return (Action)path_obj.data();
		}
		
		return null;
	}
}
