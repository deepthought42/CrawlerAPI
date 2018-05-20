package com.qanairy.persistence;

import com.syncleus.ferma.AbstractVertexFrame;
import com.syncleus.ferma.annotations.Adjacency;
import com.syncleus.ferma.annotations.Property;

import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Frames interface implementation of {@link PagePOJO}
 *
 */
public abstract class PageState extends AbstractVertexFrame implements PathObject, Persistable {
	@Property("key")
	public abstract String getKey();
	
	@Property("key")
	public abstract void setKey(String key);
	
	@Property("landable")
	public abstract boolean isLandable();
	
	@Property("landable")
	public abstract void setLandable(boolean isLandable);
	
	@Property("url")
	public abstract URL getUrl();
	
	@Property("url")
	public abstract void setUrl(URL url);
	
	@Property("element_counts")
	public abstract Map<String, Integer> getElementCounts();
	
	@Property("element_counts")
	public abstract void setElementCounts(Map<String, Integer> element_count);

	@Property("total_weight")
	public abstract void setTotalWeight(Integer totalWeight);
	
	@Property("total_weight")
	public abstract Integer getTotalWeight();

	@Property("img_weight")
	public abstract void setImageWeight(Integer imageWeight);
	
	@Property("img_weight")
	public abstract Integer getImageWeight();
	
	@Property("src")
	public abstract String getSrc();

	@Property("src")
	public abstract void setSrc(String src);
	
	@Adjacency(label="browser_screenshots")
	public abstract List<ScreenshotSet> getBrowserScreenshots();
	
	@Adjacency(label="browser_screenshots")
	public abstract void addBrowserScreenshot(ScreenshotSet browser_screenshots);

	@Adjacency(label="contains")
	public abstract List<PageElement> getElements();
	
	@Adjacency(label="contains")
	public abstract void addElement(PageElement elements);

	public abstract void setBrowserScreenshots(List<ScreenshotSet> browser_screenshots);

	public abstract void setElements(List<PageElement> elements);
}
