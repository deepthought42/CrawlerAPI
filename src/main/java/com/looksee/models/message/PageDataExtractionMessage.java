package com.looksee.models.message;

public class PageDataExtractionMessage extends Message{
	private String url;

	public PageDataExtractionMessage(
			long domain_id,
			long account_id,
			long audit_record_id,
			String url
	) {
		super(domain_id, account_id, audit_record_id);
		setUrl(url);
	}
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
}
