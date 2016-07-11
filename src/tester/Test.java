package tester;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.frames.FramedTransactionalGraph;

import browsing.Page;
import browsing.PathObject;
import persistence.IPersistable;
import persistence.ITest;
import persistence.OrientConnectionFactory;
import structs.Path;

/**
 * Defines the path of a test, the result and the expected values to determine if a test was 
 * successful or not
 * 
 * @author Brandon Kindred
 *
 */
public class Test implements IPersistable<ITest>{
	private static final Logger log = Logger.getLogger(Test.class);

	public final String key;
	public List<TestRecord> records;
	public final Path path;
	public Page result;
	
	/**
	 * Constructs a test object
	 * 
	 * @param path {@link Path} that will be used to determine what the expected path should be
	 * 
	 * @pre path != null
	 */
	public Test(Path path, Page result){
		assert path != null;
		
		this.path = path;
		this.result = result;
		this.records = new ArrayList<TestRecord>();
		this.key = this.generateKey();
	}
	
	
	/**
	 * Returns test by key
	 * 
	 * @return
	 */
	public String getKey(){
		return this.key;
	}

	public Path getPath(){
		return this.path;
	}
	
	public void addRecord(TestRecord record){
		this.records.add(record);
	}
	
	public List<TestRecord> getRecords(){
		return this.records;
	}
	
	public void setTestRecords(List<TestRecord> records){
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

	/**
	 * Generates a key using both path and result in order to guarantee uniqueness of key as well 
	 * as easy identity of {@link Test} when generated in the wild via discovery
	 * 
	 * @return
	 */
	@Override
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
	@Override
	public IPersistable<ITest> create() {
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		
		this.convertToRecord(orient_connection);
		orient_connection.save();
		
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
			connection.getTransaction().addVertex(UUID.randomUUID(), ITest.class);
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
}
