package com.qanairy.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import com.qanairy.models.rules.Rule;

/**
 * 
 */
public interface PatternRuleRepository extends Neo4jRepository<Rule, Long> {
	public Rule findByKey(@Param("key") String key);
}
