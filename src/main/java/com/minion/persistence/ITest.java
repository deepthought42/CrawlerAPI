package com.minion.persistence;

import java.util.Iterator;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.modules.typedgraph.TypeValue;

import com.minion.browsing.Page;
import com.minion.structs.Path;
import com.minion.tester.TestRecord;

/**
 * Test object data access interface for use with tinkerpop/frames
 * 
 * @author Brandon Kindred
 *
 */
@TypeValue(value="Test") public interface ITest  {
	/**
	 * @return the key for the current test
	 */
	@Property("key")
	public String getKey();
	
	/**
	 * sets the key for the current test
	 */
	@Property("key")
	public void setKey(String key);
	
	/**
	 * @return the domain for the current test
	 */
	@Property("domain")
	public String getDomain();
	
	/**
	 * sets the domain for the current test
	 */
	@Property("domain")
	public void setDomain(String url);
	
	/**
	 * @return the name for the current test
	 */
	@Property("name")
	public String getName();
	
	/**
	 * sets the name for the current test
	 */
	@Property("name")
	public void setName(String name);
	
	/**
	 * Gets the correctness value of the test
	 * 
	 * @return Correctness value. Null indicates value is unset.
	 */
	@Property("correct")
	public Boolean getCorrect();
	
	/**
	 * Sets correctness value of test
	 * 
	 * @param correctness value
	 */
	@Property("correct")
	public void setCorrect(Boolean correct);
	
	/**
	 * @return {@link Iterator} of {@link TestRecord}s that this test "has"
	 */
	@Adjacency(label="has")
	public Iterator<ITestRecord> getRecords();
	
	/**
	 * Adds a record to this test connecting it via edge with label "has"
	 */
	@Adjacency(label="has_record")
	public void addRecord(ITestRecord testRecord);
		
	/**
	 * @return {@link Path} that this test was created for
	 */
	@Adjacency(label="path")
	public IPath getPath();
	
	/**
	 * Sets the path for this test
	 */
	@Adjacency(label="path")
	public void setPath(IPath path); 
	
	/**
	 * @return {@link Page} experienced as a result of executing the path
	 */
	@Adjacency(label="result")
	public IPage getResult();
	
	/**
	 * Sets the {@link Page} that is the result of executing the path 
	 */
	@Adjacency(label="result")
	public void setResult(IPage page);
}