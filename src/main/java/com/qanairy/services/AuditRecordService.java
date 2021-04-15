           package com.qanairy.services;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.collections4.IterableUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qanairy.models.PageState;
import com.qanairy.models.audit.Audit;
import com.qanairy.models.audit.AuditRecord;
import com.qanairy.models.audit.DomainAuditRecord;
import com.qanairy.models.audit.PageAuditRecord;
import com.qanairy.models.repository.AuditRecordRepository;

/**
 * Contains business logic for interacting with and managing audits
 *
 */
@Service
public class AuditRecordService {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(AuditRecordService.class);

	@Autowired
	private AuditRecordRepository audit_record_repo;

	public AuditRecord save(AuditRecord audit) {
		assert audit != null;
		
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

	public Set<Audit> getAllAudits(String audit_record_key) {
		assert audit_record_key != null;
		assert !audit_record_key.isEmpty();
		
		return audit_record_repo.getAllAudits(audit_record_key);
	}

	public Optional<DomainAuditRecord> findMostRecentDomainAuditRecord(String host) {
		assert host != null;
		assert !host.isEmpty();
		
		return audit_record_repo.findMostRecentDomainAuditRecord(host);
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

	public Set<PageAuditRecord> getAllPageAudits(String audit_record_key) {
		assert audit_record_key != null;
		assert !audit_record_key.isEmpty();
		
		return audit_record_repo.getAllPageAudits(audit_record_key);
	}

	public Set<Audit> getAllAuditsForPageAuditRecord(String page_audit_key) {
		assert page_audit_key != null;
		assert !page_audit_key.isEmpty();
		
		return audit_record_repo.getAllAuditsForPageAuditRecord( page_audit_key);
	}

	public void addPageAuditToDomainAudit(String domain_audit_record_key, String page_audit_record_key) {
		//check if audit already exists for page state
		audit_record_repo.addPageAuditRecord(domain_audit_record_key, page_audit_record_key);
	}

	public Optional<PageAuditRecord> getMostRecentPageAuditRecord(String url) {
		assert url != null;
		assert !url.isEmpty();
		
		return audit_record_repo.getMostRecentPageAuditRecord(url);
	}

	public PageState getPageStateForAuditRecord(String page_audit_key) {
		assert page_audit_key != null;
		assert !page_audit_key.isEmpty();
		
		return audit_record_repo.getPageStateForAuditRecord(page_audit_key);
	}
}
