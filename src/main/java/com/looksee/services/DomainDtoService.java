package com.looksee.services;

import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.looksee.dto.DomainDto;
import com.looksee.models.Domain;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.DomainAuditRecord;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.utils.AuditUtils;

@Service
public class DomainDtoService {
	@Autowired
	private DomainService domain_service;
	
	@Autowired
	private AuditRecordService audit_record_service;
	
	/**
	 * 
	 * @param domain
	 * @return
	 */
	public DomainDto build(Domain domain) {
		Optional<DomainAuditRecord> audit_record_opt = domain_service.getMostRecentAuditRecord(domain.getId());

		int audited_pages = 0;
		int page_count = 0;
		if (!audit_record_opt.isPresent()) {
			return new DomainDto(domain.getId(), domain.getUrl(), 0, 0, 0, 0.0, 0, 0.0, 0, 0.0, 0, 0.0, false, 0.0);
		}
		// get most recent audit record for this domain
		DomainAuditRecord domain_audit = audit_record_opt.get();

		// get all content audits for most recent audit record and calculate overall
		// score
		Set<Audit> content_audits = audit_record_service.getAllContentAuditsForDomainRecord(domain_audit.getId());
		double content_score = AuditUtils.calculateScore(content_audits);

		// get all info architecture audits for most recent audit record and calculate
		// overall score
		Set<Audit> info_arch_audits = audit_record_service
				.getAllInformationArchitectureAuditsForDomainRecord(domain_audit.getId());
		double info_arch_score = AuditUtils.calculateScore(info_arch_audits);

		// get all accessibility audits for most recent audit record and calculate
		// overall score
		Set<Audit> accessibility_audits = audit_record_service
				.getAllAccessibilityAuditsForDomainRecord(domain_audit.getId());
		double accessibility_score = AuditUtils.calculateScore(accessibility_audits);

		// get all Aesthetic audits for most recent audit record and calculate overall
		// score
		Set<Audit> aesthetics_audits = audit_record_service
				.getAllAestheticAuditsForDomainRecord(domain_audit.getId());
		double aesthetics_score = AuditUtils.calculateScore(aesthetics_audits);

		// build domain stats
		// add domain stat to set

		// check if there is a current audit running
		AuditRecord audit_record = audit_record_opt.get();
		Set<PageAuditRecord> page_audit_records = audit_record_service.getAllPageAudits(audit_record.getId());
		page_count = page_audit_records.size();

		double content_progress = 0.0;
		double aesthetic_progress = 0.0;
		double info_architecture_progress = 0.0;
		boolean is_audit_running = false;
		double data_extraction_progress = 0.0;

		for (PageAuditRecord record : page_audit_records) {
			content_progress += record.getContentAuditProgress();
			aesthetic_progress += record.getAestheticAuditProgress();
			info_architecture_progress += record.getInfoArchAuditProgress();
			data_extraction_progress += record.getDataExtractionProgress();

			if (record.isComplete()) {
				audited_pages++;
			}
			else {
				is_audit_running = true;
			}
		}

		if (page_audit_records.size() > 0) {
			content_progress = content_progress / page_audit_records.size();
			info_architecture_progress = (info_architecture_progress / page_audit_records.size());
			aesthetic_progress = (aesthetic_progress / page_audit_records.size());
			data_extraction_progress = (data_extraction_progress / page_audit_records.size());
		}

		return new DomainDto(domain.getId(), 
							  domain.getUrl(), 
							  page_count, 
							  audited_pages, 
							  content_score,
							  content_progress, 
							  info_arch_score, 
							  info_architecture_progress, 
							  accessibility_score, 
							  100.0,
							  aesthetics_score, 
							  aesthetic_progress, 
							  is_audit_running, 
							  data_extraction_progress);
	}
}
