package com.qanairy.models;

import java.util.Date;

import org.apache.log4j.Logger;

/**
 * A {@link Test} record for reflecting an execution of a test 
 * indicating whether the execution is aligned with the test and therefore passing
 * or mis-aligned with the expectations of the test and therefore failing in 
 * which case a {@link Page} can be saved as a record of what the state of the page
 * was after the test was executed.
 *
 */
public class TestRecord {
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(TestRecord.class);

	private String key;
	private Date ran_at;
	private boolean passes;
	private Page result;
	private Test test;
	
	public TestRecord(Date ran_at, boolean passes){
		this.setRanAt(ran_at);
		this.setPasses(passes);
		this.setPage(null);
		this.setKey(null);
	}
	
	public TestRecord(Date ran_at, boolean passes, Page result, Test test){
		this.setRanAt(ran_at);
		this.setPasses(passes);
		this.setPage(result);
		this.setTest(test);
		this.setKey(null);
	}
	
	public TestRecord(String key, Date ran_at, boolean passes, Page result, Test test){
		this.setRanAt(ran_at);
		this.setPasses(passes);
		this.setPage(result);
		this.setTest(test);
		this.setKey(key);
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
