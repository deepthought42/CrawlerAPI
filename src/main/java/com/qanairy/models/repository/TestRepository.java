package com.qanairy.models.repository;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;

import com.qanairy.models.Domain;
import com.qanairy.models.Test;
import com.qanairy.models.TestRecord;

/**
 * 
 */
public interface TestRepository extends Neo4jRepository<Test, Long> {
	public Test findByKey(@Param("key") String key);
	public Test findByName(@Param("name") String name);

	@Query("MATCH (d:Domain)-[:HAS_TEST]->(t:Test{key:{key}}) RETURN d")
	public Domain getDomain();
	
	@Query("MATCH p=(t:Test{key:{key}})-[:HAS_GROUP]->() RETURN p")
	public Test getFullTestUsingKey(@Param("key") String key);
	
	@Query("MATCH p=(t:Test{key:{key}})-[r:HAS_TEST_RECORD]->(tr:TestRecord) RETURN tr ORDER BY tr.ran_at DESC LIMIT 1")
	public TestRecord getMostRecentRecord(@Param("key") String key);
}
