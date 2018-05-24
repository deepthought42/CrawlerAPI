package com.qanairy.persistence;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.tinkerpop.gremlin.structure.Direction;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.qanairy.models.dao.PathObjectDao;
import com.qanairy.models.dao.impl.PathObjectDaoImpl;
import com.syncleus.ferma.AbstractVertexFrame;
import com.syncleus.ferma.annotations.Adjacency;
import com.syncleus.ferma.annotations.Property;
import com.qanairy.persistence.serializers.TestSerializer;

/**
 * Test object data access interface for use with tinkerpop/frames
 *
 */
@JsonSerialize(using = TestSerializer.class)
public abstract class Test extends AbstractVertexFrame implements Persistable{
	/**
	 * @return the key for the current test
	 */
	@Property("key")
	public abstract String getKey();
	
	/**
	 * sets the key for the current test
	 */
	@Property("key")
	public abstract void setKey(String key);
	
	/**
	 * @return the name for the current test
	 */
	@Property("name")
	public abstract String getName();
	
	/**
	 * sets the name for the current test
	 */
	@Property("name")
	public abstract void setName(String name);
	
	/**
	 * Gets the correctness value of the test
	 * 
	 * @return Correctness value. Null indicates value is unset.
	 */
	@Property("correct")
	public abstract Boolean getCorrect();
	
	/**
	 * Sets correctness value of test
	 * 
	 * @param correctness value
	 */
	@Property("correct")
	public abstract void setCorrect(Boolean correct);
	
	/**
	 * @return date timestamp of when test was last ran
	 */
	@Property("last_ran")
	public abstract Date getLastRunTimestamp();
	
	/**
	 * sets date timestamp of when test was last ran
	 */
	@Property("last_ran")
	public abstract void setLastRunTimestamp(Date timestamp);
	
	/**
	 * @return date timestamp of when test was last ran
	 */
	@Property("run_time")
	public abstract long getRunTime();
	
	/**
	 * sets date timestamp of when test was last ran
	 */
	@Property("run_time")
	public abstract void setRunTime(long milliseconds);

	@Property("browser_run_statuses")
	public abstract void setBrowserStatuses(Map<String, Boolean> browser_statuses);
	
	
	@Property("browser_run_statuses")
	public abstract Map<String, Boolean> getBrowserStatuses();

	/**
	 * @return whether or not this path goes into another domain
	 */
	@Property("spansMultipleDomains")
	public abstract boolean getSpansMultipleDomains();

	/**
	 * @return whether or not this path goes into another domain
	 */
	@Property("spansMultipleDomains")
	public abstract void setSpansMultipleDomains(boolean spanningMultipleDomains);

	@Property("path_list")
	public abstract void setPathKeys(List<String> path_obj_key_list);
	
	/**
	 * @return {@link List} of {@link PathObject}s representing a path sequence
	 */
	@Property("path_keys")
	public abstract List<String> getPathKeys();
	
	@Adjacency(label="contains")
	public abstract void addPathObject(PathObject path_obj);
	
	@Adjacency(label="contains")
	public abstract List<PathObject> getPathObjects();
	
	/**
	 * Gets the correctness value of the test
	 * 
	 * @return Correctness value. Null indicates value is unset.
	 */
	@Adjacency(direction=Direction.IN, label="test_group")
	public abstract List<Group> getGroups();
	
	/**
	 * Sets correctness value of test
	 * 
	 * @param correctness value
	 */
	@Adjacency(direction=Direction.IN, label="test_group")
	public abstract void setGroups(List<Group> groups);
	
	/**
	 * Sets correctness value of test
	 * 
	 * @param correctness value
	 */
	@Adjacency(direction=Direction.IN, label="test_group")
	public abstract boolean addGroup(Group group);
	
	/**
	 * Sets correctness value of test
	 * 
	 * @param correctness value
	 */
	@Adjacency(direction=Direction.IN, label="test_group")
	public abstract void removeGroup(Group group);
	
	/**
	 * @return {@link Page} experienced as a result of executing the path
	 */
	@Adjacency(label="result")
	public abstract PageState getResult();
	
	/**
	 * Sets the {@link Page} that is the result of executing the path 
	 */
	@Adjacency(label="result")
	public abstract void setResult(PageState page);

	
	/**
	 * @return {@link Iterator} of {@link TestRecord}s that this test "has"
	 */
	@Adjacency(direction=Direction.OUT, label="has_record")
	public abstract List<TestRecord> getRecords();
	
	/**
	 * Sets the {@link TestRecord} that is the result of executing the path
	 */
	@Adjacency(direction=Direction.OUT, label="has_record")
	public abstract void setRecords(List<TestRecord> page);

	/**
	 * Adds a record to this test connecting it via edge with label "has"
	 */
	@Adjacency(direction=Direction.OUT, label="has_record")
	public abstract void addRecord(TestRecord testRecord);
	
	/**
	 * 
	 * @param browser_name name of browser (ie 'chrome', 'firefox')
	 * @param status boolean indicating passing or failing
	 * 
	 * @pre browser_name != null
	 */
	public void setBrowserStatus(String browser_name, Boolean status){
		assert browser_name != null;
		getBrowserStatuses().put(browser_name, status);
	}
	/**
	 * 
	 * @return
	 */
	public PageState firstPage() {
		PathObjectDao path_obj_dao = new PathObjectDaoImpl();
		for(String key : this.getPathKeys()){
			PathObject path_obj = path_obj_dao.find(key);
			if(path_obj instanceof PageState){
				return (PageState)path_obj;
			}
		}
		return null;
	}
	
	/**
	 * Gets the last Vertex in a path that is of type {@link PageState}
	 * 
	 * @return
	 */
	public PageState findLastPage(){
		List<String> path_keys = this.getPathKeys();
		PageState page = null;

		PathObjectDao path_obj_dao = new PathObjectDaoImpl();
		for(String key : path_keys){
			PathObject path_obj = path_obj_dao.find(key);
			if(path_obj != null && path_obj.getType().equals("PageState")){
				page = (PageState)path_obj;
			}
		}

		return page;
	}
}