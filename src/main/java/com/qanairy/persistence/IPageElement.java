package com.qanairy.persistence;

import java.util.List;
import java.util.Map;

import com.qanairy.models.Attribute;
import com.qanairy.models.PathObject;
import com.tinkerpop.frames.Adjacency;
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
	
	@Adjacency(label="has_attribute")
	public Iterable<IAttribute> getAttributes();
	
	/**
	 * @return {@link List} of {@link PathObject}s representing a path sequence
	 */
	@Adjacency(label="has_attribute")
	public void setAttributes(List<IAttribute> attributes);
	
	/**
	 * @return {@link List} of {@link PathObject}s representing a path sequence
	 */
	@Adjacency(label="has_attribute")
	public void addAttributes(IAttribute attribute);
	
	@Property("cssValues")
	public Map<String, String> getCssValues();
	
	@Property("cssValues")
	public void setCssValues(Map<String, String> cssMap);
}
