package com.qanairy.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import com.qanairy.models.rules.SpecialCharacterRestriction;

/**
 * 
 */
public interface SpecialCharacterRestrictionRuleRepository extends Neo4jRepository<SpecialCharacterRestriction, Long> {
	public SpecialCharacterRestriction findByKey(@Param("key") String key);
}
