package com.qanairy.persistence;

import com.tinkerpop.frames.Property;

/**
 * Tinkerpop/frames interface implementation of {@link PageSource}
 *
 */
public interface IPageSource extends IPathObject{
	@Property("key")
	public String getKey();
	
	@Property("key")
	public void setKey(String key);
		
	@Property("src")
	public String getSrc();

	@Property("src")
	public void setSrc(String src);
}
