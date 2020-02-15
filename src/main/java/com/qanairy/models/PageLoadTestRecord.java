package com.qanairy.models;

import org.neo4j.ogm.annotation.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * 
 */
public class PageLoadTestRecord extends TestRecord<PageState> {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(PageLoadTestRecord.class);

	@Relationship(type = "HAS", direction = Relationship.OUTGOING)
	private PageState result;

	@Override
	public PageState getResult() {
		return result;
	}

	@Override
	public void setResult(PageState result) {
		this.result = result;
	}
}
