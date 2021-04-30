package com.looksee.models.audit;

import java.time.LocalDateTime;
import java.util.UUID;

import com.looksee.models.LookseeObject;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.ExecutionStatus;

/**
 * Record detailing an set of {@link Audit audits}.
 */
public class AuditRecord extends LookseeObject {
	
	private String status;
	private String level;
	private LocalDateTime start_time;
	private LocalDateTime end_time;
	
	public AuditRecord() {
		setStartTime(LocalDateTime.now());
	}
	
	/**
	 * Constructor
	 * @param level TODO
	 * 
	 * @pre audit_stats != null;
	 */
	public AuditRecord(ExecutionStatus status, AuditLevel level) {
		setKey(generateKey());
		setStatus(status);
		setLevel(level);
		setStartTime(LocalDateTime.now());
	}

	public String generateKey() {
		return "auditrecord:"+UUID.randomUUID().toString()+org.apache.commons.codec.digest.DigestUtils.sha256Hex(System.currentTimeMillis() + "");
	}

	public ExecutionStatus getStatus() {
		return ExecutionStatus.create(status);
	}

	public void setStatus(ExecutionStatus status) {
		this.status = status.getShortName();
	}

	public AuditLevel getLevel() {
		return AuditLevel.create(level);
	}

	public void setLevel(AuditLevel level) {
		this.level = level.toString();
	}

	public LocalDateTime getStartTime() {
		return start_time;
	}

	public void setStartTime(LocalDateTime start_time) {
		this.start_time = start_time;
	}

	public LocalDateTime getEndTime() {
		return end_time;
	}

	public void setEndTime(LocalDateTime end_time) {
		this.end_time = end_time;
	}
}
