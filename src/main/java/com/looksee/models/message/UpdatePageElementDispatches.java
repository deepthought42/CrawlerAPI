package com.looksee.models.message;

public class UpdatePageElementDispatches {

	private String url;
	private long total_dispatches;
	
	public UpdatePageElementDispatches(String url, long total_dispatches) {
		setUrl(url);
		setTotalDispatches(total_dispatches);
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public long getTotalDispatches() {
		return total_dispatches;
	}

	public void setTotalDispatches(long total_dispatches) {
		this.total_dispatches = total_dispatches;
	}

}
