package com.qanairy.models;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;
import org.joda.time.DateTime;

import com.qanairy.models.dto.TestRepository;
import com.qanairy.persistence.OrientConnectionFactory;
import com.qanairy.persistence.PageState;
import com.qanairy.persistence.Path;
import com.qanairy.persistence.TestRecord;



/**
 * Defines the path of a test, the result and the expected values to determine if a test was 
 * successful or not
 *
 */
public class TestPOJO {
    @SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(Test.class);
    
	private String key; 
	private String name;
	private List<TestRecord> records;
	private Path path;
	private PageState result;
	private Domain domain;
	private Boolean correct;
	private boolean isUseful = false;
	private boolean spansMultipleDomains = false;
	private List<Group> groups;
	private Date last_run_time;
	private boolean is_running;
	private long run_time_length;
	private Map<String, Boolean> browser_passing_statuses;

	/**
	 * Construct a test with defaults of useful set to fault and 
	 * spansMultipleDomains set to false
	 */
	public TestPOJO(){
		this.setRecords(new ArrayList<TestRecord>());
		this.setSpansMultipleDomains(false);
		this.setGroups(new ArrayList<Group>());
		this.setLastRunTimestamp(null);
		this.setIsRunning(false);
		this.setBrowserPassingStatuses(new HashMap<String, Boolean>());
	}
	
	/**
	 * Constructs a test object
	 * 
	 * @param path {@link Path} that will be used to determine what the expected path should be
 	 * @param result
	 * @param domain
	 * 
	 * @pre path != null
	 */
	public TestPOJO(Path path, PageState result, Domain domain, String name){
		assert path != null;
		
		this.setPath(path);
		this.setResult(result);
		this.setRecords(new ArrayList<TestRecord>());
		this.setDomain(domain);
		this.setCorrect(null);
		this.setSpansMultipleDomains(false);
		this.setGroups(new ArrayList<Group>());
		this.setLastRunTimestamp(null);
		this.setKey(null);
		this.setName(name);
		this.setBrowserPassingStatuses(new HashMap<String, Boolean>());
		this.setIsRunning(false);
	}
	
	/**
	 * Constructs a test object
	 * 
	 * @param path {@link Path} that will be used to determine what the expected path should be
 	 * @param result
	 * @param domain
	 * 
	 * @pre path != null
	 */
	public TestPOJO(String key, Path path, PageState result, Domain domain, String name){
		assert path != null;
		
		this.setPath(path);
		this.setResult(result);
		this.setRecords(new ArrayList<TestRecord>());
		this.setDomain(domain);
		this.setCorrect(null);
		this.setSpansMultipleDomains(false);
		this.setGroups(new ArrayList<Group>());
		this.setLastRunTimestamp(null);
		this.setKey(key);
		this.setName(name);
		this.setBrowserPassingStatuses(new HashMap<String, Boolean>());
		this.setIsRunning(false);
	}
	
	public TestPOJO(String key, Path path, PageState result, Domain domain, String name, boolean is_running){
		assert path != null;
		
		this.setPath(path);
		this.setResult(result);
		this.setRecords(new ArrayList<TestRecord>());
		this.setDomain(domain);
		this.setCorrect(null);
		this.setSpansMultipleDomains(false);
		this.setGroups(new ArrayList<Group>());
		this.setLastRunTimestamp(null);
		this.setKey(key);
		this.setName(name);
		this.setBrowserPassingStatuses(new HashMap<String, Boolean>());
		this.setIsRunning(false);
	}
	
	/**
	 * Checks if a {@code TestRecord} snapshot of a {@code TestPOJO} is passing or not
	 * 
	 * @param record
	 * @return
	 */
	public Boolean isTestPassing(PageState page, Boolean last_test_passing_status){
		if((last_test_passing_status != null && !last_test_passing_status) && this.getResult().equals(page)){
			last_test_passing_status = false; 
		}
		else if((last_test_passing_status == null || !last_test_passing_status) && !this.getResult().equals(page)){
			last_test_passing_status = null;
		}
		else if((last_test_passing_status != null && last_test_passing_status) && this.getResult().equals(page)){
			last_test_passing_status = true;
		}
		else if((last_test_passing_status != null && last_test_passing_status) && !this.getResult().equals(page)){
			last_test_passing_status = false;
		}
		
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
	
	/**
	 * {@inheritDoc}
	 */
	public static Iterable<Test> findByKey(String generated_key, OrientConnectionFactory orient_connection) {
		return orient_connection.getTransaction().getFramedVertices("key", generated_key, Test.class);
	}

	/**
	 * {@inheritDoc}
	 * @throws MalformedURLException 
	 */
	public static List<Test> findByUrl(String pageUrl) throws MalformedURLException {
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		Iterator<Test> test_iter = orient_connection.getTransaction().getVertices("domain", pageUrl, Test.class).iterator();
		
		ArrayList<Test> list = new ArrayList<Test>();
		TestRepository test_record = new TestRepository();

		while(test_iter.hasNext()){
			Test itest = test_iter.next();
			Test test = test_record.load(itest);
			list.add(test);
		}
		orient_connection.close();
		
		return list;
	}
	
	/**
	 * {@inheritDoc}
	 * @throws MalformedURLException 
	 */
	public static List<Test> findByLandable(boolean isLandable) {
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		Iterator<Test> test_iter = orient_connection.getTransaction().getVertices("landable", isLandable, Test.class).iterator();
		
		ArrayList<Test> list = new ArrayList<Test>();
		TestRepository test_record = new TestRepository();

		while(test_iter.hasNext()){
			Test itest = test_iter.next();
			
			Test test = test_record.load(itest);
			list.add(test);
		}
		orient_connection.close();
		return list;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public static List<Test> findByName(String test_name) {
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		Iterator<Test> test_iter = orient_connection.getTransaction().getVertices("name", test_name, Test.class).iterator();
		
		ArrayList<Test> list = new ArrayList<Test>();
		TestRepository test_record = new TestRepository();

		while(test_iter.hasNext()){
			Test itest = test_iter.next();
			
			Test test = test_record.load(itest);
			list.add(test);
		}
		
		orient_connection.close();
		return list;
	}
	
	/**
	 * 
	 * @param test_name
	 * @return
	 */
	public static List<Test> findBySource(String source) {
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		Iterator<Test> test_iter = orient_connection.getTransaction().getVertices("name", source, Test.class).iterator();
		
		ArrayList<Test> list = new ArrayList<Test>();
		TestRepository test_record = new TestRepository();

		while(test_iter.hasNext()){
			Test itest = test_iter.next();	
			
			Test test = test_record.load(itest);
			list.add(test);
		}
		orient_connection.close();
		return list;
	}
	
	public Boolean isCorrect(){
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
	
	public Domain getDomain(){
		return this.domain;
	}
	
	public void setDomain(Domain domain){
		this.domain = domain;
	}
	
	/**
	 * 
	 * @param browser_name name of browser (ie 'chrome', 'firefox')
	 * @param status boolean indicating passing or failing
	 * 
	 * @pre browser_name != null
	 */
	public void setBrowserStatus(String browser_name, Boolean status){
		assert browser_name != null;
		this.browser_passing_statuses.put(browser_name, status);
	}
	
	public Path getPath(){
		return this.path;
	}
	
	public void setPath(Path path){
		this.path = path;
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

	public boolean isSpansMultipleDomains() {
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
	
	public boolean addGroup(Group group){
		return this.groups.add(group);
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

	public Map<String, Boolean> getBrowserPassingStatuses() {
		return browser_passing_statuses;
	}

	public void setBrowserPassingStatuses(Map<String, Boolean> browser_passing_statuses) {
		this.browser_passing_statuses = browser_passing_statuses;
	}
}
