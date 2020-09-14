package com.qanairy.services;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.validation.constraints.NotBlank;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.util.IterableUtils;
import org.springframework.stereotype.Service;

import com.qanairy.models.audit.Audit;
import com.qanairy.models.audit.AuditRecord;
import com.qanairy.models.repository.AuditRecordRepository;

/**
 * Contains business logic for interacting with and managing audits
 *
 */
@Service
public class AuditRecordService {
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
		Optional<Audit> audit = audit_record_repo.getAuditForPageState(audit_record_key, audit_key);
		if(!audit.isPresent()) {
			audit_record_repo.addAudit(audit_record_key, audit_key);
		}
	}

	public Set<Audit> getAllAudits(@NotBlank String audit_record_key) {
		return audit_record_repo.getAllAudits(audit_record_key);
	}

	public Optional<AuditRecord> findMostRecent(@NotBlank String domain_url) {
		return audit_record_repo.findMostRecent(domain_url);
	}

	public Set<Audit> getAllColorManagementAudits(String domain_url) {
        AuditRecord record = findMostRecent(domain_url).get();

		return audit_record_repo.getAllColorManagementAudits(record.getKey());
	}
}
