package com.looksee.models.repository;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.looksee.models.ElementState;
import com.looksee.models.audit.UXIssueMessage;

@Repository
public interface UXIssueMessageRepository extends Neo4jRepository<UXIssueMessage, Long>  {
	public UXIssueMessage findByKey(@Param("key") String key);

	@Query("MATCH (uim:UXIssueMessage)-[]->(e:ElementState) WHERE id(uim)=$id RETURN e")
	public ElementState getElement(@Param("id") long id);
	
}
