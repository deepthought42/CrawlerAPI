package com.qanairy.models;

import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qanairy.persistence.TestRecord;

/**
 * A {@link Test} record for reflecting an execution of a test 
 * indicating whether the execution is aligned with the test and therefore passing
 * or mis-aligned with the expectations of the test and therefore failing in 
 * which case a {@link Page} can be saved as a record of what the state of the page
 * was after the test was executed.
 *
 */
public class TestRecordPOJO extends TestRecord {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(TestRecordPOJO.class);

	private String key;
	private Date ran_at;
	private String browser;
	private Boolean passing;
	private Page result;
	private long run_time_length;
	
	//Empty constructor for spring
	public TestRecordPOJO(){}
	
	public TestRecordPOJO(Date ran_at, Boolean passes, String browser_name){
		this.setRanAt(ran_at);
		//this.setPage(null);
		this.setKey(null);
		this.setRunTime(-1L);
		this.setPassing(passes);
		this.setBrowser(browser_name);
	}
	
	public TestRecordPOJO(Date ran_at, Boolean passes, String browser_name, Page result, long run_time){
		this.setRanAt(ran_at);
		//this.setPage(result);
		this.setKey(null);
		this.setRunTime(run_time);
		this.setPassing(passes);
		this.setBrowser(browser_name);
	}
	
	public TestRecordPOJO(String key, Date ran_at, Boolean passes, String browser_name, Page result, long run_time){
		this.setRanAt(ran_at);
		//this.setPage(result);
		this.setKey(key);
		this.setRunTime(run_time);
		this.setPassing(passes);
		this.setBrowser(browser_name);
	}
	
	/**
	 * @return {@link Date} when test was ran
	 */
	@Override
	public Date getRanAt(){
		return ran_at;
	}
	
	/**
	 * @return {@link Date} when test was ran
	 */
	@Override
	public void setRanAt(Date date){
		this.ran_at = date;
	}
	
	/*@Override
	public Page getPage() {
		return this.result;
	}

	@Override
	public void setPage(Page page) {
		this.result = page;
	}
	*/
	/**
	 * @return whether or not the test passes compared to expected {@link Test test} path
	 */
	@Override
	public Boolean getPassing(){
		return this.passing;
	}
	
	/**
	 * @return whether or not the test passes compared to expected {@link Test test} path
	 */
	@Override
	public void setPassing(Boolean passing){
		this.passing = passing;
	}
	
	@Override
	public String getKey() {
		return key;
	}

	@Override
	public void setKey(String key) {
		this.key = key;
	}

	@Override
	public void setRunTime(long pathCrawlRunTime) {
		this.run_time_length = pathCrawlRunTime;
	}
	
	@Override
	public long getRunTime() {
		return this.run_time_length;
	}

	@Override
	public String getBrowser() {
		return browser;
	}

	@Override
	public void setBrowser(String browser) {
		this.browser = browser;
	}
}
