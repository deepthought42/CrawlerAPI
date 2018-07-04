package com.qanairy.models;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

/**
 * Record detailing a "Discovery" ran by an account.
 */
@NodeEntity
public class DiscoveryRecord implements Persistable {
	
	@GeneratedValue
    @Id
	private Long id;
	
	private String key;
	private Date started_at;
	private String browser_name;
	private String domain_url;
	private Date last_path_ran_at;
	private int total_path_count;
	private int examined_path_count;
	private int test_cnt;
	private List<String> expanded_page_state = new ArrayList<>();

	public DiscoveryRecord(){}
	
	public DiscoveryRecord(Date started_timestamp, String browser_name, String domain_url){
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
	
	public DiscoveryRecord(Date started_timestamp, String browser_name, String domain_url, Date last_path_ran, int test_cnt, int total_cnt, int examined_cnt){
		assert started_timestamp != null;
		assert browser_name != null;
		assert domain_url != null;
		assert test_cnt > -1;
		assert total_cnt > 0;
		
		setStartTime(started_timestamp);
		setBrowserName(browser_name);
		setDomainUrl(domain_url);
		setLastPathRanAt(new Date());
		setTotalPathCount(total_cnt);
		setExaminedPathCount(examined_cnt);
		setTestCount(test_cnt);
		setKey(generateKey());
	}

	public String getKey() {
		return this.key;
	}
	
	public void setKey(String key) {
		this.key = key;
	}
	
	public Date getStartTime() {
		return started_at;
	}
	
	public void setStartTime(Date started_at) {
		this.started_at = started_at;
	}
	
	public String getBrowserName() {
		return browser_name;
	}
	
	public void setBrowserName(String browser_name) {
		this.browser_name = browser_name;
	}

	public String getDomainUrl() {
		return domain_url;
	}

	public void setDomainUrl(String domain_url) {
		this.domain_url = domain_url;
	}

	public Date getLastPathRanAt() {
		return last_path_ran_at;
	}

	public void setLastPathRanAt(Date last_path_ran_at) {
		this.last_path_ran_at = last_path_ran_at;
	}

	public int getTotalPathCount() {
		return total_path_count;
	}

	public void setTotalPathCount(int total_path_count) {
		this.total_path_count = total_path_count;
	}

	public int getExaminedPathCount() {
		return examined_path_count;
	}

	public void setExaminedPathCount(int examined_path_count) {
		this.examined_path_count = examined_path_count;
	}

	public int getTestCount() {
		return this.test_cnt;
	}
	
	public void setTestCount(int cnt){
		this.test_cnt = cnt;
	}
	
	public String generateKey() {
		return getDomainUrl()+":"+getStartTime();
	}

	public List<String> getExpandedPageState() {
		return expanded_page_state;
	}

	public void setExpandedPageState(List<String> expanded_page_state) {
		this.expanded_page_state = expanded_page_state;
	}
}
