package com.qanairy.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qanairy.models.audit.Audit;
import com.qanairy.models.repository.AuditRepository;

/**
 * Contains business logic for interacting with and managing audits
 *
 */
@Service
public class AuditService {

	@Autowired
	private AuditRepository audit_repo;

	public Audit save(Audit acct) {
		return audit_repo.save(acct);
	}

	public Optional<Audit> findById(long id) {
		return audit_repo.findById(id);
	}
	
	public Audit findByKey(String key) {
		return audit_repo.findByKey(key);
	}

	public List<Audit> saveAll(List<Audit> audits) {
		List<Audit> audits_saved = new ArrayList<Audit>();
		
		for(Audit audit : audits) {
			audits_saved.add(audit_repo.save(audit));
		}
		
		return audits_saved;
	}
}
