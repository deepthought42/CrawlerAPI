package persistence;

import java.util.List;
import java.util.Map;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;

import browsing.Attribute;
import browsing.PageElement;

public interface IPageElement {
	//private String[] actions = ActionFactory.getActions();
	@Property("tagName")
	public String getTagName();
	
	@Property("tagName")
	public String setTagName();
	
	@Property("text")
	public String getText();
	
	@Property("text")
	public String setText();
	
	@Property("xpath")
	public String getXpath();
	
	@Property("xpath")
	public String setXpath();

	@Property("changed")
	public boolean getChanged();
	
	@Property("changed")
	public boolean setChanged();
	
	@Adjacency(label="has")
	public List<Attribute> getAttributes();
	
	@Adjacency(label="has")
	public List<Attribute> setAttributes();
	
	@Adjacency(label="contains")
	public List<PageElement> getChildElements();
	
	@Adjacency(label="contains")
	public List<PageElement> setChildElements();
	
	@Property("cssValues")
	public Map<String, String> getCssValues();
	
	@Property("cssValues")
	public Map<String, String> setCssValues();
}
