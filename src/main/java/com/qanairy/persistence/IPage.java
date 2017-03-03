package com.qanairy.persistence;

import com.qanairy.models.Page;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.modules.typedgraph.TypeValue;

import java.util.Map;

/**
 * Tinkerpop/frames interface implementation of {@link Page}
 * @author brandon
 *
 */
@TypeValue("Page") public interface IPage extends IPathObject{
	@Property("key")
	public String getKey();
	
	@Property("key")
	public void setKey(String key);
	
	@Property("landable")
	public boolean isLandable();
	
	@Property("landable")
	public void setLandable(boolean isLandable);
	
	@Property("screenshot")
	public String getScreenshot();
	
	@Property("screenshot")
	public void setScreenshot(String screenshotUrl);
	
	@Property("src")
	public String getSrc();

	@Property("src")
	public void setSrc(String src);
	
	@Property("url")
	public String getUrl();
	
	@Property("url")
	public void setUrl(String url);
	
	@Property("element_counts")
	public Map<String, Integer> getElementCounts();
	
	@Property("element_counts")
	public void setElementCounts(Map<String, Integer> element_count);
	/*@Adjacency(label="contains")
	public Iterator<IPageElement> getElements();
	
	@Adjacency(label="contains")
	public void setElements(List<IPageElement> elements);
	*/
}
