package com.looksee.models.audit.performance;

import java.util.Date;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;


/**
 * 
 */
@Node
public abstract class AuditDetail {

	@GeneratedValue
    @Id
	private Long id;
	private Date created_date = new Date();
	
	public Long getId() {
		return this.id;
	}

	public Date getCreatedDate() {
		return created_date;
	}

	public void setCreatedDate(Date created_date) {
		this.created_date = created_date;
	}
}
