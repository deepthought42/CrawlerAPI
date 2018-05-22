package com.qanairy.models;

import java.util.Date;

import com.qanairy.persistence.DiscoveryRecord;

/**
 * Record detailing a "Discovery" ran by an account.
 */
public class DiscoveryRecordPOJO extends DiscoveryRecord {
	private String key;
	private Date started_at;
	private String browser_name;
	private String domain_url;
	private Date last_path_ran_at;
	private int total_path_count;
	private int examined_path_count;
	private int test_cnt;

	public DiscoveryRecordPOJO(Date started_timestamp, String browser_name, String domain_url){
		assert started_timestamp != null;
		assert browser_name != null;
		assert domain_url != null;
		
		setStartTime(started_timestamp);
		setBrowserName(browser_name);
		setDomainUrl(domain_url);
		setLastPathRanAt(new Date());
		setTotalPathCount(0);
		setExaminedPathCount(0);
		setTestCount(0);
		setKey(generateKey());
	}
	
	public DiscoveryRecordPOJO(Date started_timestamp, String browser_name, String domain_url, Date last_path_ran, int test_cnt, int total_cnt, int examined_cnt){
		assert started_timestamp != null;
		assert browser_name != null;
		assert domain_url != null;
		assert test_cnt > -1;
		assert total_path_count > 0;
		
		setStartTime(started_timestamp);
		setBrowserName(browser_name);
		setDomainUrl(domain_url);
		setLastPathRanAt(new Date());
		setTotalPathCount(total_cnt);
		setExaminedPathCount(examined_cnt);
		setTestCount(test_cnt);
		setKey(generateKey());
	}

	@Override
	public String getKey() {
		return this.key;
	}
	
	@Override
	public void setKey(String key) {
		this.key = generateKey();
	}
	
	@Override
	public Date getStartTime() {
		return started_at;
	}
	
	@Override
	public void setStartTime(Date started_at) {
		this.started_at = started_at;
	}
	
	@Override
	public String getBrowserName() {
		return browser_name;
	}
	
	@Override
	public void setBrowserName(String browser_name) {
		this.browser_name = browser_name;
	}

	@Override
	public String getDomainUrl() {
		return domain_url;
	}

	@Override
	public void setDomainUrl(String domain_url) {
		this.domain_url = domain_url;
	}

	@Override
	public Date getLastPathRanAt() {
		return last_path_ran_at;
	}

	@Override
	public void setLastPathRanAt(Date last_path_ran_at) {
		this.last_path_ran_at = last_path_ran_at;
	}

	@Override
	public int getTotalPathCount() {
		return total_path_count;
	}

	@Override
	public void setTotalPathCount(int total_path_count) {
		this.total_path_count = total_path_count;
	}

	@Override
	public int getExaminedPathCount() {
		return examined_path_count;
	}

	@Override
	public void setExaminedPathCount(int examined_path_count) {
		this.examined_path_count = examined_path_count;
	}

	@Override
	public int getTestCount() {
		return this.test_cnt;
	}
	
	@Override
	public void setTestCount(int cnt){
		this.test_cnt = cnt;
	}
	
	public String generateKey() {
		return getDomainUrl()+":"+getStartTime().toString();
	}
}
