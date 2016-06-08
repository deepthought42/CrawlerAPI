package tester;

import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import browsing.PathObject;
import structs.Path;

/**
 * A record for when a path was observed
 * 
 * @author Brandon Kindred
 *
 */
public class TestRecord {
	
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(Test.class);

	public final List<PathObject> result_path;
	public final Date ran_at;
	public final boolean passes;
	public Boolean isCorrect;
	
	public TestRecord(Path path, Date ran_at, boolean passes){
		this.result_path = path.getPath();
		this.ran_at = ran_at;
		this.passes = passes;
		this.isCorrect = null;
	}
	
	/**
	 * @return the path that was observed. This may defer from the actual test path
	 */
	public List<PathObject> getResultPath(){
		return result_path;
	}
	
	/**
	 * @return {@link Date} when test was ran
	 */
	public Date getRanAt(){
		return ran_at;
	}
	
	/**
	 * @return whether or not the test passes compared to expected {@link Test test} path
	 */
	public boolean getPasses(){
		return this.passes;
	}
	
	/**
	 * Sets the correctness of the test. If the resulting path deviates from the original 
	 * path then it is incorrect
	 * 
	 * @param isCorrect
	 */
	public Boolean isCorrect(){
		return this.isCorrect;
	}
	
	public void setIsCorrect(boolean isCorrect){
		this.isCorrect = isCorrect;
	}
}
