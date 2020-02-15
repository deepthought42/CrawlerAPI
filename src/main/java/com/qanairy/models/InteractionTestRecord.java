package com.qanairy.models;

import org.neo4j.ogm.annotation.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * 
 */
public class InteractionTestRecord extends TestRecord<Path> {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(InteractionTestRecord.class);

	@Relationship(type = "HAS", direction = Relationship.OUTGOING)
	private Path result;

	@Override
	public Path getResult() {
		return result;
	}

	@Override
	public void setResult(Path result) {
		this.result = result;
	}
}
