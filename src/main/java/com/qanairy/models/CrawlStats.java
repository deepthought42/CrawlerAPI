package com.qanairy.models;

import java.time.LocalDateTime;

public class CrawlStats {
	
	private LocalDateTime start_time; //time that the 
	private LocalDateTime end_time;
	private long total_time;
	private long page_count;
	private double average_time_per_page;
	
	public CrawlStats(LocalDateTime start_time, LocalDateTime end_time, long total_seconds, int size, long avg_time_per_page) {
		setStartTime(start_time);
		setEndTime(end_time);
		setTotalTime(total_seconds);
		setPageCount(size);
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
}
