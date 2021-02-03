package com.qanairy.models;

import java.time.LocalDateTime;

import com.qanairy.models.enums.AuditSubcategory;

public class AuditSubcategoryStat extends LookseeObject{
	
	private LocalDateTime start_time; //time that audit subcategory is started
	private LocalDateTime end_time; //time that audit of subcategory is completed
	private AuditSubcategory subcategory;
	private long pages_completed;
	
	public AuditSubcategoryStat() {}
	
	public AuditSubcategoryStat(AuditSubcategory subcategory, 
								LocalDateTime start_time, 
								LocalDateTime end_time, 
								int page_count) {
		setStartTime(start_time);
		setEndTime(end_time);
		setPagesCompleted(page_count);
		setSubcategory(subcategory);
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
	
	public long getPagesCompleted() {
		return pages_completed;
	}
	
	public void setPagesCompleted(long pages_completed) {
		this.pages_completed = pages_completed;
	}
	
	public void setSubcategory(AuditSubcategory subcategory) {
		this.subcategory = subcategory;
	}
	
	public AuditSubcategory getSubcategory() {
		return subcategory;
	}

	@Override
	public String generateKey() {
		return "auditsubcategorystat::"+org.apache.commons.codec.digest.DigestUtils.sha512Hex( Integer.toString(start_time.hashCode()+end_time.hashCode()));
	}
}
