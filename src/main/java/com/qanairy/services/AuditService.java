package com.qanairy.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.minion.actors.DiscoveryActor;
import com.qanairy.models.experience.AccessibilityAudit;
import com.qanairy.models.experience.Audit;
import com.qanairy.models.repository.AuditRepository;

@Service
public class AuditService {
	private static Logger log = LoggerFactory.getLogger(AuditService.class.getName());

	@Autowired
	private AuditRepository audit_repo;
	
	/**
	 * Objects are expected to be immutable as of 3/14/19. When this method is ran, if the 
	 * object already exists then it will be loaded from the database by key, otherwise it will be saved
	 * 
	 * @param audit {@link AccessibilityAudit} 
	 * @return
	 */
	public AccessibilityAudit save(AccessibilityAudit audit){
		return audit_repo.save(audit);
	}
	
	/**
	 * Objects are expected to be immutable as of 3/14/19. When this method is ran, if the 
	 * object already exists then it will be loaded from the database by key, otherwise it will be saved
	 * 
	 * @param audit {@link Audit} 
	 * @return
	 */
	public Audit save(Audit audit){
		return audit_repo.save(audit);
	}

	/**
	 * Retrieve data from database
	 * 
	 * @param key
	 * @return
	 */
	public Audit findByKey(String key) {
		return audit_repo.findByKey(key);
	}
}
