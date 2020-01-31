package com.qanairy.models.experience;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

/**
 * 
 */
@NodeEntity
public abstract class AuditDetail {

	@GeneratedValue
    @Id
	private Long id;
	
	public Long getId() {
		return this.id;
	}
}
