package com.looksee.dto;

/**
 * Data transfer object for {@link Domain} object that is designed to comply with
 * the data format for browser extensions
 */
public class DomainDto {
	private long id;
	private String url;
	private int page_count;
	private double content_score;
	private double info_architecture_score;
	private double accessibility_score;
	private double aesthetics_score;

	public DomainDto(){}

	public DomainDto(
			long id,
			String url,
			int page_count,
			double content_score,
			double info_architecture_score,
			double accessibility_score,
			double aesthetics_score){
		setId(id);
		setUrl(url);
		setPageCount(page_count);
		setContentScore(content_score);
		setInfoArchitectureScore(info_architecture_score);
		setAccessibilityScore(accessibility_score);
		setAestheticsScore(aesthetics_score);
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public double getContentScore() {
		return content_score;
	}

	public void setContentScore(double content_score) {
		this.content_score = content_score;
	}

	public double getInfoArchitectureScore() {
		return info_architecture_score;
	}

	public void setInfoArchitectureScore(double info_architecture_score) {
		this.info_architecture_score = info_architecture_score;
	}

	public double getAccessibilityScore() {
		return accessibility_score;
	}

	public void setAccessibilityScore(double accessibility_score) {
		this.accessibility_score = accessibility_score;
	}

	public double getAestheticsScore() {
		return aesthetics_score;
	}

	public void setAestheticsScore(double aesthetics_score) {
		this.aesthetics_score = aesthetics_score;
	}

	public int getPageCount() {
		return page_count;
	}

	public void setPageCount(int page_count) {
		this.page_count = page_count;
	}

	
}
