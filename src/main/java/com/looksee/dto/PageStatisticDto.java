package com.looksee.dto;

/**
 * A simplified data set for page consisting of full page and viewport screenshots, url and the height and width
 *  of the full page screenshot
 *
 */
public class PageStatisticDto {
	private long id;
	private String url;
	private String screenshot_url;
	private double content_score;
	private double info_arch_score;
	private double accessibility_score;
	private double aesthetic_score;
	
	public PageStatisticDto(
			long id, 
			String url, 
			String screenshot_url, 
			double content_score, 
			double info_arch_score, 
			double accessibility_score, 
			double aesthetic_score
	) {
		setId(id);
		setUrl(url);
		setScreenshotUrl(screenshot_url);
		setContentScore(content_score);
		setInfoArchScore(info_arch_score);
		setAccessibilityScore(accessibility_score);
		setAestheticScore(aesthetic_score);
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getScreenshotUrl() {
		return screenshot_url;
	}

	public void setScreenshotUrl(String screenshot_url) {
		this.screenshot_url = screenshot_url;
	}

	public double getContentScore() {
		return content_score;
	}

	public void setContentScore(double content_score) {
		this.content_score = content_score;
	}

	public double getInfoArchScore() {
		return info_arch_score;
	}

	public void setInfoArchScore(double info_arch_score) {
		this.info_arch_score = info_arch_score;
	}

	public double getAccessibilityScore() {
		return accessibility_score;
	}

	public void setAccessibilityScore(double accessibility_score) {
		this.accessibility_score = accessibility_score;
	}

	public double getAestheticScore() {
		return aesthetic_score;
	}

	public void setAestheticScore(double aesthetic_score) {
		this.aesthetic_score = aesthetic_score;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

}
