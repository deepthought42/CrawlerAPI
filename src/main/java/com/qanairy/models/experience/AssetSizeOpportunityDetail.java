package com.qanairy.models.experience;

import com.qanairy.models.enums.AuditType;

/**
 * 
 */
public class AssetSizeOpportunityDetail extends AuditDetail {

	private String url;
	private Integer wasted_bytes;
	private Double wasted_percent;
	private Integer total_bytes;
	
	public AssetSizeOpportunityDetail(String url, int wasted_bytes, double wasted_percent, int total_bytes) {
		setUrl(url);
		setWastedBytes(wasted_bytes);
		setWastedPercent(wasted_percent);
		setTotalBytes(total_bytes);
		setType(AuditType.OPPORTUNITY);
	}

	/** GETTERS AND SETTERS  */
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public Integer getWastedBytes() {
		return wasted_bytes;
	}

	public void setWastedBytes(Integer wasted_bytes) {
		this.wasted_bytes = wasted_bytes;
	}

	public Double getWastedPercent() {
		return wasted_percent;
	}

	public void setWastedPercent(Double wasted_percent) {
		this.wasted_percent = wasted_percent;
	}

	public Integer getTotalBytes() {
		return total_bytes;
	}

	public void setTotalBytes(Integer total_bytes) {
		this.total_bytes = total_bytes;
	}

}
