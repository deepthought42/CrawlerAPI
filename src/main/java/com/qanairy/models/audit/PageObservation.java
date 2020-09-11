package com.qanairy.models.audit;

import org.neo4j.ogm.annotation.Relationship;

import com.qanairy.models.Element;
import com.qanairy.models.PageVersion;
import com.qanairy.models.enums.ObservationType;

/**
 * A observation of potential error for a given {@link Element element} 
 */
public class PageObservation extends Observation {

	@Relationship(type = "FOR")
	private PageVersion page;
	
	public PageObservation() {}
	
	public PageObservation(PageVersion page, String description) {
		setPage(page);
		setDescription(description);
		setType(ObservationType.PAGE);
		setKey(this.generateKey());
	}
	
	@Override
	public String generateKey() {
		assert page != null;
		
		return "observation::"+org.apache.commons.codec.digest.DigestUtils.sha256Hex( page.getKey() + this.getDescription() );
	}


	public PageVersion getElements() {
		return page;
	}


	public void setPage(PageVersion page) {
		this.page = page;
	}

	@Override
	public ObservationType getType() {
		return ObservationType.ELEMENT;
	}
}
