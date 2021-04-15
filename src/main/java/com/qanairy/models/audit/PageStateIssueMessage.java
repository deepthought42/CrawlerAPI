package com.qanairy.models.audit;

import org.neo4j.ogm.annotation.Relationship;

import com.qanairy.models.Element;
import com.qanairy.models.PageState;
import com.qanairy.models.enums.Priority;

/**
 * A observation of potential error for a given {@link Element element} 
 */
public class PageStateIssueMessage extends UXIssueMessage {

	@Relationship(type = "FOR")
	private PageState page_state;
	
	public PageStateIssueMessage() {}
	
	public PageStateIssueMessage(
				PageState page, 
				String recommendation,
				Priority priority
	) {
		setPage(page);
		setRecommendation(recommendation);
		setPriority(priority);
		setKey(this.generateKey());
	}
	
	/*
	@Override
	public String generateKey() {
		assert page_state != null;
		
		return "observation"+org.apache.commons.codec.digest.DigestUtils.sha256Hex( page_state.getKey() + this.getDescription() );
	}
	*/


	public PageState getElements() {
		return page_state;
	}


	public void setPage(PageState page_state) {
		this.page_state = page_state;
	}
}
