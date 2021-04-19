package com.qanairy.models.repository;

import java.util.Optional;
import java.util.Set;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;

import com.qanairy.models.PageState;
import com.qanairy.models.audit.Audit;
import com.qanairy.models.audit.AuditRecord;
import com.qanairy.models.audit.DomainAuditRecord;
import com.qanairy.models.audit.PageAuditRecord;

/**
 * Repository interface for Spring Data Neo4j to handle interactions with {@link Audit} objects
 */
public interface AuditRecordRepository extends Neo4jRepository<AuditRecord, Long> {
	public AuditRecord findByKey(@Param("key") String key);
	
	@Query("MATCH (:AuditRecord{key:$audit_record_key})-[:HAS]->(a:Audit{key:$audit_key}) RETURN a")
	public Optional<Audit> getAuditForAuditRecord(@Param("audit_record_key") String audit_record_key, @Param("audit_key") String audit_key);

	@Query("MATCH (ar:AuditRecord{key:$audit_record_key}),(a:Audit{key:$audit_key}) CREATE (ar)-[h:HAS]->(a) RETURN ar")
	public void addAudit(@Param("audit_record_key") String audit_record_key, @Param("audit_key") String audit_key);
	
	@Query("MATCH (dar:DomainAuditRecord{key:$domain_audit_record_key}),(par:PageAuditRecord{key:$page_audit_key}) CREATE (dar)-[h:HAS]->(par) RETURN dar")
	public void addPageAuditRecord(@Param("domain_audit_record_key") String domain_audit_record_key, @Param("page_audit_key") String page_audit_key);

	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit) OPTIONAL MATCH y=(audit)-->(e) OPTIONAL MATCH z=(e)-->(f) RETURN audit,y,z")
	public Set<Audit> getAllAudits(@Param("audit_record_key") String audit_record_key);

	@Query("MATCH (d:Domain{host:$domain_host})-[]-(ar:DomainAuditRecord) RETURN ar ORDER BY ar.created_at DESC LIMIT 1")
	public Optional<DomainAuditRecord> findMostRecentDomainAuditRecord(@Param("domain_host")  String domain_host);

	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{category:'Color Management'}) WHERE audit.level='domain' RETURN audit")
	public Set<Audit> getAllColorManagementAudits(@Param("audit_record_key") String audit_record_key);

	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{category:'Visuals'}) WHERE audit.level='domain' RETURN audit")
	public Set<Audit> getAllVisualAudits(@Param("audit_record_key") String audit_record_key);

	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{subcategory:'Color Palette'}) WHERE audit.level='page' RETURN audit")
	public Set<Audit> getAllPageColorPaletteAudits(@Param("audit_record_key") String audit_record_key);

	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{subcategory:'Text Background Contrast'}) WHERE audit.level='page' RETURN audit")
	public Set<Audit> getAllPageTextColorContrastAudits(@Param("audit_record_key") String audit_record_key);

	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{subcategory:'Non Text Background Contrast'}) WHERE audit.level='page' RETURN audit")
	public Set<Audit> getAllPageNonTextColorContrastAudits(@Param("audit_record_key") String audit_record_key);

	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{category:'Typography'}) WHERE audit.level='domain' RETURN audit")
	public Set<Audit> getAllTypographyAudits(@Param("audit_record_key") String key);

	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{subcategory:'Typefaces'}) WHERE audit.level='page' RETURN audit")
	public Set<Audit> getAllPageTypefaceAudits(@Param("audit_record_key") String audit_record_key);

	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{category:'Information Architecture'}) WHERE audit.level='domain' RETURN audit")
	public Set<Audit> getAllInformationArchitectureAudits(@Param("audit_record_key") String audit_record_key);

	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{subcategory:'Links'}) WHERE audit.level='page' RETURN audit")
	public Set<Audit> getAllPageLinkAudits(@Param("audit_record_key") String audit_record_key);

	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{subcategory:'Titles'}) WHERE audit.level='page' RETURN audit")
	public Set<Audit> getAllPageTitleAndHeaderAudits(@Param("audit_record_key") String audit_record_key);

	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{subcategory:'Alt Text'}) WHERE audit.level='page' RETURN audit")
	public Set<Audit> getAllPageAltTextAudits(@Param("audit_record_key") String audit_record_key);

	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{subcategory:'Margin'}) WHERE audit.level='page' RETURN audit")
	public Set<Audit> getAllPageMarginAudits(@Param("audit_record_key") String audit_record_key);
	
	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{subcategory:'Padding'}) WHERE audit.level='page' RETURN audit")
	public Set<Audit> getAllPagePaddingAudits(@Param("audit_record_key") String audit_record_key);

	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{subcategory:'Paragraphing'}) WHERE audit.level='page' RETURN audit")
	public Set<Audit> getAllPageParagraphingAudits(@Param("audit_record_key") String audit_record_key);

	@Query("MATCH (ar:DomainAuditRecord{key:$domain_audit_record_key})-[]->(page_audit:PageAuditRecord) RETURN page_audit")
	public Set<PageAuditRecord> getAllPageAudits(@Param("domain_audit_record_key") String domain_audit_record_key);

	@Query("MATCH (page_audit:PageAuditRecord{key:$page_audit_key})-[]->(audit:Audit) MATCH y=(audit)-[]->(issue:UXIssueMessage) OPTIONAL MATCH z=(issue)-->(:ElementState) RETURN audit,y,z")
	public Set<Audit> getAllAuditsForPageAuditRecord(@Param("page_audit_key") String page_audit_key);

	@Query("MATCH (page_audit:PageAuditRecord)-[]->(page_state:PageState{url:$url}) RETURN page_audit ORDER BY page_audit.created_at DESC LIMIT 1")
	public Optional<PageAuditRecord> getMostRecentPageAuditRecord(@Param("url") String url);

	@Query("MATCH (page_audit:PageAuditRecord{key:$page_audit_key})-[]->(page_state:PageState) RETURN page_state LIMIT 1")
	public PageState getPageStateForAuditRecord(@Param("page_audit_key") String page_audit_key);
}
