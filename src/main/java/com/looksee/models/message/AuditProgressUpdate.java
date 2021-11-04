package com.looksee.models.message;

import com.looksee.models.audit.Audit;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditLevel;

/**
 * Intended to contain information about progress an audit
 */
public class AuditProgressUpdate {
	private long audit_record_id;
	private Audit audit;
	private AuditCategory category;
	private AuditLevel level;
	private double progress;
	private String message;
	
	public AuditProgressUpdate(
			long audit_record_id,
			double progress,
			String message,
			AuditCategory category, 
			AuditLevel level,
			Audit audit
	) {
		setAuditRecordId(audit_record_id);
		setProgress(progress);
		setMessage(message);
		setCategory(category);
		setLevel(level);
		setAudit(audit);
	}
	
	/* GETTERS / SETTERS */
	public long getAuditRecordId() {
		return audit_record_id;
	}
	public void setAuditRecordId(long audit_record_id) {
		this.audit_record_id = audit_record_id;
	}
	public double getProgress() {
		return progress;
	}
	public void setProgress(double progress) {
		this.progress = progress;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}

	public AuditCategory getCategory() {
		return category;
	}

	public void setCategory(AuditCategory audit_category) {
		this.category = audit_category;
	}

	public AuditLevel getLevel() {
		return level;
	}

	public void setLevel(AuditLevel level) {
		this.level = level;
	}

	public Audit getAudit() {
		return audit;
	}

	public void setAudit(Audit audits) {
		this.audit = audits;
	}
}
