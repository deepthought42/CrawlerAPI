package com.qanairy.persistence;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.qanairy.persistence.serializers.ScreenshotSetSerializer;
import com.syncleus.ferma.AbstractVertexFrame;
import com.syncleus.ferma.annotations.Property;

/**
 * 
 */
@JsonSerialize(using = ScreenshotSetSerializer.class)
public abstract class ScreenshotSet extends AbstractVertexFrame implements Persistable {
	@Property("key")
	public abstract String getKey();
	
	@Property("key")
	public abstract void setKey(String key);
	
	@Property("browser")
	public abstract String getBrowser();

	@Property("browser")
	public abstract void setBrowser(String browser_name);
	
	@Property("full_screenshot")
	public abstract String getFullScreenshot();

	@Property("full_screenshot")
	public abstract void setFullScreenshot(String full_src);

	@Property("viewport_screenshot")
	public abstract String getViewportScreenshot();

	@Property("viewport_screenshot")
	public abstract void setViewportScreenshot(String viewport_url);
}
