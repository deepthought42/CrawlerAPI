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
import com.qanairy.persistence.ITest;
import com.qanairy.persistence.OrientConnectionFactory;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.frames.FramedTransactionalGraph;

/**
 * Defines the path of a test, the result and the expected values to determine if a test was 
 * successful or not
 *
 */
public class Test {
    @SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(Test.class);
    
	private String key; 
	private String name;
	private List<TestRecord> records;
	private Path path;
	private Page result;
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
	public Test(){
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
	public Test(Path path, Page result, Domain domain, String name){
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
	public Test(String key, Path path, Page result, Domain domain, String name){
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
	
	public Test(String key, Path path, Page result, Domain domain, String name, boolean is_running){
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
	 * Checks if a {@code TestRecord} snapshot of a {@code Test} is passing or not
	 * 
	 * @param record
	 * @return
	 */
	public Boolean isTestPassing(Page page, Boolean last_test_passing_status){
		System.err.println("IS TEST PASSING?     ----------------------    "+page.getElements().size());
		System.err.println("THIS TEST RESULT **----------------------    "+this.getResult());
		System.err.println("THIS TEST RESULT ELEMENTS **----------------------    "+this.getResult().getElements().size());

		if((last_test_passing_status != null && !last_test_passing_status) && this.getResult().equals(page)){
			System.err.println("Pages are equal and test is NOT marked as passing");
			last_test_passing_status = false; 
		}
		else if((last_test_passing_status == null || !last_test_passing_status) && !this.getResult().equals(page)){
			System.err.println("Pages are NOT equal and test is NOT marked as passing");
			last_test_passing_status = null;
		}
		else if((last_test_passing_status != null && last_test_passing_status) && this.getResult().equals(page)){
			System.err.println("pages are equal and test is marked as passing");
			last_test_passing_status = true;
		}
		else if((last_test_passing_status != null && last_test_passing_status) && !this.getResult().equals(page)){
			System.err.println("pages are NOT equal and test is marked as passing");
			last_test_passing_status = false;
		}
		
		return last_test_passing_status;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object o){
		if(o instanceof Test){
			
		}
		
		return false;
	}
		
	/**
	 * 
	 * @param framedGraph
	 * @param id
	 * @return
	 */
	public ITest findById(FramedTransactionalGraph<OrientGraph> framedGraph, String id ){
		return framedGraph.getVertex(id, ITest.class);
	}

	/**
	 * Looks up tests by group
	 */
	public static Iterable<ITest> findTestByGroup(String group) {
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		Iterable<ITest> tests = orient_connection.getTransaction().getVertices("groups", group, ITest.class);
		orient_connection.close();
		return tests;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public static Iterable<ITest> findByKey(String generated_key, OrientConnectionFactory orient_connection) {
		return orient_connection.getTransaction().getVertices("key", generated_key, ITest.class);
	}

	/**
	 * {@inheritDoc}
	 * @throws MalformedURLException 
	 */
	public static List<Test> findByUrl(String pageUrl) throws MalformedURLException {
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		Iterator<ITest> test_iter = orient_connection.getTransaction().getVertices("domain", pageUrl, ITest.class).iterator();
		
		ArrayList<Test> list = new ArrayList<Test>();
		TestRepository test_record = new TestRepository();

		while(test_iter.hasNext()){
			ITest itest = test_iter.next();
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
		Iterator<ITest> test_iter = orient_connection.getTransaction().getVertices("landable", isLandable, ITest.class).iterator();
		
		ArrayList<Test> list = new ArrayList<Test>();
		TestRepository test_record = new TestRepository();

		while(test_iter.hasNext()){
			ITest itest = test_iter.next();
			
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
		Iterator<ITest> test_iter = orient_connection.getTransaction().getVertices("name", test_name, ITest.class).iterator();
		
		ArrayList<Test> list = new ArrayList<Test>();
		TestRepository test_record = new TestRepository();

		while(test_iter.hasNext()){
			ITest itest = test_iter.next();
			
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
		Iterator<ITest> test_iter = orient_connection.getTransaction().getVertices("name", source, ITest.class).iterator();
		
		ArrayList<Test> list = new ArrayList<Test>();
		TestRepository test_record = new TestRepository();

		while(test_iter.hasNext()){
			ITest itest = test_iter.next();	
			
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
	 * @return result of running the test. Can be either null or have a {@link Page} set
	 */
	public Page getResult(){
		return this.result;
	}
	
	/**
	 * @param result_page expected {@link Page} state after running through path
	 */
	public void setResult(Page result_page){
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
