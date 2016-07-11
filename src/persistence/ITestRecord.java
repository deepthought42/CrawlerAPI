package persistence;

import java.util.Date;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;

/*
 * 
 */
public interface ITestRecord extends IPersistable<ITestRecord> {
	@Property("key")
	public String getKey();
	
	@Property("key")
	public void setKey(String key);
	
	@Adjacency(label="result")
	public IPage getResult();
	
	@Adjacency(label="result")
	public void setResult(IPage page);
	
	@Property("ran_at")
	public Date getRanAt();
	
	@Property("ran_at")
	public void setRanAt(Date date);
	
	@Property("passes")
	public boolean getPasses();
	
	@Property("passes")
	public void setPasses(boolean isPassing);
	
	@Property("correct")
	public Boolean isCorrect();
	
	@Property("correct")
	public void setCorrect(Boolean isCorrect);
}
