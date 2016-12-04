package com.minion.tester;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minion.browsing.Page;
import com.minion.persistence.IPersistable;
import com.minion.persistence.ITestRecord;
import com.minion.persistence.OrientConnectionFactory;

/**
 * A {@link Test} record for reflecting an execution of a test 
 * indicating whether the execution is aligned with the test and therefore passing
 * or mis-aligned with the expectations of the test and therefore failing in 
 * which case a {@link Page} can be saved as a record of what the state of the page
 * was after the test was executed.
 *
 */
public class TestRecord  implements IPersistable<ITestRecord> {
	private static final Logger log = LoggerFactory.getLogger(TestRecord.class);

	private Date ran_at;
	private boolean passes;
	private Page page;
	
	public TestRecord(Date ran_at, boolean passes){
		this.ran_at = ran_at;
		this.passes = passes;
		this.setPage(null);
	}
	
	public TestRecord(Date ran_at, boolean passes, Page page){
		this.ran_at = ran_at;
		this.passes = passes;
		this.setPage(page);
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
		return page;
	}

	public void setPage(Page page) {
		this.page = page;
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
	 * Generates a key for thos object
	 * @return generated key
	 */
	public String generateKey() {
		return this.getRanAt() + ":"+this.getPasses()+":";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ITestRecord create() {
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		
		ITestRecord record = this.convertToRecord(orient_connection);
		orient_connection.save();
		
		return record;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ITestRecord update() {
		OrientConnectionFactory connection = new OrientConnectionFactory();
		ITestRecord test_record = this.convertToRecord(connection);
		connection.save();
		
		return test_record;
	}

	public static List<TestRecord> convertFromRecord(Iterator<ITestRecord> records) {
		// TODO Auto-generated method stub
		return null;
	}	
}
