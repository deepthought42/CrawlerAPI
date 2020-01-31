package com.qanairy.models.experience;

import java.util.Date;

import com.qanairy.models.enums.BugType;

/**
 * 
 */
public class BugMessage extends AuditDetail {

	private String message;
	private BugType bug_type;
	private Date date_identified;
	
	public BugMessage() {}
	
	public BugMessage(
		String message,
		BugType type,
		Date date
	) {
		setMessage(message);
		setBugType(type);
		setDateIdentified(date);
	}
	
	public boolean equals(Object o) {
		if (this == o) return true;
        if (!(o instanceof BugMessage)) return false;
        
        BugMessage that = (BugMessage)o;
		return this.getMessage().equals(that.getMessage());
	}
	
	/*******************************
	 * GETTERS/SETTERS
	 *******************************/
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public BugType getBugType() {
		return bug_type;
	}
	public void setBugType(BugType bug_type) {
		this.bug_type = bug_type;
	}
	public Date getDateIdentified() {
		return date_identified;
	}
	public void setDateIdentified(Date date_identified) {
		this.date_identified = date_identified;
	}
}
