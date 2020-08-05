package com.qanairy.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.qanairy.models.Action;

/**
 * 
 */
@Repository
public interface ActionRepository extends Neo4jRepository<Action, Long> {
	public	Action findByKey(@Param("key") String key);

}
