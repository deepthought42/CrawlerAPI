package com.crawlerApi.models.repository;

import java.util.Set;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import com.crawlerApi.models.Group;

/**
 * 
 */
public interface GroupRepository extends Neo4jRepository<Group, Long> {
	public Group findByKey(@Param("key") String key);

	@Query("MATCH (:Test{key:$key})-[:HAS_GROUP]-(g) RETURN g")
	public Set<Group> getGroups(@Param("key") String key);
	
}
