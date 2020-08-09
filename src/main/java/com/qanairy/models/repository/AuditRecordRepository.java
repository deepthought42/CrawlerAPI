package com.qanairy.models.repository;

import java.util.Optional;
import java.util.Set;

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

	@Query("MATCH(ar:AuditRecord{key:{audit_record_key}})-[]->(audit:Audit) OPTIONAL MATCH z=(audit)-->(e) OPTIONAL MATCH y=(e)-->() RETURN z,y")
	public Set<Audit> getAllAudits(@Param("audit_record_key") String audit_record_key);
}
