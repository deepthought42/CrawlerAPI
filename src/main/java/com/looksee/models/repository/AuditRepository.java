package com.looksee.models.repository;

import java.util.Set;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;

import com.looksee.models.audit.Audit;
import com.looksee.models.audit.UXIssueMessage;

/**
 * Repository interface for Spring Data Neo4j to handle interactions with {@link Audit} objects
 */
public interface AuditRepository extends Neo4jRepository<Audit, Long> {
	public Audit findByKey(@Param("key") String key);

	@Query("MATCH (audit:Audit)-[:HAS]-(issue:UXIssueMessage) WHERE id(audit)=$audit_id OPTIONAL MATCH y=(issue)-->(element) RETURN issue, element")
	public Set<UXIssueMessage> findIssueMessages(@Param("audit_id") long audit_id);

	@Query("MATCH (audit:Audit{key:$key}),(msg:UXIssueMessage{key:$msg_key}) CREATE audit_issue=(audit)-[observed:OBSERVED]->(msg) RETURN msg")
	public UXIssueMessage addIssueMessage(@Param("key") String key, 
									  @Param("msg_key") String issue_msg_key);
}
