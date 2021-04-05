package com.qanairy.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;

import com.qanairy.models.AuditStats;

public interface AuditStatsRepository extends Neo4jRepository<AuditStats, Long> {
	
}
