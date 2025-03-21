package com.crawlerApi.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;

import com.crawlerApi.models.AuditSubcategoryStat;

public interface AuditSubcategoryStatRepository extends Neo4jRepository<AuditSubcategoryStat, Long> {
	
}
