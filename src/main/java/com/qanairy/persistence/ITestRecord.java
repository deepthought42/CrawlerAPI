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
	
	@Property("passing")
	public Boolean getPassing();
	
	@Property("passing")
	public void setPassing(Boolean isPassing);
	
	@Property("browser_name")
	public String getBrowser();
	
	@Property("browser_name")
	public void setBrowser(String browser_name);
	
	@Property("run_time")
	public long getRunTime();
	
	@Property("run_time")
	public void setRunTime(long run_time);
	
	@Adjacency(label="has_result")
	public void setResult(IPage page);
	
	@Adjacency(label="has_result")
	public IPage getResult();
	
}
