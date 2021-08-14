package com.looksee.models.message;

import java.util.List;

/**
 * Message for different audit actions to perform and which audit types to perform them for.
 * 
 */
public class ElementProgressMessage extends Message{
	private long page_state_id;
	private List<String> xpaths_explored;
	
	public ElementProgressMessage(
			long audit_record_id, 
			long page_state_id, 
			List<String> xpaths){
		setAuditRecordId(audit_record_id);
		setXpathsExplored(xpaths);
		setPageStateId(page_state_id);
	}

	public long getPageStateId() {
		return page_state_id;
	}

	public void setPageStateId(long page_state_id) {
		this.page_state_id = page_state_id;
	}

	public List<String> getXpathsExplored() {
		return xpaths_explored;
	}

	public void setXpathsExplored(List<String> xpaths_explored) {
		this.xpaths_explored = xpaths_explored;
	}	
}
