package com.qanairy.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;

import com.qanairy.models.rules.NumericRule;

/**
 * 
 */
public interface NumericRuleRepository extends Neo4jRepository<NumericRule, Long> {
	public NumericRule findByKey(@Param("key") String key);
}
