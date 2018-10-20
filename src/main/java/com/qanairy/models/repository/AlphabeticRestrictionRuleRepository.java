package com.qanairy.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;

import com.qanairy.models.rules.AlphabeticRestrictionRule;

/**
 * 
 */
public interface AlphabeticRestrictionRuleRepository extends Neo4jRepository<AlphabeticRestrictionRule, Long> {
	public AlphabeticRestrictionRule findByKey(@Param("key") String key);
}
