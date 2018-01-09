package com.qanairy.persistence;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.qanairy.models.TestRecord;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.modules.typedgraph.TypeValue;

/**
 * Test object data access interface for use with tinkerpop/frames
 *
 */
@TypeValue(value="Test") 
public interface ITest  {
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
	 * @return date timestamp of when test was last ran
	 */
	@Property("last_ran")
	public Date getLastRunTimestamp();
	
	/**
	 * sets date timestamp of when test was last ran
	 */
	@Property("last_ran")
	public void setLastRunTimestamp(Date timestamp);
	
	/**
	 * @return date timestamp of when test was last ran
	 */
	@Property("run_time")
	public long getRunTime();
	
	/**
	 * sets date timestamp of when test was last ran
	 */
	@Property("run_time")
	public void setRunTime(long milliseconds);
	
	@Property("run_status")
	public void setRunStatus(boolean status);
	
	@Property("run_status")
	public boolean getRunStatus();
	
	/**
	 * @return the domain for the current test
	 */
	@Adjacency(direction=Direction.IN, label="contains_test")
	public IDomain getDomain();
	
	/**
	 * sets the domain for the current test
	 */
	@Adjacency(direction=Direction.IN, label="contains_test")
	public void addDomain(IDomain domain);
	
	
	/**
	 * Gets the correctness value of the test
	 * 
	 * @return Correctness value. Null indicates value is unset.
	 */
	@Adjacency(direction=Direction.IN, label="test_group")
	public Iterable<IGroup> getGroups();
	
	/**
	 * Sets correctness value of test
	 * 
	 * @param correctness value
	 */
	@Adjacency(direction=Direction.IN, label="test_group")
	public void setGroups(List<IGroup> groups);
	
	/**
	 * Sets correctness value of test
	 * 
	 * @param correctness value
	 */
	@Adjacency(direction=Direction.IN, label="test_group")
	public void addGroup(IGroup group);
	
	/**
	 * Sets correctness value of test
	 * 
	 * @param correctness value
	 */
	@Adjacency(direction=Direction.IN, label="test_group")
	public void removeGroup(IGroup group);
	
	/**
	 * Adds a record to this test connecting it via edge with label "has"
	 */
	@Adjacency(direction=Direction.OUT, label="has_record")
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

	
	/**
	 * @return {@link Iterator} of {@link TestRecord}s that this test "has"
	 */
	@Adjacency(direction=Direction.OUT, label="has_record")
	public Iterable<ITestRecord> getRecords();
	
	/**
	 * Sets the {@link TestRecord} that is the result of executing the path
	 */
	@Adjacency(direction=Direction.OUT, label="has_record")
	public void setRecords(List<TestRecord> page);
}