package com.looksee.models.message;

import java.net.URL;

public class PageCandidateFound extends Message {

	private URL url;

	public PageCandidateFound(long account_id, 
							  long audit_record_id,
							  URL url
	) {
		setAccountId(account_id);
		setUrl(url);
		setAuditRecordId(audit_record_id);
	}
	
	public URL getUrl() {
		return url;
	}

	public void setUrl(URL url) {
		this.url = url;
	}	
}
