package com.qanairy.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;

import com.qanairy.models.rules.RequirementRule;

/**
 * 
 */
public interface RequirementRuleRepository extends Neo4jRepository<RequirementRule, Long> {
	public RequirementRule findByKey(@Param("key") String key);
}
