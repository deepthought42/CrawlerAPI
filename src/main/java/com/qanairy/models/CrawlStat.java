package com.qanairy.models;

import java.time.LocalDateTime;

public class CrawlStat extends LookseeObject{
	
	private LocalDateTime start_time; //time that the 
	private LocalDateTime end_time;
	private String host;
	private long page_count;
	private double average_time_per_page;
	
	public CrawlStat(String url) {
		setStartTime(start_time);
		setHost(url);
		setPageCount(0);
		setKey(generateKey());
	}
	
	public CrawlStat(String host, LocalDateTime start_time, LocalDateTime end_time, int page_count, double avg_time_per_page) {
		setStartTime(start_time);
		setEndTime(end_time);
		setPageCount(page_count);
		setAverageTimePerPage(avg_time_per_page);
		setHost(host);
		setKey(generateKey());
	}

	public LocalDateTime getStartTime() {
		return start_time;
	}
	
	public void setStartTime(LocalDateTime start_time) {
		this.start_time = start_time;
	}
	
	public LocalDateTime getEndTime() {
		return end_time;
	}
	
	public void setEndTime(LocalDateTime end_time) {
		this.end_time = end_time;
	}
	
	public long getPageCount() {
		return page_count;
	}
	
	public void setPageCount(long page_count) {
		this.page_count = page_count;
	}
	
	public double getAverageTimePerPage() {
		return average_time_per_page;
	}
	
	public void setAverageTimePerPage(double average_time_per_page) {
		this.average_time_per_page = average_time_per_page;
	}

	public String getUrl() {
		return host;
	}

	public void setHost(String url) {
		this.host = url;
	}

	@Override
	public String generateKey() {
		return "crawlstat::"+org.apache.commons.codec.digest.DigestUtils.sha512Hex( start_time.hashCode()+this.host);
	}
}
