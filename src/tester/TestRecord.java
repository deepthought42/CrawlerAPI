package tester;

import java.util.Date;
import java.util.Iterator;
import java.util.UUID;

import org.apache.log4j.Logger;

import browsing.Page;
import persistence.IPersistable;
import persistence.ITestRecord;
import persistence.OrientConnectionFactory;

/**
 * A record for when a path was observed
 * 
 * @author Brandon Kindred
 *
 */
public class TestRecord implements IPersistable<ITestRecord> {
	private static final Logger log = Logger.getLogger(Test.class);

	public final Page result;
	public final Date ran_at;
	public final boolean passes;
	public Boolean isCorrect;
	
	public TestRecord(Page page, Date ran_at, boolean passes){
		this.result = page;
		this.ran_at = ran_at;
		this.passes = passes;
		this.isCorrect = null;
	}
	
	/**
	 * @return the path that was observed. This may defer from the actual test path
	 */
	public Page getResult(){
		return result;
	}
	
	/**
	 * @return {@link Date} when test was ran
	 */
	public Date getRanAt(){
		return ran_at;
	}
	
	/**
	 * @return whether or not the test passes compared to expected {@link Test test} path
	 */
	public boolean getPasses(){
		return this.passes;
	}
	
	/**
	 * Sets the correctness of the test. If the resulting path deviates from the original 
	 * path then it is incorrect
	 * 
	 * @param isCorrect
	 */
	public Boolean isCorrect(){
		return this.isCorrect;
	}
	
	public void setIsCorrect(boolean isCorrect){
		this.isCorrect = isCorrect;
	}
	
	/**
	 * 
	 * @param page
	 */
	@Override
	public ITestRecord convertToRecord(OrientConnectionFactory connection){
		ITestRecord testRecord = connection.getTransaction().addVertex(UUID.randomUUID(), ITestRecord.class);

		testRecord.setResult(this.getResult().convertToRecord(connection));
		testRecord.setPasses(this.getPasses());
		testRecord.setRanAt(this.getRanAt());
		testRecord.setCorrect(this.isCorrect());
		testRecord.setKey(this.generateKey());
		
		return testRecord;
	}

	@Override
	public String generateKey() {
		return this.getResult().generateKey() + ":"+this.getPasses()+":"+this.isCorrect();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IPersistable<ITestRecord> create() {
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		
		this.convertToRecord(orient_connection);
		orient_connection.save();
		
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IPersistable<ITestRecord> update(ITestRecord existing_obj) {
		Iterator<ITestRecord> page_iter = this.findByKey(this.generateKey()).iterator();
		int cnt=0;
		while(page_iter.hasNext()){
			page_iter.next();
			cnt++;
		}
		log.info("# of existing records with key "+this.generateKey() + " :: "+cnt);
		
		OrientConnectionFactory connection = new OrientConnectionFactory();
		ITestRecord test_record = null;
		if(cnt == 0){
			test_record = connection.getTransaction().addVertex(UUID.randomUUID(), ITestRecord.class);	
		}
		
		test_record = this.convertToRecord(connection);
		connection.save();
		
		return test_record;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterable<ITestRecord> findByKey(String generated_key) {
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		return orient_connection.getTransaction().getVertices("key", generated_key, ITestRecord.class);
	}
}
