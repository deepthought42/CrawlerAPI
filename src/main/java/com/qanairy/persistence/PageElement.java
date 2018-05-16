package com.qanairy.persistence;

import java.util.List;
import java.util.Map;
import com.syncleus.ferma.AbstractVertexFrame;
import com.syncleus.ferma.annotations.Adjacency;
import com.syncleus.ferma.annotations.Property;
import com.qanairy.persistence.PathObject;

/**
 * 
 */
public abstract class PageElement extends AbstractVertexFrame implements PathObject{
	@Property("key")
	public abstract String getKey();
	
	@Property("key")
	public abstract void setKey(String key);
	
	@Property("tagName")
	public abstract String getName();
	
	@Property("tagName")
	public abstract void setName(String tagName);
	
	@Property("text")
	public abstract String getText();
	
	@Property("text")
	public abstract void setText(String text);
	
	@Property("xpath")
	public abstract String getXpath();
	
	@Property("xpath")
	public abstract void setXpath(String xpath);
	
	@Property("cssValues")
	public abstract Map<String, String> getCssValues();
	
	@Property("cssValues")
	public abstract void setCssValues(Map<String, String> cssMap);
	
	@Property("screenshot")
	public abstract String getScreenshot();
	
	@Property("screenshot")
	public abstract void setScreenshot(String cssMap);
	
	@Adjacency(label="has_attribute")
	public abstract List<Attribute> getAttributes();
	
	/**
	 * @return {@link List} of {@link PathObject}s representing a path sequence
	 */
	@Adjacency(label="has_attribute")
	public abstract void setAttributes(List<Attribute> attributes);
	
	/**
	 * @return {@link List} of {@link PathObject}s representing a path sequence
	 */
	@Adjacency(label="has_attribute")
	public abstract void addAttribute(Attribute attribute);
	
	/**
	 * @return {@link List} of {@link PathObject}s representing a path sequence
	 */
	@Adjacency(label="has_rule")
	public abstract void setRules(List<IRule> rules);
	
	@Adjacency(label="has_rule")
	public abstract void addRule(IRule rules);
	
	@Adjacency(label="has_rule")
	public abstract List<Rule> getRules();
}
