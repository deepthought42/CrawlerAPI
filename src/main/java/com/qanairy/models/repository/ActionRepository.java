package com.qanairy.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;

import com.qanairy.models.Action;

/**
 * 
 */
public interface ActionRepository extends Neo4jRepository<Action, Long> {
	public	Action findByKey(@Param("key") String key);

}
