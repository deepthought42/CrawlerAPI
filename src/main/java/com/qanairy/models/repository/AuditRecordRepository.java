package com.qanairy.models.repository;

import java.util.Optional;
import java.util.Set;

import javax.validation.constraints.NotBlank;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;

import com.qanairy.models.audit.Audit;
import com.qanairy.models.audit.AuditRecord;

/**
 * Repository interface for Spring Data Neo4j to handle interactions with {@link Audit} objects
 */
public interface AuditRecordRepository extends Neo4jRepository<AuditRecord, Long> {
	public AuditRecord findByKey(@Param("key") String key);
	
	@Query("MATCH (:AuditRecord{key:{audit_record_key}})-[:HAS]->(a:Audit{key:{audit_key}}) RETURN a")
	public Optional<Audit> getAuditForPageState(@Param("audit_record_key") String audit_record_key, @Param("audit_key") String audit_key);

	@Query("MATCH(ar:AuditRecord{key:{audit_record_key}}),(a:Audit{key:{audit_key}}) CREATE (ar)-[h:HAS]->(a) RETURN ar")
	public void addAudit(@Param("audit_record_key") String audit_record_key, @Param("audit_key") String audit_key);

	@Query("MATCH(ar:AuditRecord{key:{audit_record_key}})-[]->(audit:Audit) OPTIONAL MATCH y=(audit)-->(e) OPTIONAL MATCH z=(e)-->(f) RETURN audit,y,z")
	public Set<Audit> getAllAudits(@Param("audit_record_key") String audit_record_key);

	@Query("MATCH (d:Domain{host:{domain_url}})-[]-(ar:AuditRecord) RETURN ar ORDER BY ar.created_at DESC LIMIT 1")
	public Optional<AuditRecord> findMostRecent(@Param("domain_url") @NotBlank String domain_url);

	@Query("MATCH(ar:AuditRecord{key:{audit_record_key}})-[]->(audit:Audit{category:'Color Management'}) OPTIONAL MATCH y=(audit)-->(e) OPTIONAL MATCH z=(e)-->(f) RETURN audit,y,z")
	public Set<Audit> getAllColorManagementAudits(@Param("audit_record_key") String audit_record_key);
}
