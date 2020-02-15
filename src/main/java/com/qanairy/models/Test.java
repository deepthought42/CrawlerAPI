package com.qanairy.models;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.qanairy.models.enums.TestStatus;

import org.joda.time.DateTime;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Properties;
import org.neo4j.ogm.annotation.Relationship;

/**
 * Defines the path of a test, the result and the expected values to determine if a test was 
 * successful or not
 *
 */
@NodeEntity
public abstract class Test implements Persistable {
    @SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(Test.class);
	
    @GeneratedValue
    @Id
	private Long id;
	
	private String key; 
	private String name;
	private TestStatus status;
	private boolean is_running;
	private boolean archived;
	private Date last_run_time;

	@Properties
	private Map<String, String> browser_passing_statuses = new HashMap<>();
	
	@Relationship(type = "HAS_TEST_RECORD")
	private List<TestRecord> records = new ArrayList<>();
	
	@Relationship(type = "HAS_GROUP")
	private Set<Group> groups = new HashSet<>();
	
	public Test(){}
	
	/**
	 * Constructs a test object
	 * 
	 * @param path {@link Path} that will be used to determine what the expected path should be
 	 * @param result
	 * @param domain
	 * @throws MalformedURLException 
	 * 
	 * @pre path_keys != null
	 * @pre !path_keys.isEmpty()
	 * @pre path_objects != null
	 * @pre !path_objects.isEmpty()
	 */
	@Deprecated
	public Test(List<String> path_keys, List<PathObject> path_objects, PageState result, boolean spansMultipleDomains) throws MalformedURLException{
		assert path_keys != null;
		assert !path_keys.isEmpty();
		assert path_objects != null;
		assert !path_objects.isEmpty();
		
		setRecords(new ArrayList<TestRecord>());
		setStatus(TestStatus.UNVERIFIED);
		setLastRunTimestamp(new Date());
		setName(generateTestName());
		setBrowserStatuses(new HashMap<String, String>());
		setArchived(false);
		setIsRunning(false);
		setKey(generateKey());
	}
	
	/**
	 * Constructs a test object
	 * 
	 * @param path {@link Path} that will be used to determine what the expected path should be
 	 * @param result
	 * @param domain
	 * @throws MalformedURLException 
	 * 
	 * @pre path_keys != null
	 * @pre !path_keys.isEmpty()
	 * @pre path_objects != null
	 * @pre !path_objects.isEmpty()
	 */
	@Deprecated
	public Test(List<String> path_keys, List<PathObject> path_objects, PageState result, String name, boolean is_running, boolean spansMultipleDomains) throws MalformedURLException{
		assert path_keys != null;
		assert !path_keys.isEmpty();
		assert path_objects != null;
		assert !path_objects.isEmpty();
		
		setPath(path_keys);
		setRecords(new ArrayList<TestRecord>());
		setStatus(TestStatus.UNVERIFIED);
		setLastRunTimestamp(new Date());
		setName(name);
		setBrowserStatuses(new HashMap<String, String>());
		setIsRunning(is_running);
		setArchived(false);
		setKey(generateKey());
	}
	
	/**
	 * Checks if a {@code TestRecord} snapshot of a {@code Test} is passing or not
	 * 
	 * @param record
	 * @return
	 */
	public static TestStatus isTestPassing(PageState expected_page, PageState new_result_page, TestStatus last_test_passing_status){
		assert expected_page != null;
		assert new_result_page != null;
		assert last_test_passing_status != null;
		
		if(last_test_passing_status.equals(TestStatus.FAILING) && expected_page.equals(new_result_page)){
			return TestStatus.FAILING; 
		}
		else if(last_test_passing_status.equals(TestStatus.FAILING) && !expected_page.equals(new_result_page)){
			return TestStatus.UNVERIFIED;
		}
		else if(last_test_passing_status.equals(TestStatus.PASSING) && expected_page.equals(new_result_page)){
			return TestStatus.PASSING;
		}
		else if(last_test_passing_status.equals(TestStatus.PASSING) && !expected_page.equals(new_result_page)){
			return TestStatus.FAILING;
		}
		
		return last_test_passing_status;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object o){
		if(o instanceof Test){
			Test test = (Test)o;
			
			return test.getKey().equals(this.getKey());
		}
		
		return false;
	}

	public TestStatus getStatus(){
		return this.status;
	}
	
	public void setStatus(TestStatus status){
		this.status = status;
	}
	
	public String getKey(){
		return this.key;
	}
	
	public void setKey(String key){
		this.key = key;
	}
	
	public String getName(){
		return this.name;
	}
	
	public void setName(String name){
		this.name = name;
	}
	
	public void addRecord(TestRecord record){
		this.records.add(record);
	}
	
	public List<TestRecord> getRecords(){
		return this.records;
	}
	
	public void setRecords(List<TestRecord> records){
		this.records = records;
	}

	public Set<Group> getGroups() {
		return groups;
	}

	public void setGroups(Set<Group> groups) {
		this.groups = groups;
	}
	
	public void addGroup(Group group){
		this.groups.add(group);
	}
	
	public void removeGroup(Group group) {
		//remove edge between test and group
		this.groups.remove(group);
	}
	
	/**
	 * @return date timestamp of when test was last ran
	 */
	public Date getLastRunTimestamp(){
		return this.last_run_time;
	}
	
	/**
	 * sets date timestamp of when test was last ran
	 * 
	 * @param timestamp of last run as a {@link DateTime}
	 */
	public void setLastRunTimestamp(Date timestamp){
		this.last_run_time = timestamp;
	}

	public boolean isRunning() {
		return is_running;
	}

	public void setIsRunning(boolean is_running) {
		this.is_running = is_running;
	}

	public Map<String, String> getBrowserStatuses() {
		return browser_passing_statuses;
	}

	public void setBrowserStatuses(Map<String, String> browser_passing_statuses) {
		this.browser_passing_statuses = browser_passing_statuses;
	}
	
	/**
	 * 
	 * @param browser_name name of browser (ie 'chrome', 'firefox')
	 * @param status boolean indicating passing or failing
	 * 
	 * @pre browser_name != null
	 */
	public void setBrowserStatus(String browser_name, String status){
		assert browser_name != null;
		getBrowserStatuses().put(browser_name, status);
	}
	
	/**
	 * Clone {@link Test} object
	 * 
	 * @param path
	 * @return
	 * @throws MalformedURLException 
	 */
	public static Test clone(Test test) throws MalformedURLException{
		Test clone_test = new Test(new ArrayList<String>(test.getPathKeys()),
									   new ArrayList<PathObject>(test.getPathObjects()),
									   test.getResult());

		clone_test.setBrowserStatuses(test.getBrowserStatuses());
		clone_test.setGroups(new HashSet<>(test.getGroups()));
		clone_test.setLastRunTimestamp(test.getLastRunTimestamp());
		clone_test.setStatus(test.getStatus());
		
		return clone_test;
	}

	public boolean isArchived() {
		return archived;
	}

	public void setArchived(boolean is_archived) {
		this.archived = is_archived;
	}
	
	public String generateTestName() throws MalformedURLException {
		 String test_name = "";
			int page_state_idx = 0;
			int element_action_cnt = 0;
			for(PathObject obj : this.path_objects){
				if(obj instanceof PageState && page_state_idx < 1){
					String path = (new URL(((PageState)obj).getUrl())).getPath().trim();
					path = path.replace("/", " ");
					path = path.trim();
					if("/".equals(path) || path.isEmpty()){
						path = "home";
					}
					test_name +=  path + " page ";
					page_state_idx++;
				}
				else if(obj instanceof ElementState){
					if(element_action_cnt > 0){
						test_name += "> ";
					}
					
					ElementState element = (ElementState)obj;
					String tag_name = element.getName();
					
					if(element.getAttribute("id") != null){
						tag_name = element.getAttribute("id").getVals().get(0);
					}
					else{
						if("a".equals(tag_name)){
							tag_name = "link";
						}
					}
					test_name += tag_name + " ";
					element_action_cnt++;
				}
				else if(obj instanceof Action){
					Action action = ((Action)obj);
					test_name += action.getName() + " ";
					if(action.getValue() != null ){
						test_name += action.getValue() + " ";
					}
				}
			}
			
			return test_name.trim();
	}
}
