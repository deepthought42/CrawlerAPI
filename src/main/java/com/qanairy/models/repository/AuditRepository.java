package com.qanairy.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;

import com.qanairy.models.experience.Audit;

/**
 * 
 */
public interface AuditRepository extends Neo4jRepository<Audit, Long> {
	public Audit findByKey(@Param("key") String key);

}
