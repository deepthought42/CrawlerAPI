package com.qanairy.models.experience;

/**
 * 
 */
public class ScreenshotThumbnailDetails extends AuditDetail {

	private Integer timing;
	private Long timestamp;
	private String data;
	
	public ScreenshotThumbnailDetails(Integer timing, Long timestamp, String data) {
		
	}

	public Integer getTiming() {
		return timing;
	}

	public void setTiming(Integer timing) {
		this.timing = timing;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

}
