package com.qanairy.models.repository;

import java.util.List;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;

import com.qanairy.models.audit.AuditRecord;
import com.qanairy.models.audit.ColorPaletteAudit;

/**
 * Repository interface for Spring Data Neo4j to handle interactions with {@link AuditRecord} objects
 */
public interface AuditRecordRepository extends Neo4jRepository<AuditRecord, Long> {
	public AuditRecord findByKey(@Param("key") String key);

	@Query("MATCH (a:AuditRecord)-[]-(audit:ColorPaletteAudit) WHERE a.key IN {audit_record_keys} RETURN audit")
	public List<ColorPaletteAudit> findColorPaletteByAuditRecords(@Param("audit_record_keys") List<String> audit_record_keys);
}
