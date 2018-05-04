package com.qanairy.persistence;

import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.modules.typedgraph.TypeValue;

@TypeValue("IScreenshotSet") 
public interface IScreenshotSet {
	@Property("key")
	public String getKey();
	
	@Property("key")
	public void setKey(String key);
	
	@Property("page_key")
	public String getPageKey();

	@Property("page_key")
	public void setPageKey(String key);
	
	@Property("browser")
	public String getBrowser();

	@Property("browser")
	public void setBrowser(String browser_name);
	
	@Property("full_screenshot")
	public String getFullScreenshot();

	@Property("full_screenshot")
	public void setFullScreenshot(String full_src);

	@Property("viewport_screenshot")
	public String getViewportScreenshot();

	@Property("viewport_screenshot")
	public void setViewportScreenshot(String viewport_url);

}
