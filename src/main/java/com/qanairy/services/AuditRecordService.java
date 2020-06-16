package com.qanairy.services;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.Domain;
import com.qanairy.models.TestRecord;
import com.qanairy.models.audit.Audit;
import com.qanairy.models.audit.AuditRecord;
import com.qanairy.models.repository.AuditRecordRepository;
import com.qanairy.models.repository.AuditRepository;

/**
 * Contains business logic for interacting with and managing audits
 *
 */
@Service
public class AuditRecordService {

	@Autowired
	private AuditRecordRepository audit_record_repo;

	public AuditRecord save(AuditRecord audit_record) {
		return audit_record_repo.save(audit_record);
	}

	public Optional<AuditRecord> findById(long id) {
		return audit_record_repo.findById(id);
	}
	
	public AuditRecord findByKey(String key) {
		return audit_record_repo.findByKey(key);
	}
}
