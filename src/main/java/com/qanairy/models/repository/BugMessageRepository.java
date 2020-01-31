package com.qanairy.models.repository;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.qanairy.models.experience.AccessibilityDetail;

/**
 * Spring Data interface for interacting with {@link AccessibilityDetail} objects in database
 */
@Repository
public interface BugMessageRepository extends Neo4jRepository<AccessibilityDetail, Long> {

	@Query("MATCH (bm:BugMessage{message:{message}}) RETURN bm LIMIT 1")
	public AccessibilityDetail findByMessage(@Param("message") String message);
}
