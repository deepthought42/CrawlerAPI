package com.qanairy.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;

import com.qanairy.models.experience.AuditDetail;

/**
 * Repository interface for Spring Data Neo4j to handle interactions with {@link AuditDetail} objects
 */
public interface AuditDetailRepository extends Neo4jRepository<AuditDetail, Long> {

}
