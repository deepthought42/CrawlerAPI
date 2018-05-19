package com.qanairy.persistence;

import java.util.Date;

import com.syncleus.ferma.AbstractVertexFrame;
import com.syncleus.ferma.annotations.Adjacency;
import com.syncleus.ferma.annotations.Property;

/**
 * 
 */
public abstract class TestRecord extends AbstractVertexFrame {
	@Property("key")
	public abstract String getKey();
	
	@Property("key")
	public abstract void setKey(String key);
	
	@Property("ran_at")
	public abstract Date getRanAt();
	
	@Property("ran_at")
	public abstract void setRanAt(Date date);
	
	@Property("passing")
	public abstract Boolean getPassing();
	
	@Property("passing")
	public abstract void setPassing(Boolean isPassing);
	
	@Property("browser_name")
	public abstract String getBrowser();
	
	@Property("browser_name")
	public abstract void setBrowser(String browser_name);
	
	@Property("run_time")
	public abstract long getRunTime();
	
	@Property("run_time")
	public abstract void setRunTime(long run_time);
	
	@Adjacency(label="has_result")
	public abstract void setResult(PageState page);
	
	@Adjacency(label="has_result")
	public abstract PageState getResult();
}
