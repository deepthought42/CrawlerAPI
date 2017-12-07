package com.qanairy.persistence;

import java.util.Date;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.modules.typedgraph.TypeValue;

/**
 * 
 */
@TypeValue(value="TestRecord") 
public interface ITestRecord {
	@Property("key")
	public String getKey();
	
	@Property("key")
	public void setKey(String key);
	
	@Property("ran_at")
	public Date getRanAt();
	
	@Property("ran_at")
	public void setRanAt(Date date);
	
	@Property("passes")
	public Boolean getPasses();
	
	@Property("passes")
	public void setPasses(Boolean isPassing);
	
	@Adjacency(label="has_result")
	public void setResult(IPage page);
	
	@Adjacency(label="has_result")
	public IPage getResult();
	
}
