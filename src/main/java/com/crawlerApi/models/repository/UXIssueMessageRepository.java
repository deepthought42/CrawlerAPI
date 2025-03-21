package com.crawlerApi.models.repository;

import java.util.Set;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.crawlerApi.models.audit.UXIssueMessage;

@Repository
public interface UXIssueMessageRepository extends Neo4jRepository<UXIssueMessage, Long>  {
	public UXIssueMessage findByKey(@Param("key") String key);

	@Query("MATCH (uim:UXIssueMessage) WITH uim MATCH (e:ElementState) WHERE id(uim)=$issue_id AND id(e)=$element_id MERGE (uim)-[r:FOR]->(e) RETURN r")
	public void addElement(@Param("issue_id") long issue_id, @Param("element_id") long element_id);

	@Query("MATCH (uim:UXIssueMessage) WITH uim MATCH (e:PageState) WHERE id(uim)=$issue_id AND id(e)=$page_id MERGE (uim)-[r:FOR]->(e) RETURN r")
	public void addPage(@Param("issue_id") long issue_id, @Param("page_id") long page_id);
	
	@Query("MATCH (audit_record:PageAuditRecord)-[]-(audit:Audit)  MATCH (audit)-[:HAS]-(issue:UXIssueMessage) WHERE id(audit_record)=$audit_record_id RETURN issue")
	public Set<UXIssueMessage> getIssues(@Param("audit_record_id") long audit_record_id);

	@Query("MATCH (audit:Audit)-[:HAS]-(issue:UXIssueMessage) WHERE id(audit)=$audit_id OPTIONAL MATCH y=(issue)-->(element) RETURN issue, element")
	public Set<UXIssueMessage> findIssueMessages(@Param("audit_id") long audit_id);
	
	@Query("MATCH (audit:Audit{key:$key}) WITH audit MATCH (msg:UXIssueMessage{key:$msg_key}) MERGE audit_issue=(audit)-[:HAS]->(msg) RETURN msg")
	public UXIssueMessage addIssueMessage(@Param("key") String key, 
									  @Param("msg_key") String issue_msg_key);
}
