           package com.looksee.services;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.collections4.IterableUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.DomainAuditRecord;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.models.audit.UXIssueMessage;
import com.looksee.models.repository.AuditRecordRepository;

import io.github.resilience4j.retry.annotation.Retry;

/**
 * Contains business logic for interacting with and managing audits
 *
 */
@Service
@Retry(name = "neo4j")
public class AuditRecordService {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(AuditRecordService.class);

	@Autowired
	private AuditRecordRepository audit_record_repo;
	
	@Autowired
	private PageStateService page_state_service;

	public AuditRecord save(AuditRecord audit) {
		assert audit != null;
		
		log.warn("saving audit record with id ::   "+audit.getId());
		log.warn("saving audit record ::   " + audit.toString());

		return audit_record_repo.save(audit);
	}

	public Optional<AuditRecord> findById(long id) {
		return audit_record_repo.findById(id);
	}
	
	public AuditRecord findByKey(String key) {
		return audit_record_repo.findByKey(key);
	}


	public List<AuditRecord> findAll() {
		// TODO Auto-generated method stub
		return IterableUtils.toList(audit_record_repo.findAll());
	}
	
	public void addAudit(String audit_record_key, String audit_key) {
		//check if audit already exists for page state
		Optional<Audit> audit = audit_record_repo.getAuditForAuditRecord(audit_record_key, audit_key);
		if(!audit.isPresent()) {
			audit_record_repo.addAudit(audit_record_key, audit_key);
		}
	}

	public void addAudit(long audit_record_id, long audit_id) {
		//check if audit already exists for page state
		audit_record_repo.addAudit(audit_record_id, audit_id);
	}
	
	public Set<Audit> getAllAudits(String audit_record_key) {
		assert audit_record_key != null;
		assert !audit_record_key.isEmpty();
		
		return audit_record_repo.getAllAudits(audit_record_key);
	}

	/**
	 * deprecated in favor or using domain id for lookup
	 * @param host
	 * @return
	 */
	@Deprecated
	public Optional<DomainAuditRecord> findMostRecentDomainAuditRecord(String host) {
		assert host != null;
		assert !host.isEmpty();
		
		return audit_record_repo.findMostRecentDomainAuditRecord(host);
	}
	
	public Optional<DomainAuditRecord> findMostRecentDomainAuditRecord(long id) {
		return audit_record_repo.findMostRecentDomainAuditRecord(id);
	}
	
	public Optional<PageAuditRecord> findMostRecentPageAuditRecord(String page_url) {
		assert page_url != null;
		assert !page_url.isEmpty();
		
		return audit_record_repo.getMostRecentPageAuditRecord(page_url);
	}
	
	public Set<Audit> findMostRecentAuditsForPage(String page_url) {
		assert page_url != null;
		assert !page_url.isEmpty();
		
		//get most recent page state
		PageState page_state = page_state_service.findByUrl(page_url);
		return audit_record_repo.getMostRecentAuditsForPage(page_state.getKey());
		//return audit_record_repo.findMostRecentDomainAuditRecord(page_url);
	}

	public Set<Audit> getAllColorManagementAudits(String host) {
		assert host != null;
		assert !host.isEmpty();
		
        AuditRecord record = findMostRecentDomainAuditRecord(host).get();
		return audit_record_repo.getAllColorManagementAudits(record.getKey());
	}

	public Set<Audit> getAllColorPaletteAudits(String audit_record_key) {
		assert audit_record_key != null;
		assert !audit_record_key.isEmpty();
		
		return audit_record_repo.getAllPageColorPaletteAudits(audit_record_key);
	}

	public Set<Audit> getAllTextColorContrastAudits(String audit_record_key) {
		assert audit_record_key != null;
		assert !audit_record_key.isEmpty();
		
		return audit_record_repo.getAllPageTextColorContrastAudits(audit_record_key);
	}

	public Set<Audit> getAllNonTextColorContrastAudits(String audit_record_key) {
		assert audit_record_key != null;
		assert !audit_record_key.isEmpty();
		
		return audit_record_repo.getAllPageNonTextColorContrastAudits(audit_record_key);
	}

	public Set<Audit> getAllTypographyAudits( String host) {
		assert host != null;
		assert !host.isEmpty();
		
        AuditRecord record = findMostRecentDomainAuditRecord(host).get();
		return audit_record_repo.getAllTypographyAudits(record.getKey());
	}

	public Set<Audit> getAllTypefaceAudits(String audit_record_key) {
		assert audit_record_key != null;
		assert !audit_record_key.isEmpty();
		
		return audit_record_repo.getAllPageTypefaceAudits(audit_record_key);
	}

	////////////////////////////////////////
	//	INFORMATION ARCHITECTURE
	/////////////////////////////////////////	
	public Set<Audit> getAllInformationArchitectureAudits( String host) {
		assert host != null;
		assert !host.isEmpty();
		
        AuditRecord record = findMostRecentDomainAuditRecord(host).get();
		return audit_record_repo.getAllInformationArchitectureAudits(record.getKey());
	}

	public Set<Audit> getAllLinkAudits(String audit_record_key) {
		assert audit_record_key != null;
		assert !audit_record_key.isEmpty();
		
		return audit_record_repo.getAllPageLinkAudits(audit_record_key);
	}

	public Set<Audit> getAllTitleAndHeaderAudits(String audit_record_key) {
		assert audit_record_key != null;
		assert !audit_record_key.isEmpty();
		
		return audit_record_repo.getAllPageTitleAndHeaderAudits(audit_record_key);
	}

	public Set<Audit> getAllAltTextAudits(String audit_record_key) {
		assert audit_record_key != null;
		assert !audit_record_key.isEmpty();
		
		return audit_record_repo.getAllPageAltTextAudits(audit_record_key);
	}

	public Set<Audit> getAllVisualAudits( String host) {
		assert host != null;
		assert !host.isEmpty();
		
        AuditRecord record = findMostRecentDomainAuditRecord(host).get();
		return audit_record_repo.getAllVisualAudits(record.getKey());
	}

	public Set<Audit> getAllMarginAudits(String audit_record_key) {
		assert audit_record_key != null;
		assert !audit_record_key.isEmpty();
		
		return audit_record_repo.getAllPageMarginAudits(audit_record_key);
	}

	public Set<Audit> getAllPagePaddingAudits(String audit_record_key) {
		assert audit_record_key != null;
		assert !audit_record_key.isEmpty();
		
		return audit_record_repo.getAllPagePaddingAudits(audit_record_key);
	}

	public Set<Audit> getAllPageParagraphingAudits(String audit_record_key) {
		assert audit_record_key != null;
		assert !audit_record_key.isEmpty();
		
		return audit_record_repo.getAllPageParagraphingAudits(audit_record_key);
	}

	public Set<PageAuditRecord> getAllPageAudits(long audit_record_id) {		
		return audit_record_repo.getAllPageAudits(audit_record_id);
	}

	@Deprecated
	public Set<Audit> getAllAuditsForPageAuditRecord(String page_audit_key) {
		assert page_audit_key != null;
		assert !page_audit_key.isEmpty();
		
		return audit_record_repo.getAllAuditsForPageAuditRecord( page_audit_key);
	}
	
	public Set<Audit> getAllAuditsForPageAuditRecord(long page_audit_id) {		
		return audit_record_repo.getAllAuditsForPageAuditRecord( page_audit_id);
	}

	public void addPageAuditToDomainAudit(long domain_audit_record_id, String page_audit_record_key) {
		//check if audit already exists for page state
		audit_record_repo.addPageAuditRecord(domain_audit_record_id, page_audit_record_key);
	}

	public Optional<PageAuditRecord> getMostRecentPageAuditRecord(String url) {
		assert url != null;
		assert !url.isEmpty();
		
		return audit_record_repo.getMostRecentPageAuditRecord(url);
	}

	@Deprecated
	public PageState getPageStateForAuditRecord(String page_audit_key) {
		assert page_audit_key != null;
		assert !page_audit_key.isEmpty();
		
		return audit_record_repo.getPageStateForAuditRecord(page_audit_key);
	}

	public Set<Audit> getAllContentAuditsForDomainRecord(long id) {
		return audit_record_repo.getAllContentAuditsForDomainRecord(id);
	}

	public Set<Audit> getAllInformationArchitectureAuditsForDomainRecord(long id) {
		return audit_record_repo.getAllInformationArchitectureAuditsForDomainRecord(id);
	}

	public Set<Audit> getAllAccessibilityAuditsForDomainRecord(long id) {
		return audit_record_repo.getAllAccessibilityAuditsForDomainRecord(id);
	}

	public Set<Audit> getAllAestheticAuditsForDomainRecord(long id) {
		return audit_record_repo.getAllAestheticsAuditsForDomainRecord(id);
	}

	public Set<Audit> getAllContentAudits(long audit_record_id) {
		return audit_record_repo.getAllContentAudits(audit_record_id);
	}

	public Set<Audit> getAllInformationArchitectureAudits(long id) {
		return audit_record_repo.getAllInformationArchitectureAudits(id);
	}

	public Set<Audit> getAllAccessibilityAudits(Long id) {
		return audit_record_repo.getAllAccessibilityAudits(id);
	}

	public Set<Audit> getAllAestheticAudits(long id) {
		return audit_record_repo.getAllAestheticsAudits(id);
	}
	
	public Set<PageAuditRecord> getPageAuditRecords(long domain_audit_id) {
		return audit_record_repo.getPageAuditRecord(domain_audit_id);
	}

	public PageState getPageStateForAuditRecord(long page_audit_id) {
		return audit_record_repo.getPageStateForAuditRecord(page_audit_id);
	}

	public Set<UXIssueMessage> getIssues(long audit_record_id) {
		return audit_record_repo.getIssues(audit_record_id);
	}

	public Set<PageState> getPageStatesForDomainAuditRecord(long audit_record_id) {
		return audit_record_repo.getPageStatesForDomainAuditRecord(audit_record_id);
	}

	public void addPageToAuditRecord(long audit_record_id, long page_state_id) {
		audit_record_repo.addPageToAuditRecord( audit_record_id, page_state_id );		
	}
}
