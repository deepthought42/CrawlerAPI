package com.qanairy.models;

import java.time.LocalDateTime;

public class CrawlStats extends LookseeObject{
	
	private LocalDateTime start_time; //time that the 
	private LocalDateTime end_time;
	private long total_time;
	private long page_count;
	private double average_time_per_page;
	
	public CrawlStats() {}
	
	public CrawlStats(LocalDateTime start_time, LocalDateTime end_time, long total_seconds, int page_count, double avg_time_per_page) {
		setStartTime(start_time);
		setEndTime(end_time);
		setTotalTime(total_seconds);
		setPageCount(page_count);
		setAverageTimePerPage(avg_time_per_page);
	}

	public LocalDateTime getStartTime() {
		return start_time;
	}
	public void setStartTime(LocalDateTime start_time) {
		this.start_time = start_time;
	}
	public long getTotalTime() {
		return total_time;
	}
	public void setTotalTime(long total_time) {
		this.total_time = total_time;
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

	@Override
	public String generateKey() {
		return org.apache.commons.codec.digest.DigestUtils.sha512Hex( Integer.toString(start_time.hashCode()+end_time.hashCode()));
	}
}
