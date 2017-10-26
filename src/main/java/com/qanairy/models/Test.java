package com.qanairy.models;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
    private static Logger log = LogManager.getLogger(Test.class);
    
	private String key; 
	private String name;
	private List<TestRecord> records;
	private Path path;
	private Page result;
	private Domain domain;
	private Boolean correct;
	private boolean isUseful = false;
	private boolean spansMultipleDomains = false;
	private Map<String, Boolean> browser_statuses = new HashMap<String, Boolean>();
	private List<Group> groups;
	private Date last_run_time;

	private long run_time_length;
	/**
	 * Construct a test with defaults of useful set to fault and 
	 * spansMultipleDomains set to false
	 */
	public Test(){
		this.setRecords(new ArrayList<TestRecord>());
		this.setSpansMultipleDomains(false);
		this.setGroups(new ArrayList<Group>());
		this.setLastRunTimestamp(null);
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
	public Test(Path path, Page result, Domain domain){
		assert path != null;
		
		this.path = path;
		this.result = result;
		this.setRecords(new ArrayList<TestRecord>());
		this.domain = domain;
		this.correct = null;
		this.setSpansMultipleDomains(false);
		this.setGroups(new ArrayList<Group>());
		this.setLastRunTimestamp(null);
		this.setKey(null);
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
	public Test(String key, Path path, Page result, Domain domain){
		assert path != null;
		
		this.path = path;
		this.result = result;
		this.setRecords(new ArrayList<TestRecord>());
		this.domain = domain;
		this.correct = null;
		this.setSpansMultipleDomains(false);
		this.setGroups(new ArrayList<Group>());
		this.setLastRunTimestamp(null);
		this.setKey(key);
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
	public Test(String key, 
				Path path, 
				Page result, 
				Domain domain, 
				boolean correct, 
				boolean isUseful, 
				boolean doesSpanMultipleDomains){
		assert path != null;
		
		this.setPath(path);
		this.setResult(result);
		this.setRecords(new ArrayList<TestRecord>());
		this.setDomain(domain);
		this.setCorrect(correct);
		this.setUseful(isUseful);
		this.setSpansMultipleDomains(doesSpanMultipleDomains);
		this.setGroups(new ArrayList<Group>());
		this.setLastRunTimestamp(null);
		this.setKey(key);
	}
	
	/**
	 * Checks if a {@code TestRecord} snapshot of a {@code Test} is passing or not
	 * 
	 * @param record
	 * @return
	 */
	public Boolean isTestPassing(Page page, boolean last_test_passing_status){
		Boolean passing = null;
		System.out.println("determining passing status..");
		if(!last_test_passing_status && this.getResult().equals(page)){
			System.out.println("Pages are equal and test is NOT already passing");
			passing = false; 
		}
		else if(!last_test_passing_status && !this.getResult().equals(page)){
			System.out.println("Pages are NOT equal and test is NOT already passing");
			passing = null;
		}
		else if(last_test_passing_status && this.getResult().equals(page)){
			System.out.println("pages are equal and test is already marked as passing");
			passing = true;
		}
		else if(last_test_passing_status && !this.getResult().equals(page)){
			System.out.println("pages are NOT equal and test is already passing");
			passing = false;
		}
		
		return passing;
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
	 * Looks up tests unverified tests
	 */
	public static Iterable<ITest> findByDomain(String domain) {
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		//return orient_connection.getTransaction().getBaseGraph().getRawGraph().query(new OSQLSynchQuery<ITest>("SELECT FROM V WHERE color = 'red'"));
		return null;
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
		
		System.out.println("Looking up tests by url" );
		ArrayList<Test> list = new ArrayList<Test>();
		int count = 0;
		TestRepository test_record = new TestRepository();

		while(test_iter.hasNext()){
			System.out.println("Inspecting object " + count);
			ITest itest = test_iter.next();
			Test test = test_record.convertFromRecord(itest);
			list.add(test);
			count++;
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
			
			Test test = test_record.convertFromRecord(itest);
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
			
			Test test = test_record.convertFromRecord(itest);
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
			
			Test test = test_record.convertFromRecord(itest);
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
	
	public void setBrowserStatus(String browser_name, Boolean status){
		this.browser_statuses.put(browser_name, status);
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
}
