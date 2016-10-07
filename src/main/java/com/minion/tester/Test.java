package com.minion.tester;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.orient.commons.repository.annotation.Vertex;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.minion.browsing.Page;
import com.minion.browsing.PathObject;
import com.minion.structs.Path;

/**
 * Defines the path of a test, the result and the expected values to determine if a test was 
 * successful or not
 * 
 * @author Brandon Kindred
 *
 */
@Vertex
public class Test {
	private static final Logger log = Logger.getLogger(Test.class);

	@Id
	private String id;
	
	@Version
    @JsonIgnore
    private Long version;
	
	private String key; 
	private String name;
	private List<TestRecord> records;
	private Path path;
	private Page result;
	private URL domain;
	
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
	/*@Override
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
			test = connection.getTransaction().addVertex(UUID.randomUUID(), ITest.class);
		}
		test.setPath(this.getPath().convertToRecord(connection));
		test.setResult(this.getResult().convertToRecord(connection));
		for(TestRecord record : this.getRecords()){
			test.addRecord(record.convertToRecord(connection));
		}
		test.setKey(this.generateKey());
		
		return test;
	}
	
	public ITest findById(FramedTransactionalGraph<OrientGraph> framedGraph, String id ){
		return framedGraph.getVertex(id, ITest.class);
	}
	*/
	/**
	 * Generates a key using both path and result in order to guarantee uniqueness of key as well 
	 * as easy identity of {@link Test} when generated in the wild via discovery
	 * 
	 * @return
	 */
	public String generateKey() {
		String path_key = "";
		log.error("TEST PATH VALUE :: "+this.getPath().getKey());
		for(PathObject path_obj : this.getPath().getPath()){
			log.error("TEST PATH -  PATH OBJECT VALUE :: "+path_obj.data().hashCode());
			path_key +=this.getPath().getKey()+":";
		}
		
		path_key += this.getResult().hashCode();
		return path_key;
	}
	
	/**
	 * {@inheritDoc}
	 */
	/*@Override
	public IPersistable<ITest> create() {
		System.err.println("SAVING TEST TO ORIENTDB");
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		log.info("Orient database connection factory");
		
		this.convertToRecord(orient_connection);
		
		log.info("Convert to record complete for test");
		orient_connection.save();
		log.info("TEST SACED TO DATABASE");
		return this;
	}*/

	/**
	 * {@inheritDoc}
	 */
	/*@Override
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
			connection.getTransaction().addVertex(UUID.randomUUID(), ITest.class);
			this.convertToRecord(connection);
		}
		
		connection.save();
		
		return this;
	}*/

	/**
	 * {@inheritDoc}
	 */
	/*@Override
	public Iterable<ITest> findByKey(String generated_key) {
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		return orient_connection.getTransaction().getVertices("key", generated_key, ITest.class);
	}
	*/
}
