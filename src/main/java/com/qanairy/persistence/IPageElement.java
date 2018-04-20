package com.qanairy.persistence;

import java.util.List;
import java.util.Map;

import com.qanairy.models.PathObject;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.annotations.gremlin.GremlinGroovy;

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
	
	@Property("cssValues")
	public Map<String, String> getCssValues();
	
	@Property("cssValues")
	public void setCssValues(Map<String, String> cssMap);
	
	@Property("screenshot")
	public String getScreenshot();
	
	@Property("screenshot")
	public void setScreenshot(String cssMap);
	
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
	public void addAttribute(IAttribute attribute);
	
	/**
	 * @return {@link List} of {@link PathObject}s representing a path sequence
	 */
	@Adjacency(label="has_rule")
	public void setRules(List<IRule> rules);
	
	@Adjacency(label="has_rule")
	public void addRule(IRule rules);
	
	@Adjacency(label="has_rule")
	public Iterable<IRule> getRules();
}
