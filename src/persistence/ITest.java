package persistence;

import java.util.Iterator;
import java.util.List;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;

import browsing.Page;
import structs.Path;
import tester.TestRecord;

/**
 * Test object data access interface for use with tinkerpop/frames
 * 
 * @author Brandon Kindred
 *
 */
public interface ITest {
	/**
	 * @return the key for the current test
	 */
	@Property("key")
	public int getKey();
	
	/**
	 * sets the key for the current test
	 */
	@Property("key")
	public void setKey(String key);
	
	/**
	 * @return {@link Iterator} of {@link TestRecord}s that this test "has"
	 */
	@Adjacency(label="has")
	public Iterator<ITestRecord> getRecords();
	
	/**
	 * Adds a record to this test connecting it via edge with label "has"
	 */
	@Adjacency(label="has")
	public void setRecords(List<TestRecord> testRecord);
		
	/**
	 * @return {@link Path} that this test was created for
	 */
	@Property("path")
	public IPath getPath();
	
	/**
	 * Sets the path for this test
	 */
	@Property("path")
	public void setPath(IPath path); 
	
	/**
	 * @return {@link Page} experienced as a result of executing the path
	 */
	@Property("result")
	public IPage getResult();
	
	/**
	 * Sets the {@link Page} that is the result of executing the path 
	 */
	@Property("result")
	public void setResult(IPage page);
}