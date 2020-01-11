package com.qanairy.models.experience;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

import com.qanairy.models.enums.AuditType;

/**
 * 
 */
@NodeEntity
public class AuditDetail {
	@GeneratedValue
    @Id
	private Long id;
	private String type;

	public AuditType getType() {
		return AuditType.create(this.type.toLowerCase());
	};

	public void setType(AuditType type) {
		this.type = type.getShortName();
	}
}
