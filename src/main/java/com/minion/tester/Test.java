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
import com.minion.browsing.PathObject;
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


	private String id;
	
	private String key; 
	private String name;
	private List<TestRecord> records;
	private Path path;
	private Page result;
	private URL domain;
	
	public Test(){}
	
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
	}
	
	
	/**
	 * Returns test by key
	 * 
	 * @return
	 */
	public String getId(){
		return this.id;
	}

	public void setId(String id){
		this.id = id;
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
	
	public void setResult(Page result_page){
		this.result = result_page;
	}
	
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
		Iterable<ITest> tests = findByKey(this.getKey());
		int cnt = 0;
		Iterator<ITest> iter = tests.iterator();
		ITest test = null;
		while(iter.hasNext()){
			iter.next();
			cnt++;
		}
		log.info("# of existing records with key "+this.getKey() + " :: "+cnt);
		
		if(cnt == 0){
			test = connection.getTransaction().addVertex("class:"+ITest.class.toString()+","+UUID.randomUUID(), ITest.class);
		}
		test.setPath(this.getPath().convertToRecord(connection));
		test.setResult(this.getResult().convertToRecord(connection));
		test.setDomain(this.getDomain().toString());
		test.setName(this.getName());
		for(TestRecord record : this.getRecords()){
			test.addRecord(record.convertToRecord(connection));
		}
		test.setKey(this.generateKey());
		
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
			e.printStackTrace();
		}
		
		log.info("setting key");
		test.setKey(itest.getKey());
		
		log.info("setting name");
		test.setName(itest.getName());
		
		log.info("converting path to record");
		test.setPath(Path.convertFromRecord(itest.getPath()));
		//test.setRecords(itest.getRecords());
		//test.setResult(itest.getResult());
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
	public String generateKey() {
		String path_key = "";
		log.error("TEST PATH VALUE :: "+this.getPath().getKey());
		for(PathObject<?> path_obj : this.getPath().getPath()){
			log.error("TEST PATH -  PATH OBJECT VALUE :: "+path_obj.getData().hashCode());
			path_key +=this.getPath().getKey()+":";
		}
		
		path_key += this.getResult().hashCode();
		return path_key;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public IPersistable<ITest> create() {
		System.err.println("SAVING TEST TO ORIENTDB");
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		log.info("Orient database connection factory");
		
		this.convertToRecord(orient_connection);
		
		log.info("Convert to record complete for test");
		orient_connection.save();
		log.info("TEST SACED TO DATABASE");
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IPersistable<ITest> update(ITest existing_obj) {
		Iterator<ITest> test_iter = this.findByKey(this.generateKey()).iterator();
		int cnt=0;
		while(test_iter.hasNext()){
			test_iter.next();
			cnt++;
		}
		log.info("# of existing records with key "+this.getKey() + " :: "+cnt);
		
		OrientConnectionFactory connection = new OrientConnectionFactory();
		if(cnt == 0){
			connection.getTransaction().addVertex("class:"+ITest.class.getCanonicalName()+","+UUID.randomUUID(), ITest.class);
			this.convertToRecord(connection);
		}
		
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
	 * @throws MalformedURLException 
	 */
	public static List<Test> findByUrl(String pageUrl) throws MalformedURLException {
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		Iterator<ITest> test_iter = orient_connection.getTransaction().getVertices("domain", pageUrl, ITest.class).iterator();
		
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
	public static Iterable<ITest> findByName(String test_name) {
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		
		return orient_connection.getTransaction().getVertices("name", test_name, ITest.class);
	}
}
