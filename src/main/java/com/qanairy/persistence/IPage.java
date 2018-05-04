package com.qanairy.persistence;

import com.qanairy.models.Page;
import com.qanairy.models.ScreenshotSet;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import java.util.Map;

/**
 * Tinkerpop/frames interface implementation of {@link Page}
 *
 */
public interface IPage extends IPathObject{
	@Property("key")
	public String getKey();
	
	@Property("key")
	public void setKey(String key);
	
	@Property("landable")
	public boolean isLandable();
	
	@Property("landable")
	public void setLandable(boolean isLandable);
	
	@Property("url")
	public String getUrl();
	
	@Property("url")
	public void setUrl(String url);
	
	@Property("element_counts")
	public Map<String, Integer> getElementCounts();
	
	@Property("element_counts")
	public void setElementCounts(Map<String, Integer> element_count);

	@Property("total_weight")
	public void setTotalWeight(Integer totalWeight);
	
	@Property("total_weight")
	public Integer getTotalWeight();

	@Property("img_weight")
	public void setImageWeight(Integer imageWeight);
	
	@Property("img_weight")
	public Integer getImageWeight();
	
	@Property("src")
	public String getSrc();

	@Property("src")
	public void setSrc(String src);
	
	@Adjacency(label="browser_screenshots")
	public Iterable<IScreenshotSet> getBrowserScreenshots();
	
	@Adjacency(label="browser_screenshots")
	public void addBrowserScreenshot(IScreenshotSet browser_screenshots);

	@Adjacency(label="contains")
	public Iterable<IPageElement> getElements();
	
	@Adjacency(label="contains")
	public void addElement(IPageElement elements);

	public void setBrowserScreenshots(Map<String, ScreenshotSet> browserScreenshots);
	
	
}
