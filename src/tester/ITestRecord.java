package tester;

import java.util.Date;

import com.tinkerpop.frames.Property;

import browsing.IPage;
import browsing.Page;

public interface ITestRecord {
	
	@Property("result")
	public IPage getResult();
	
	@Property("result")
	public void setResult(Page page);
	
	@Property("ranAt")
	public Date getRanAt();
	
	@Property("ranAt")
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
