package com.looksee.models.journeys;

import org.neo4j.ogm.annotation.Relationship;

import com.looksee.models.LookseeObject;
import com.looksee.models.PageState;

public class Step extends LookseeObject {
	@Relationship(type = "STARTS_WITH")
	private PageState start_page;
	
	@Relationship(type = "ENDS_WITH")
	private PageState end_page;

	public PageState getStartPage() {
		return start_page;
	}
	
	public void setStartPage(PageState page_state) {
		this.start_page = page_state;
	}
	
	
	public PageState getEndPage() {
		return this.end_page;
	}
	
	public void setEndPage(PageState page_state) {
		this.end_page = page_state;
	}
	
	@Override
	public String generateKey() {
		String key = "";
		if(start_page != null) {
			key += start_page.getId();
		}
		if(end_page != null) {
			key += end_page.getId();
		}
		return "step"+key;
	}
}
