package com.looksee.models.audit;

import java.util.Set;

public class ElementIssueTwoWayMapping {
	private Set<IssueElementMap> issues;
	private Set<ElementIssueMap> element_issues;
	private AuditScore audit_score;
	private String page_src;
	
	public ElementIssueTwoWayMapping(
			Set<IssueElementMap> issues,
			Set<ElementIssueMap> element_issues,
			AuditScore audit_score,
			String page_src
	) {
		setIssues(issues);
		setElementIssues(element_issues);
		setScores(audit_score);
		setPageSrc(page_src);
	}


	public Set<IssueElementMap> getIssues() {
		return issues;
	}


	public void setIssues(Set<IssueElementMap> issues) {
		this.issues = issues;
	}


	public Set<ElementIssueMap> getElementIssues() {
		return element_issues;
	}


	public void setElementIssues(Set<ElementIssueMap> element_issues) {
		this.element_issues = element_issues;
	}


	public AuditScore getScores() {
		return audit_score;
	}


	public void setScores(AuditScore audit_score) {
		this.audit_score = audit_score;
	}


	public String getPageSrc() {
		return page_src;
	}


	public void setPageSrc(String page_src) {
		this.page_src = page_src;
	}

}
