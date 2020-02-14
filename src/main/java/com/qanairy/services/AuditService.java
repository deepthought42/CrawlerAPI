package com.qanairy.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qanairy.models.experience.Audit;
import com.qanairy.models.experience.AuditDetail;
import com.qanairy.models.repository.AuditDetailRepository;
import com.qanairy.models.repository.AuditRepository;

@Service
public class AuditService {

	@Autowired
	private AuditRepository audit_repo;
	
	@Autowired
	private AuditDetailRepository audit_detail_repo;
	
	/**
	 * Objects are expected to be immutable as of 3/14/19. When this method is ran, if the 
	 * object already exists then it will be loaded from the database by key, otherwise it will be saved
	 * 
	 * @param audit {@link Audit} 
	 * @return
	 */
	public Audit save(Audit audit){
		List<AuditDetail> audit_details = new ArrayList<AuditDetail>();
		
		//save audit details
		for(AuditDetail detail : audit.getDetails()) {
			audit_details.add(audit_detail_repo.save(detail));
		}
		
		audit.setDetails(audit_details);
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
