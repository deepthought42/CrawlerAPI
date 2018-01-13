package com.qanairy.models;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private static Logger log = LoggerFactory.getLogger(TestRecord.class);

	private String key;
	private Date ran_at;
	private Boolean passes;
	private Page result;
	private long run_time_length;
	private Map<String, Boolean> browser_statuses = new HashMap<String, Boolean>();
	
	public TestRecord(Date ran_at, Map<String,Boolean> passes){
		this.setRanAt(ran_at);
		this.setPage(null);
		this.setKey(null);
		this.setRunTime(-1L);
		this.setBrowserStatuses(passes);
	}
	
	public TestRecord(Date ran_at, Map<String,Boolean> passes, Page result){
		this.setRanAt(ran_at);
		this.setPage(result);
		this.setKey(null);
		this.setRunTime(-1L);
		this.setBrowserStatuses(passes);
	}
	
	public TestRecord(String key, Date ran_at, Map<String, Boolean> passes, Page result){
		this.setRanAt(ran_at);
		this.setPage(result);
		this.setKey(key);
		this.setRunTime(-1L);
		this.setBrowserStatuses(passes);
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
	public Boolean getPasses(){
		return this.passes;
	}
	
	/**
	 * @return whether or not the test passes compared to expected {@link Test test} path
	 */
	public void setPasses(Boolean passing){
		this.passes = passing;
	}
	
	public String getKey() {
		return key;
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

	public Map<String, Boolean> getBrowserStatuses() {
		return browser_statuses;
	}

	public void setBrowserStatuses(Map<String, Boolean> browser_statuses) {
		this.browser_statuses = browser_statuses;
	}
	
	public void setBrowserStatus(String browser, Boolean passing) {
		this.browser_statuses.put(browser, passing);
	}
}
