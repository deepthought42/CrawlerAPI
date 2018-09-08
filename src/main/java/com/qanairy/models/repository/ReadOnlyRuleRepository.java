package com.qanairy.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;

import com.qanairy.models.rules.ReadOnlyRule;

/**
 * 
 */
public interface ReadOnlyRuleRepository extends Neo4jRepository<ReadOnlyRule, Long> {
	public ReadOnlyRule findByKey(@Param("key") String key);
}
