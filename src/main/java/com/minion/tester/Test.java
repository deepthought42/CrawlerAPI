package com.minion.tester;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minion.browsing.Page;
import com.minion.persistence.IPersistable;
import com.minion.persistence.ITest;
import com.minion.persistence.OrientConnectionFactory;
import com.minion.structs.Path;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.frames.FramedTransactionalGraph;

/**
 * Defines the path of a test, the result and the expected values to determine if a test was 
 * successful or not
 * 
 * @author Brandon Kindred
 *
 */
public class Test implements IPersistable<ITest>{
    private static final Logger log = LoggerFactory.getLogger(Test.class);

	private String key; 
	private String name;
	private List<TestRecord> records;
	private Path path;
	private Page result;
	private URL domain;
	private Boolean correct;
	private boolean isUseful;
	private boolean spansMultipleDomains = false;
	
	/**
	 * 
	 */
	public Test(){
		this.records = new ArrayList<TestRecord>();
		this.isUseful = false;
		this.spansMultipleDomains = false;
	}
	
	/**
	 * Constructs a test object
	 * 
	 * @param path {@link Path} that will be used to determine what the expected path should be
	 * 
	 * @pre path != null
	 */
	public Test(Path path, Page result, URL domain){
		assert path != null;
		
		this.path = path;
		this.result = result;
		this.records = new ArrayList<TestRecord>();
		this.key = this.generateKey();
		this.domain = domain;
		this.correct = null;
		this.isUseful = false;
		this.spansMultipleDomains = false;
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
	
	public URL getDomain(){
		return this.domain;
	}
	
	public void setDomain(URL domain){
		this.domain = domain;
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
	 * 
	 * @param result_page
	 */
	public void setResult(Page result_page){
		this.result = result_page;
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
	 * {@inheritDoc}
	 */
	@Override
	public ITest convertToRecord(OrientConnectionFactory connection){
		this.setKey(this.generateKey());
		Iterable<ITest> tests = findByKey(this.getKey(), connection);
		
		int cnt = 0;
		Iterator<ITest> iter = tests.iterator();
		ITest test = null;

		log.info("# of existing test records with key "+this.getKey() + " :: "+cnt);
		
		if(!iter.hasNext()){
			test = connection.getTransaction().addVertex("class:"+ITest.class.getCanonicalName()+","+UUID.randomUUID(), ITest.class);
		}
		else{
			test = iter.next();
		}
		
		test.setPath(this.getPath().convertToRecord(connection));
		test.setResult(this.getResult().convertToRecord(connection));
		test.setDomain(this.getDomain().toString());
		test.setName(this.getName());
		test.setCorrect(this.isCorrect());
		
		for(TestRecord record : this.getRecords()){
			test.addRecord(record.convertToRecord(connection));
		}
		test.setKey(this.getKey());
		
		return test;
	}
	
	/**
	 * 
	 * @param itest
	 * @return
	 */
	public static Test convertFromRecord(ITest itest){
		Test test = new Test();
		
		log.info("converting record with domain : " + itest.getDomain());
		try {
			test.setDomain(new URL(itest.getDomain()));
			log.info("Set domain to new test object");
		} catch (MalformedURLException e) {
			test.setDomain(null);
			//e.printStackTrace();
		}
		
		log.info("setting key to "+itest.getKey());
		test.setKey(itest.getKey());
		
		log.info("setting name");
		test.setName(itest.getName());
		test.setCorrect(itest.getCorrect());
		log.info("Converting path from record");
		test.setPath(Path.convertFromRecord(itest.getPath()));
		//test.setRecords(TestRecord.convertFromRecord(itest.getRecords()));
		test.setResult(Page.convertFromRecord(itest.getResult()));
		return test;
		
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
	 * Generates a key using both path and result in order to guarantee uniqueness of key as well 
	 * as easy identity of {@link Test} when generated in the wild via discovery
	 * 
	 * @return
	 */
	@Override
	public String generateKey() {
		String path_key = "";
		
		path_key += this.getPath().generateKey();
		
		path_key += this.getResult().generateKey();
		this.key = path_key;
		return path_key;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public IPersistable<ITest> create() {
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		log.info("Orient database connection factory");
		
		this.convertToRecord(orient_connection);
		
		log.info("Convert to record complete for test");
		orient_connection.save();
		log.info("TEST SAVED TO DATABASE");
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IPersistable<ITest> update() {
		/*
		Iterator<ITest> test_iter = this.findByKey(this.generateKey()).iterator();
		int cnt=0;
		while(test_iter.hasNext()){
			test_iter.next();
			cnt++;
		}
		
		log.info("# of existing records with key "+this.getKey() + " :: "+cnt);
		*/
		OrientConnectionFactory connection = new OrientConnectionFactory();
		//if(cnt == 0){
			//connection.getTransaction().addVertex("class:"+ITest.class.getCanonicalName()+","+UUID.randomUUID(), ITest.class);
			this.convertToRecord(connection);
		//}
		
		connection.save();
		
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterable<ITest> findByKey(String generated_key) {
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		return orient_connection.getTransaction().getVertices("key", generated_key, ITest.class);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public static Iterable<ITest> findTestByKey(String generated_key) {
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		return orient_connection.getTransaction().getVertices("key", generated_key, ITest.class);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public static Iterable<ITest> findTestByKey(String generated_key, OrientConnectionFactory orient_connection) {
		return orient_connection.getTransaction().getVertices("key", generated_key, ITest.class);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterable<ITest> findByKey(String generated_key, OrientConnectionFactory orient_connection) {
		return orient_connection.getTransaction().getVertices("key", generated_key, ITest.class);
	}
	
	/**
	 * {@inheritDoc}
	 * @throws MalformedURLException 
	 */
	public static List<Test> findByUrl(String pageUrl) throws MalformedURLException {
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		Iterator<ITest> test_iter = orient_connection.getTransaction().getVertices("domain", pageUrl, ITest.class).iterator();
		
		log.info("Looking up tests by url" );
		ArrayList<Test> list = new ArrayList<Test>();
		int count = 0;
		while(test_iter.hasNext()){
			log.info("Inspecting object " + count);
			ITest itest = test_iter.next();
			Test test = Test.convertFromRecord(itest);
			list.add(test);
			count++;
		}
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
		while(test_iter.hasNext()){
			ITest itest = test_iter.next();
			
			
			Test test = Test.convertFromRecord(itest);
			list.add(test);
		}
		return list;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public static List<Test> findByName(String test_name) {
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		Iterator<ITest> test_iter = orient_connection.getTransaction().getVertices("name", test_name, ITest.class).iterator();
		
		ArrayList<Test> list = new ArrayList<Test>();
		while(test_iter.hasNext()){
			ITest itest = test_iter.next();
			
			
			Test test = Test.convertFromRecord(itest);
			list.add(test);
		}
		
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
		while(test_iter.hasNext()){
			ITest itest = test_iter.next();
			
			
			Test test = Test.convertFromRecord(itest);
			list.add(test);
		}
		
		return list;
	}
}
