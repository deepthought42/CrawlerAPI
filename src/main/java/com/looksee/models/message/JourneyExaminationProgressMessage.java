package com.looksee.models.message;

public class JourneyExaminationProgressMessage {

	private long accountId;
	private long auditRecordId;
	private long domainId;
	private int examinedJourneys;
	private int generatedJourneys;
	
	public JourneyExaminationProgressMessage(long accountId, 
											 long auditRecordId, 
											 long domainId, 
											 int examined_journeys,
											 int generated_journeys
	) {
		setAccountId(accountId);
		setAuditRecordId(auditRecordId);
		setDomainId(domainId);
		setExaminedJourneys(examined_journeys);
		setGeneratedJourneys(generated_journeys);
	}

	public long getAccountId() {
		return accountId;
	}

	public void setAccountId(long accountId) {
		this.accountId = accountId;
	}

	public long getAuditRecordId() {
		return auditRecordId;
	}

	public void setAuditRecordId(long auditRecordId) {
		this.auditRecordId = auditRecordId;
	}

	public long getDomainId() {
		return domainId;
	}

	public void setDomainId(long domainId) {
		this.domainId = domainId;
	}

	public int getExaminedJourneys() {
		return examinedJourneys;
	}

	public void setExaminedJourneys(int examinedJourneys) {
		this.examinedJourneys = examinedJourneys;
	}

	public int getGeneratedJourneys() {
		return generatedJourneys;
	}

	public void setGeneratedJourneys(int generatedJourneys) {
		this.generatedJourneys = generatedJourneys;
	}

}
