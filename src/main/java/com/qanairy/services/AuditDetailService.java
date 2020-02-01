package com.qanairy.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qanairy.models.experience.Audit;
import com.qanairy.models.experience.AuditDetail;
import com.qanairy.models.repository.AuditDetailRepository;

/**
 * 
 */
@Service
public class AuditDetailService {
	private static Logger log = LoggerFactory.getLogger(AuditDetailService.class.getName());

	@Autowired
	private AuditDetailRepository audit_detail_repo;
	
	/**
	 * Objects are expected to be immutable as of 3/14/19. When this method is ran, if the 
	 * object already exists then it will be loaded from the database by key, otherwise it will be saved
	 * 
	 * @param audit {@link Audit} 
	 * @return
	 */
	public AuditDetail save(AuditDetail audit_detail){
		return audit_detail_repo.save( audit_detail );
	}
}
