package com.qanairy.models;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.IPersistable;
import com.qanairy.persistence.ITestRecord;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * A {@link Test} record for reflecting an execution of a test 
 * indicating whether the execution is aligned with the test and therefore passing
 * or mis-aligned with the expectations of the test and therefore failing in 
 * which case a {@link Page} can be saved as a record of what the state of the page
 * was after the test was executed.
 *
 */
public class TestRecord  implements IPersistable<ITestRecord> {
	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(TestRecord.class);

	private String key;
	private Date ran_at;
	private boolean passes;
	private Page result;
	private Test test;
	
	public TestRecord(Date ran_at, boolean passes){
		this.setRanAt(ran_at);
		this.setPasses(passes);
		this.setPage(null);
		this.setKey(generateKey());
	}
	
	public TestRecord(Date ran_at, boolean passes, Page result, Test test){
		this.setRanAt(ran_at);
		this.setPasses(passes);
		this.setPage(null);
		this.setTest(test);
		this.setKey(generateKey());
	}
	
	/**
	 * @return {@link Date} when test was ran
	 */
	public Date getRanAt(){
		return ran_at;
	}
	
	/**
	 * @return {@link Date} when test was ran
	 */
	public void setRanAt(Date date){
		this.ran_at = date;
	}
	
	public Page getPage() {
		return this.result;
	}

	public void setPage(Page page) {
		this.result = page;
	}

	/**
	 * @return whether or not the test passes compared to expected {@link Test test} path
	 */
	public boolean getPasses(){
		return this.passes;
	}
	
	/**
	 * @return whether or not the test passes compared to expected {@link Test test} path
	 */
	public void setPasses(boolean passing){
		this.passes = passing;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public ITestRecord convertToRecord(OrientConnectionFactory connection){
		ITestRecord testRecord = connection.getTransaction().addVertex(UUID.randomUUID(), ITestRecord.class);

		testRecord.setPasses(this.getPasses());
		testRecord.setRanAt(this.getRanAt());
		testRecord.setKey(this.getKey());
		
		return testRecord;
	}

	/**
	 * Generates a key for thos object
	 * @return generated key
	 */
	public String generateKey() {
		return this.getPage().getKey()+":"+this.getRanAt();
	}

	/**
	 * {@inheritDoc}
	 */
	public ITestRecord create(OrientConnectionFactory connection) {
		ITestRecord record = find(connection);
		if(record == null){
			record = this.convertToRecord(connection);
			connection.save();
		}
		
		return record;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public ITestRecord update(OrientConnectionFactory connection) {
		ITestRecord record = this.find(connection);
		if(record != null){
			record.setPasses(this.getPasses());
			record.setRanAt(this.getRanAt());
			
			connection.save();
		}
		
		return record;
	}

	/**
	 * 
	 */
	@Override
	public ITestRecord find(OrientConnectionFactory conn){
		@SuppressWarnings("unchecked")
		Iterable<ITestRecord> domains = (Iterable<ITestRecord>) DataAccessObject.findByKey(this.getKey(), conn, ITestRecord.class);
		Iterator<ITestRecord> iter = domains.iterator();
		  
		if(iter.hasNext()){
			return iter.next();
		}
		
		return null;
	}
	
	public static List<TestRecord> convertFromRecord(Iterator<ITestRecord> records) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Test getTest() {
		return test;
	}

	public void setTest(Test test) {
		this.test = test;
	}	
}
