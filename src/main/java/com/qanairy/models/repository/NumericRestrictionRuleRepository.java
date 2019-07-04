package com.qanairy.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import com.qanairy.models.rules.NumericRestrictionRule;

/**
 * 
 */
public interface NumericRestrictionRuleRepository extends Neo4jRepository<NumericRestrictionRule, Long> {
	public NumericRestrictionRule findByKey(@Param("key") String key);
}
