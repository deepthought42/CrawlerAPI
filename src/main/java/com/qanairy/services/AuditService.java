package com.qanairy.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.util.IterableUtils;
import org.springframework.stereotype.Service;

import com.qanairy.models.audit.Audit;
import com.qanairy.models.audit.Observation;
import com.qanairy.models.repository.AuditRepository;

/**
 * Contains business logic for interacting with and managing audits
 *
 */
@Service
public class AuditService {
	private static Logger log = LoggerFactory.getLogger(AuditService.class);

	@Autowired
	private AuditRepository audit_repo;

	public Audit save(Audit audit) {
		assert audit != null;
		return audit_repo.save(audit);
	}

	public Optional<Audit> findById(long id) {
		return audit_repo.findById(id);
	}
	
	public Audit findByKey(String key) {
		return audit_repo.findByKey(key);
	}

	public List<Audit> saveAll(List<Audit> audits) {
		assert audits != null;
		
		List<Audit> audits_saved = new ArrayList<Audit>();
		
		for(Audit audit : audits) {
			if(audit == null) {
				continue;
			}
			
			Audit audit_record = audit_repo.findByKey(audit.getKey());
			if(audit_record != null) {
				log.warn("audit already exists!!!");
				audits_saved.add(audit_record);
				continue;
			}
			log.warn("------------------------------------------------------------------------------");
			log.warn("saving audit ;;: "+audit);
			log.warn("Audit key :: "+audit.getKey());
			log.warn(" :: "+audit.getPoints());
			log.warn(" :: "+audit.getTotalPossiblePoints());
			log.warn(" :: "+audit.getCategory());
			log.warn(" :: "+audit.getCreatedAt());
			log.warn(" :: "+audit.getLevel());
			log.warn(" :: "+audit.getObservations());
			for(Observation observation : audit.getObservations()) {
				log.warn(" observation description :  "+observation.getDescription());
				log.warn(" observation type :  "+observation.getType());
			}
			log.warn("Subcategory  :: "+audit.getSubcategory());
			
			log.warn("saving using audit repo :: " + audit_repo);
			try {
				Audit saved_audit = audit_repo.save(audit);
				audits_saved.add(saved_audit);
			}catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		return audits_saved;
	}

	public List<Audit> findAll() {
		// TODO Auto-generated method stub
		return IterableUtils.toList(audit_repo.findAll());
	}

	public List<Observation> getObservations(String audit_key) {
		assert audit_key != null;
		assert !audit_key.isEmpty();
		log.warn("LOADING OBSERVATIONS FOR AUDIT WITH KEY :: " + audit_key);
		return audit_repo.findObservationsForAudit(audit_key);
	}
}
