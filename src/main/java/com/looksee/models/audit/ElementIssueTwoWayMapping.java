package com.looksee.models.audit;

import java.util.Set;

public class ElementIssueTwoWayMapping {
	private Set<IssueElementMap> issue_elements;
	private Set<ElementIssueMap> element_issues;

	
	public ElementIssueTwoWayMapping(
			Set<IssueElementMap> issue_elements,
			Set<ElementIssueMap> element_issues
	) {
		setIssueElements(issue_elements);
		setElementIssues(element_issues);
	}


	public Set<IssueElementMap> getIssueElements() {
		return issue_elements;
	}


	public void setIssueElements(Set<IssueElementMap> issue_elements) {
		this.issue_elements = issue_elements;
	}


	public Set<ElementIssueMap> getElementIssues() {
		return element_issues;
	}


	public void setElementIssues(Set<ElementIssueMap> element_issues) {
		this.element_issues = element_issues;
	}

}
