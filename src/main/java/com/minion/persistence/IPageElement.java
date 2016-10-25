package com.minion.persistence;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.minion.browsing.PathObject;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;


public interface IPageElement extends IPathObject {
	//private String[] actions = ActionFactory.getActions();
	@Property("key")
	public String getKey();
	
	@Property("key")
	public void setKey(String key);
	
	@Property("tagName")
	public String getTagName();
	
	@Property("tagName")
	public void setTagName(String tagName);
	
	@Property("text")
	public String getText();
	
	@Property("text")
	public void setText(String text);
	
	@Property("xpath")
	public String getXpath();
	
	@Property("xpath")
	public void setXpath(String xpath);

	@Property("changed")
	public boolean getChanged();
	
	@Property("changed")
	public void setChanged(boolean isChanged);
	
	/*
	@Adjacency(label="has")
	public Iterator<IAttribute> getAttributes();
	
	@Adjacency(label="has")
	public void setAttributes(List<IAttribute> attributes);
	
	
	@Adjacency(label="contains")
	public Iterator<IPageElement> getChildElements();
	
	@Adjacency(label="contains")
	public void setChildElements(List<IPageElement> elements);
	*/
	
	@Property("cssValues")
	public Map<String, String> getCssValues();
	
	@Property("cssValues")
	public void setCssValues(Map<String, String> cssMap);
}
