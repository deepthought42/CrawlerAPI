package com.qanairy.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;

import com.qanairy.models.rules.EmailPatternRule;

/**
 * 
 */
public interface EmailPatternRuleRepository extends Neo4jRepository<EmailPatternRule, Long> {
	public EmailPatternRule findByKey(@Param("key") String key);
}
