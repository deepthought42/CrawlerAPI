package com.qanairy.models;

import org.neo4j.ogm.annotation.NodeEntity;

@NodeEntity
public class Element implements Persistable{
	private String xpath;

	@Override
	public String generateKey() {
		return xpath.hashCode()+"";
	}
}
