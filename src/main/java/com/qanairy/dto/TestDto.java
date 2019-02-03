package com.qanairy.dto;

import java.util.ArrayList;
import java.util.List;

import com.qanairy.models.Action;
import com.qanairy.models.PageElement;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Test;

/**
 * Data transfer object for {@link Test} object that is designed to comply with 
 * the data format for browser extensions
 */
public class TestDto {
	private String key;
	private List<Object> path;
	
	public TestDto(){}
	
	public TestDto(Test test){
		setKey(test.getKey());
		
		this.path = new ArrayList<Object>();
		
		List<PathObject> path_objects = test.getPathObjects();
		List<PathObject> ordered_path_objects = new ArrayList<PathObject>();
		//order by key
		for(String key : test.getPathKeys()){
			for(PathObject obj : path_objects){
				if(obj.getKey().equals(key)){
					ordered_path_objects.add(obj);
				}
			}
		}
		
		boolean first_page = true;
		for(int idx = 0; idx < test.getPathObjects().size()-1; idx++){
			if(ordered_path_objects.get(idx).getType().equals("PageState") && first_page){
				first_page = false;
				this.path.add(new PageStateDto((PageState)ordered_path_objects.get(idx)));	
			}
			else if(ordered_path_objects.get(idx).getType().equals("PageElement")){
				System.err.println("PATH OBJECT :: " + ordered_path_objects.get(idx));
				this.path.add(new ElementActionDto((PageElement)ordered_path_objects.get(idx), (Action)ordered_path_objects.get(++idx)));
			}
		}
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public List<Object> getPath() {
		return path;
	}

	public void setPath(List<Object> path) {
		this.path = path;
	}
}
