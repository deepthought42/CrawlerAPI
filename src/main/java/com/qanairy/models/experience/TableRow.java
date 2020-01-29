package com.qanairy.models.experience;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a data row in a table. This class is intended to be extended for various table types. 
 * The reason for the existence of this object is to create a more dynamic table structure
 */
@NodeEntity
public abstract class TableRow {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(TableRow.class);
	
	@GeneratedValue
	@Id
	private Long id;
	
	@Properties
	private Map<String, ?> row = new HashMap<>();

	public abstract Map<String, ?> getRow();

	public abstract void setRow(Map<String, ?> row);
}
