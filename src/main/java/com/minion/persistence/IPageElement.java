package com.minion.persistence;

import java.util.List;
import java.util.Map;

import com.minion.browsing.Attribute;
import com.tinkerpop.frames.Property;

public interface IPageElement extends IPathObject{
	@Property("key")
	public String getKey();
	
	@Property("key")
	public void setKey(String key);
	
	@Property("tagName")
	public String getName();
	
	@Property("tagName")
	public void setName(String tagName);
	
	@Property("text")
	public String getText();
	
	@Property("text")
	public void setText(String text);
	
	@Property("xpath")
	public String getXpath();
	
	@Property("xpath")
	public void setXpath(String xpath);
	
	@Property("attributes")
	public List<Attribute> getAttributes();
	
	@Property("attributes")
	public void setAttributes(List<Attribute> attributes);
	
	@Property("cssValues")
	public Map<String, String> getCssValues();
	
	@Property("cssValues")
	public void setCssValues(Map<String, String> cssMap);
}
