package com.looksee.models.message;

public class PageDataExtractionMessage extends Message{
	private String url;
	private int dispatch_count;

	public PageDataExtractionMessage(
			long domain_id,
			long account_id,
			long audit_record_id,
			String url, 
			int dispatch_count
	) {
		super(domain_id, account_id, audit_record_id);
		setUrl(url);
		setDispatchCount(dispatch_count);
	}
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	public int getDispatchCount() {
		return dispatch_count;
	}
	public void setDispatchCount(int dispatch_count) {
		this.dispatch_count = dispatch_count;
	}
}
