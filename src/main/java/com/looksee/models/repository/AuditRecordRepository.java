package com.looksee.models.repository;

import java.util.Optional;
import java.util.Set;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;

import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.DomainAuditRecord;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.models.audit.UXIssueMessage;

/**
 * Repository interface for Spring Data Neo4j to handle interactions with {@link Audit} objects
 */
public interface AuditRecordRepository extends Neo4jRepository<AuditRecord, Long> {
	public AuditRecord findByKey(@Param("key") String key);
	
	@Query("MATCH (:AuditRecord{key:$audit_record_key})-[:HAS]->(a:Audit{key:$audit_key}) RETURN a")
	public Optional<Audit> getAuditForAuditRecord(@Param("audit_record_key") String audit_record_key, @Param("audit_key") String audit_key);

	@Query("MATCH (ar:AuditRecord{key:$audit_record_key}),(a:Audit{key:$audit_key}) CREATE (ar)-[h:HAS]->(a) RETURN ar")
	public void addAudit(@Param("audit_record_key") String audit_record_key, @Param("audit_key") String audit_key);
	
	@Query("MATCH (dar:DomainAuditRecord),(par:PageAuditRecord{key:$page_audit_key}) WHERE id(dar)=$domain_audit_record_id CREATE (dar)-[h:HAS]->(par) RETURN dar")
	public void addPageAuditRecord(@Param("domain_audit_record_id") long domain_audit_record_id, @Param("page_audit_key") String page_audit_key);

	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit) OPTIONAL MATCH y=(audit)-->(e) OPTIONAL MATCH z=(e)-->(f) RETURN audit,y,z")
	public Set<Audit> getAllAudits(@Param("audit_record_key") String audit_record_key);

	@Query("MATCH (d:Domain{host:$domain_host})-[]-(ar:DomainAuditRecord) RETURN ar ORDER BY ar.created_at DESC LIMIT 1")
	@Deprecated
	public Optional<DomainAuditRecord> findMostRecentDomainAuditRecord(@Param("domain_host")  String domain_host);

	@Query("MATCH (d:Domain)-[]-(ar:DomainAuditRecord) WHERE id(d)=$domain_id RETURN ar ORDER BY ar.created_at DESC LIMIT 1")
	public Optional<DomainAuditRecord> findMostRecentDomainAuditRecord(@Param("domain_id") long domain_id);

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

	@Query("MATCH (domain_audit:DomainAuditRecord)-[]->(audit:PageAuditRecord) WHERE id(domain_audit)=$domain_audit_id RETURN audit")
	public Set<PageAuditRecord> getAllPageAudits(@Param("domain_audit_id") long domain_audit_id);

	@Query("MATCH (page_audit:PageAuditRecord{key:$page_audit_key})-[]->(audit:Audit) MATCH y=(audit)-[]->(issue:UXIssueMessage) OPTIONAL MATCH z=(issue)-->(:ElementState) RETURN audit,y,z")
	@Deprecated
	public Set<Audit> getAllAuditsForPageAuditRecord(@Param("page_audit_key") String page_audit_key);

	@Query("MATCH (page_audit:PageAuditRecord)-[]->(audit:Audit) WHERE id(page_audit)=$page_audit_id RETURN audit")
	public Set<Audit> getAllAuditsForPageAuditRecord(@Param("page_audit_id") long page_audit_id);

	@Query("MATCH (page_audit:PageAuditRecord)-[]->(page_state:PageState{url:$url}) RETURN page_audit ORDER BY page_audit.created_at DESC LIMIT 1")
	public Optional<PageAuditRecord> getMostRecentPageAuditRecord(@Param("url") String url);

	@Query("MATCH (page_audit:PageAuditRecord{key:$page_audit_key})-[]->(page_state:PageState) RETURN page_state LIMIT 1")
	@Deprecated
	public PageState getPageStateForAuditRecord(@Param("page_audit_key") String page_audit_key);

	@Query("MATCH (page_audit:PageAuditRecord)-[]->(page_state:PageState) WHERE id(page_audit) = $page_audit_id RETURN page_state LIMIT 1")
	public PageState getPageStateForAuditRecord(@Param("page_audit_id") long page_audit_id);
	
	@Query("MATCH (page_audit:PageAuditRecord)-[]->(page_state:PageState{key:$page_key}) MATCH (page_audit)-[]->(audit:Audit) RETURN audit")
	public Set<Audit> getMostRecentAuditsForPage(@Param("page_key") String key);

	@Query("MATCH (ar:DomainAuditRecord)-[]->(par:PageAuditRecord) MATCH (par)-[]->(audit:Audit{category:'Content'}) WHERE id(ar)=$id RETURN audit")
	public Set<Audit> getAllContentAuditsForDomainRecord(@Param("id") long id);

	@Query("MATCH (ar:DomainAuditRecord)-[]->(par:PageAuditRecord) MATCH (par)-[]->(audit:Audit{category:'Information Architecture'})  WHERE id(ar)=$id RETURN audit")
	public Set<Audit> getAllInformationArchitectureAuditsForDomainRecord(@Param("id") long id);

	@Query("MATCH (ar:DomainAuditRecord)-[]->(par:PageAuditRecord) MATCH (par)-[]->(audit:Audit{category:'Aesthetics'}) WHERE id(ar) = $id RETURN audit")
	public Set<Audit> getAllAestheticsAuditsForDomainRecord(@Param("id") long id);

	@Query("MATCH (ar:AuditRecord)-[]->(audit:Audit{category:'Content'}) WHERE id(ar)=$id RETURN audit")
	public Set<Audit> getAllContentAudits(@Param("id") long id);

	@Query("MATCH (ar:AuditRecord)-[]->(audit:Audit{category:'Information Architecture'})  WHERE id(ar)=$id RETURN audit")
	public Set<Audit> getAllInformationArchitectureAudits(@Param("id") long id);

	@Query("MATCH (ar:AuditRecord)-[]->(audit:Audit{category:'Aesthetics'}) WHERE id(ar)=$id RETURN audit")
	public Set<Audit> getAllAestheticsAudits(@Param("id") long id);

	@Query("MATCH (dar:DomainAuditRecord)-[]->(par:PageAuditRecord) WHERE id(dar)=$domain_audit_id RETURN par")
	public Set<PageAuditRecord> getPageAuditRecord(@Param("domain_audit_id") long domain_audit_id);

	@Query("MATCH (dar:DomainAuditRecord)-[]->(par:PageAuditRecord) MATCH (par)-[]->(audit:Audit{is_accessibility:true}) WHERE id(dar)=$domain_audit_id RETURN audit")
	public Set<Audit> getAllAccessibilityAuditsForDomainRecord(@Param("domain_audit_id") long domain_audit_id);

	@Query("MATCH (par:PageAuditRecord)-[]->(audit:Audit{is_accessibility:true}) WHERE id(par)=$page_audit_id RETURN audit")
	public Set<Audit> getAllAccessibilityAudits(@Param("page_audit_id") long page_audit_id);

	@Query("MATCH (audit_record:PageAuditRecord)-[]-(audit:Audit)  MATCH (audit)-[:HAS]-(issue:UXIssueMessage) WHERE id(audit_record)=$audit_record_id OPTIONAL MATCH y=(issue)-->(element) RETURN issue, element")
	public Set<UXIssueMessage> getIssues(@Param("audit_record_id") long audit_record_id);

}
