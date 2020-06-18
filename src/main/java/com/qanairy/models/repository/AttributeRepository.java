package com.qanairy.models.repository;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;

import com.qanairy.models.Attribute;

/**
 * Defines how a persisted {@link Attribute} can be interacted with
 */
public interface AttributeRepository  extends Neo4jRepository<Attribute, Long> {
	@Query("MATCH (p:Attribute{key:{key}}) RETURN p LIMIT 1")
	public Attribute findByKey(@Param("key") String key);

}
