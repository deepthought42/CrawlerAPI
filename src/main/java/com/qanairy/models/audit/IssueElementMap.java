package com.qanairy.models.audit;

import java.util.Set;

import com.qanairy.models.SimpleElement;

public class IssueElementMap {
	private UXIssueMessage issue;
	private Set<SimpleElement> elements;

	
	public IssueElementMap(
			UXIssueMessage issue_msg,
			Set<SimpleElement> elements
	) {
		setIssue(issue_msg);
		setElements(elements);
	}


	public UXIssueMessage getIssue() {
		return issue;
	}

	public void setIssue(UXIssueMessage issue_msg) {
		this.issue = issue_msg;
	}

	public Set<SimpleElement> getElements() {
		return elements;
	}

	public void setElements(Set<SimpleElement> elements) {
		this.elements = elements;
	}
}
