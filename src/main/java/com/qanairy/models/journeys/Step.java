package com.qanairy.models.journeys;

import org.neo4j.ogm.annotation.NodeEntity;

import com.qanairy.models.LookseeObject;

/**
 * A set of Steps
 */
@NodeEntity
public abstract class Step extends LookseeObject {
	public Step() {
		super();
	}
}
