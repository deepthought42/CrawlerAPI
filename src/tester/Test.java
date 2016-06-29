package tester;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.frames.FramedTransactionalGraph;

import browsing.Page;
import structs.Path;

/**
 * Defines the path of a test, the result and the expected values to determine if a test was 
 * successful or not
 * 
 * @author Brandon Kindred
 *
 */
public class Test{
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(Test.class);

	public final int key;
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
	public Test(Path path){
		assert path != null;
		this.key = path.hashCode();
		this.path = path;
		this.result = null;
		this.records = new ArrayList<TestRecord>();
	}
	
	/**
	 * Constructs a test object
	 * 
	 * @param path {@link Path} that will be used to determine what the expected path should be
	 * 
	 * @pre path != null
	 */
	public Test(Path path, Page result){
		assert path != null;
		this.key = path.hashCode();
		this.path = path;
		this.result = result;
		this.records = new ArrayList<TestRecord>();
	}
	
	
	/**
	 * Returns test by key
	 * 
	 * @return
	 */
	public int getKey(){
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
	 * 
	 * @param page
	 */
	public ITest convertToRecord(FramedTransactionalGraph<OrientGraph> framedGraph ){
		ITest test = framedGraph.addVertex(UUID.randomUUID(), ITest.class);
		test.setPath(this.getPath().convertToRecord(framedGraph));
		test.setResult(this.getResult().convertToRecord(framedGraph));
		test.setKey(this.generateKey());
		
		return test;
	}

	/**
	 * Generates a key using both path and result in order to guarantee uniqueness of key as well 
	 * as easy identity of {@link Test} when generated in the wild via discovery
	 * @return
	 */
	private String generateKey() {
		
		
		// TODO Auto-generated method stub
		return null;
	}
}
