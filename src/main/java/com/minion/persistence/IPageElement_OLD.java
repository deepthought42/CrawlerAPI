package com.minion.persistence;

import com.tinkerpop.frames.Property;


public interface IPageElement_OLD extends IPathObject {
	//private String[] actions = ActionFactory.getActions();
	@Property("key")
	public String getKey();
	
	@Property("key")
	public void setKey(String key);
	
	@Property("screenshot")
	public String getScreenshot();
	
	@Property("screenshot")
	public void setScreenshot(String screenshot_url);
	
	/*
	@Adjacency(label="contains")
	public Iterator<IPageElement> getChildElements();
	
	@Adjacency(label="contains")
	public void setChildElements(List<IPageElement> elements);
	*/

}
