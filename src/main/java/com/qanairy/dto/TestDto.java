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
	
	public TestDto(Test test){
		setKey(test.getKey());
		
		this.path = new ArrayList<Object>();
		
		List<PathObject> path_objects = test.getPathObjects();
		for(int idx = 0; idx<test.getPathObjects().size(); idx++){
			if(path_objects.get(idx).getType().equals("PageState")){

				this.path.add(new PageStateDto((PageState)path_objects.get(idx)));	
			}
			else if(path_objects.get(idx).getType().equals("PageElement")){
				this.path.add(new ElementActionDto((PageElement)test.getPathObjects().get(idx), (Action)path_objects.get(++idx)));
			}
		}
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public List<PathObject> getPath() {
		return path;
	}

	public void setPath(List<PathObject> path) {
		this.path = path;
	}
}
