package com.qanairy.models;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.joda.time.DateTime;

import com.qanairy.persistence.Group;
import com.qanairy.persistence.PageState;
import com.qanairy.persistence.PathObject;
import com.qanairy.persistence.Test;
import com.qanairy.persistence.TestRecord;



/**
 * Defines the path of a test, the result and the expected values to determine if a test was 
 * successful or not
 *
 */
public class TestPOJO extends Test{
    @SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(TestPOJO.class);
    
	private String key; 
	private String name;
	private List<TestRecord> records;
	private List<String> path_keys;
	private List<PathObject> path_objects;
	private PageState result;
	private Boolean correct;
	private boolean isUseful = false;
	private boolean spansMultipleDomains = false;
	private List<Group> groups;
	private Date last_run_time;
	private boolean is_running;
	private long run_time_length;
	private Map<String, Boolean> browser_passing_statuses;
	
	/**
	 * Constructs a test object
	 * 
	 * @param path {@link Path} that will be used to determine what the expected path should be
 	 * @param result
	 * @param domain
	 * 
	 * @pre path != null
	 */
	public TestPOJO(List< String> path_keys, List<PathObject> path_objects, PageState result, String name){
		assert path_keys != null;
		assert !path_keys.isEmpty();
		assert path_objects != null;
		assert !path_objects.isEmpty();
		
		setPathKeys(path_keys);
		setPathObjects(path_objects);
		setResult(result);
		setRecords(new ArrayList<TestRecord>());
		setCorrect(null);
		setSpansMultipleDomains(false);
		setGroups(new ArrayList<Group>());
		setLastRunTimestamp(null);
		setName(name);
		setBrowserStatuses(new HashMap<String, Boolean>());
		setIsRunning(false);
		setKey(generateKey());
		setRunTime(0L);
	}

	public TestPOJO(List<String> path_keys, List<PathObject> path_objects, PageState result, String name, boolean is_running, boolean spansMultipleDomains){
		assert path_keys != null;
		assert !path_keys.isEmpty();
		assert path_objects != null;
		assert !path_objects.isEmpty();
		
		setPathKeys(path_keys);
		setPathObjects(path_objects);
		setResult(result);
		setRecords(new ArrayList<TestRecord>());
		setCorrect(null);
		setSpansMultipleDomains(spansMultipleDomains);
		setGroups(new ArrayList<Group>());
		setLastRunTimestamp(null);
		setName(name);
		setBrowserStatuses(new HashMap<String, Boolean>());
		setIsRunning(false);
		setKey(generateKey());
		setRunTime(0L);
	}
	
	/**
	 * Checks if a {@code TestRecord} snapshot of a {@code TestPOJO} is passing or not
	 * 
	 * @param record
	 * @return
	 */
	public static Boolean isTestPassing(PageState expected_page, PageState new_result_page, Boolean last_test_passing_status){
		if((last_test_passing_status != null && !last_test_passing_status) && expected_page.getKey().equals(new_result_page.getKey())){
			System.err.println("LAST TEST PASSING STATUS ::  "+last_test_passing_status);
			System.err.println("ARE PAGES EQUAL????     "+expected_page.equals(new_result_page));
			last_test_passing_status = false; 
		}
		else if((last_test_passing_status == null || !last_test_passing_status) && !expected_page.getKey().equals(new_result_page.getKey())){
			last_test_passing_status = null;
		}
		else if((last_test_passing_status != null && last_test_passing_status) && expected_page.getKey().equals(new_result_page.getKey())){
			last_test_passing_status = true;
		}
		else if((last_test_passing_status != null && last_test_passing_status) && expected_page.getKey().equals(new_result_page.getKey())){
			System.err.println("Result page KEY      :::: "+new_result_page.getKey());
			System.err.println("Expected page key    :::: "+expected_page.getKey());
			System.err.println("LAST TEST PASSING STATUS ::  "+last_test_passing_status);
			System.err.println("ARE PAGES EQUAL????     "+expected_page.equals(new_result_page));
			last_test_passing_status = false;
		}
		
		System.err.println("Return value from isTestPassing()   ........    "+last_test_passing_status);
		return last_test_passing_status;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object o){
		if(o instanceof TestPOJO){
			
		}
		
		return false;
	}

	public Boolean getCorrect(){
		return this.correct;
	}
	
	public void setCorrect(Boolean correct){
		this.correct = correct;
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
	
	public List< String> getPathKeys(){
		return this.path_keys;
	}
	
	public void setPathKeys(List< String> path_keys){
		this.path_keys = path_keys;
	}
	
	public boolean addPathKey(String key) {
		return this.path_keys.add(key);
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
	
	/**
	 * @return result of running the test. Can be either null or have a {@link PageState} set
	 */
	public PageState getResult(){
		return this.result;
	}
	
	/**
	 * @param result_page expected {@link PageState} state after running through path
	 */
	public void setResult(PageState result_page){
		this.result = result_page;
	}

	public boolean isUseful() {
		return isUseful;
	}

	public void setUseful(boolean isUseful) {
		this.isUseful = isUseful;
	}

	public boolean getSpansMultipleDomains() {
		return spansMultipleDomains;
	}

	public void setSpansMultipleDomains(boolean spansMultipleDomains) {
		this.spansMultipleDomains = spansMultipleDomains;
	}

	public List<Group> getGroups() {
		return groups;
	}

	public void setGroups(List<Group> groups) {
		this.groups = groups;
	}
	
	public void addGroup(Group group){
		this.groups.add(group);
	}
	
	@Override
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

	public void setRunTime(long pathCrawlRunTime) {
		this.run_time_length = pathCrawlRunTime;
		
	}
	
	public long getRunTime() {
		return this.run_time_length;
	}

	public boolean isRunning() {
		return is_running;
	}

	public void setIsRunning(boolean is_running) {
		this.is_running = is_running;
	}

	public Map<String, Boolean> getBrowserStatuses() {
		return browser_passing_statuses;
	}

	public void setBrowserStatuses(Map<String, Boolean> browser_passing_statuses) {
		this.browser_passing_statuses = browser_passing_statuses;
	}

	

	@Override
	public void addPathObject(PathObject path_obj) {
		this.path_objects.add(path_obj);
	}

	@Override
	public List<PathObject> getPathObjects() {
		return this.path_objects;
	}

	private void setPathObjects(List<PathObject> path_objects) {
		this.path_objects = path_objects;
	}
	
	/**
	 * Generates a key using both path and result in order to guarantee uniqueness of key as well 
	 * as easy identity of {@link Test} when generated in the wild via discovery
	 * 
	 * @return
	 */
	public String generateKey() {
		String path_key =  String.join("::", getPathKeys());
		path_key += getResult().getKey();
		
		return path_key;
	}
	
	/**
	 * Clone {@link Test} object
	 * 
	 * @param path
	 * @return
	 */
	public static Test clone(Test test){
		Test clone_test = new TestPOJO(new ArrayList<String>(test.getPathKeys()),
									   new ArrayList<PathObject>(test.getPathObjects()),
									   test.getResult(),
									   test.getName(), false, test.getSpansMultipleDomains());
		
		clone_test.setBrowserStatuses(test.getBrowserStatuses());
		clone_test.setGroups(new ArrayList<Group>(test.getGroups()));
		clone_test.setLastRunTimestamp(test.getLastRunTimestamp());
		clone_test.setCorrect(test.getCorrect());
		clone_test.setRunTime(test.getRunTime());
		
		return clone_test;
	}
}
