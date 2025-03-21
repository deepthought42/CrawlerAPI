package com.crawlerApi.models.audit;

import com.crawlerApi.models.SimpleElement;

//deprecated on 8-24-2021
@Deprecated
public class IssueElementMap {
	private UXIssueMessage issue;
	private SimpleElement element;

	
	public IssueElementMap(
			UXIssueMessage issue_msg,
			SimpleElement element
	) {
		setIssue(issue_msg);
		setElement(element);
	}


	public UXIssueMessage getIssue() {
		return issue;
	}

	public void setIssue(UXIssueMessage issue_msg) {
		this.issue = issue_msg;
	}

	public SimpleElement getElement() {
		return element;
	}

	public void setElement(SimpleElement element) {
		this.element = element;
	}
}
