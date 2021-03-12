package com.qanairy.models.audit;

import org.neo4j.ogm.annotation.Relationship;

import com.qanairy.models.Element;
import com.qanairy.models.PageState;
import com.qanairy.models.enums.ObservationType;
import com.qanairy.models.enums.Priority;

/**
 * A observation of potential error for a given {@link Element element} 
 */
public class PageStateObservation extends Observation {

	@Relationship(type = "FOR")
	private PageState page_state;
	
	public PageStateObservation() {}
	
	public PageStateObservation(
				PageState page, 
				String description, 
				String why_it_matters, 
				String ada_compliance, 
				Priority priority) {
		setPage(page);
		setDescription(description);
		setType(ObservationType.PAGE_STATE);
		setWhyItMatters(why_it_matters);
		setAdaCompliance(ada_compliance);
		setPriority(priority);
		setKey(this.generateKey());
	}
	
	/*
	@Override
	public String generateKey() {
		assert page_state != null;
		
		return "observation::"+org.apache.commons.codec.digest.DigestUtils.sha256Hex( page_state.getKey() + this.getDescription() );
	}
	*/


	public PageState getElements() {
		return page_state;
	}


	public void setPage(PageState page_state) {
		this.page_state = page_state;
	}

	@Override
	public ObservationType getType() {
		return ObservationType.PAGE_STATE;
	}
}
