package com.qanairy.models.repository;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import com.qanairy.models.Test;

/**
 * 
 */
public interface TestRepository extends Neo4jRepository<Test, Long> {
	public Test findByKey(@Param("key") String key);
	public Test findByName(@Param("name") String name);

	@Query("MATCH p=(t:Test{key:{key}})-[:HAS_GROUP]->() RETURN p")
	public Test getFullTestUsingKey(@Param("key") String key);
}
