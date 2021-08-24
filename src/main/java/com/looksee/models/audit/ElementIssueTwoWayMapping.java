package com.looksee.models.audit;

import java.util.Map;
import java.util.Set;

import com.looksee.models.SimpleElement;

public class ElementIssueTwoWayMapping {
	private Map<String, UXIssueMessage> issues;
	private Map<String, SimpleElement> elements;
	
	private Map<String, String> issues_element_map; // 1 to 1 correlation
	private Map<String, Set<String>> element_issues; // 1 to many correlation
	
	private AuditScore audit_score;
	private String page_src;
	
	public ElementIssueTwoWayMapping(
			Map<String, UXIssueMessage> issues,
			Map<String, SimpleElement> elements,
			Map<String, String> issue_element_map,
			Map<String, Set<String>> element_issues, 
			AuditScore audit_score, 
			String page_src
	) {
		setIssues(issues);
		setElements(elements);
		setIssuesElementMap(issues_element_map);
		setElementIssues(element_issues);
		setScores(audit_score);
		setPageSrc(page_src);
	}


	public Map<String, UXIssueMessage> getIssues() {
		return issues;
	}


	public void setIssues(Map<String, UXIssueMessage> issues) {
		this.issues = issues;
	}


	public Map<String, SimpleElement> getElements() {
		return elements;
	}


	public void setElements(Map<String, SimpleElement> elements) {
		this.elements = elements;
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


	public Map<String, String> getIssuesElementMap() {
		return issues_element_map;
	}


	public void setIssuesElementMap(Map<String, String> issues_element_map) {
		this.issues_element_map = issues_element_map;
	}


	public Map<String, Set<String>> getElementIssues() {
		return element_issues;
	}


	public void setElementIssues(Map<String, Set<String>> element_issues) {
		this.element_issues = element_issues;
	}

}
