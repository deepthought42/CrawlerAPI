package com.qanairy.models;

import java.util.Date;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qanairy.models.enums.TestStatus;

/**
 * A {@link Test} record for reflecting an execution of a test 
 * indicating whether the execution is aligned with the test and therefore status
 * or mis-aligned with the expectations of the test and therefore failing in 
 * which case a {@link PageState} can be saved as a record of what the state of the page
 * was after the test was executed.
 *
 */
@NodeEntity
public class TestRecord implements Persistable {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(TestRecord.class);

	@GeneratedValue
    @Id
	private Long id;
	
	private String key;
	private Date ran_at;
	private String browser;
	private TestStatus status;
	private long run_time_length;

	@Relationship(type = "HAS_RESULT", direction = Relationship.OUTGOING)
	private PageState result;
	
	@Relationship(type = "HAS_TEST_RECORD", direction = Relationship.INCOMING)
	private Test test;
	
	//Empty constructor for spring
	public TestRecord(){}
	
	public TestRecord(Date ran_at, TestStatus status, String browser_name, PageState result, long run_time){
		setRanAt(ran_at);
		setResult(result);
		setRunTime(run_time);
		setStatus(status);
		setBrowser(browser_name);
		setKey(generateKey());
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
	
	public PageState getResult() {
		return this.result;
	}

	public void setResult(PageState page) {
		this.result = page;
	}
	
	/**
	 * @return whether or not the test passes compared to expected {@link Test test} path
	 */
	public TestStatus getStatus(){
		return this.status;
	}
	
	/**
	 * @return whether or not the test passes compared to expected {@link Test test} path
	 */
	public void setStatus(TestStatus status){
		this.status = status;
	}
	
	public String getKey() {
		return this.key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public void setRunTime(long pathCrawlRunTime) {
		this.run_time_length = pathCrawlRunTime;
	}
	
	public long getRunTime() {
		return this.run_time_length;
	}

	public String getBrowser() {
		return browser;
	}

	public void setBrowser(String browser) {
		this.browser = browser;
	}
		
	/**
	 * Generates a key for this object
	 * @return generated key
	 */
	@Override
	public String generateKey() {
		return "testrecord::"+getRanAt().hashCode()+getResult().getKey();
	}

	public Test getTest() {
		return this.test;
	}
}
