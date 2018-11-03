package com.qanairy.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;

import com.qanairy.models.rules.DisabledRule;

/**
 * 
 */
public interface DisabledRuleRepository extends Neo4jRepository<DisabledRule, Long> {
	public DisabledRule findByKey(@Param("key") String key);
}
