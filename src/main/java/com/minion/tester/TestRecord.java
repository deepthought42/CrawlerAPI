package com.minion.tester;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minion.persistence.IPersistable;
import com.minion.persistence.ITestRecord;
import com.minion.persistence.OrientConnectionFactory;

/**
 * A record for when a path was observed
 * 
 * @author Brandon Kindred
 *
 */
public class TestRecord  implements IPersistable<ITestRecord> {
	private static final Logger log = LoggerFactory.getLogger(TestRecord.class);

	private Date ran_at;
	private boolean passes;
	
	public TestRecord(Test test, Date ran_at, boolean passes){
		this.ran_at = ran_at;
		this.passes = passes;
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
	@Override
	public ITestRecord convertToRecord(OrientConnectionFactory connection){
		ITestRecord testRecord = connection.getTransaction().addVertex(UUID.randomUUID(), ITestRecord.class);

		testRecord.setPasses(this.getPasses());
		testRecord.setRanAt(this.getRanAt());
		testRecord.setKey(this.generateKey());
		
		return testRecord;
	}

	/**
	 * 
	 * @return
	 */
	public String generateKey() {
		return this.getRanAt() + ":"+this.getPasses()+":";
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
	public IPersistable<ITestRecord> update() {
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
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterable<ITestRecord> findByKey(String generated_key, OrientConnectionFactory orient_connection) {
		return orient_connection.getTransaction().getVertices("key", generated_key, ITestRecord.class);
	}

	public static List<TestRecord> convertFromRecord(Iterator<ITestRecord> records) {
		// TODO Auto-generated method stub
		return null;
	}	
}
