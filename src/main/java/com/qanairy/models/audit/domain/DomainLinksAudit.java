package com.qanairy.models.audit.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.qanairy.models.Domain;
import com.qanairy.models.audit.Audit;
import com.qanairy.models.audit.Observation;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditLevel;
import com.qanairy.models.enums.AuditSubcategory;
import com.qanairy.services.AuditService;
import com.qanairy.services.DomainService;

/**
 * Responsible for executing an audit on the page audits for hyperlinks
 */
@Component
public class DomainLinksAudit implements IExecutableDomainAudit {

	@Autowired
	private AuditService audit_service;
	
	@Autowired
	private DomainService domain_service;
	
	public DomainLinksAudit() {	}

	/**
	 * {@inheritDoc}
	 * 
	 * Scores links across a whole site based on the link scores for the audits provided

	 * 
	 * @pre audits != null
	 */
	@Override
	public Audit execute(Domain domain) {
		assert domain != null;
		
		List<Observation> observations = new ArrayList<>();

		Set<Audit> link_audits = domain_service.getMostRecentAuditRecordLinks(domain.getHost());
		int points = 0;
		int max_points = 0;
		System.out.println("loaded page level LINK audits :: "+link_audits.size());
		for(Audit audit : link_audits) {
			points += audit.getPoints();
			max_points += audit.getTotalPossiblePoints();
			System.out.println("Obtaining observations from NEO4J FOR AUDIT WITH KEY ;:::  "+audit.getKey());
			observations.addAll(audit_service.getObservations(audit.getKey()));
			System.out.println("observations size for domain link audit ::      "+observations.size());
		}
		
		return new Audit(AuditCategory.INFORMATION_ARCHITECTURE, 
						 AuditSubcategory.LINKS, 
						 points, 
						 observations, 
						 AuditLevel.PAGE, 
						 max_points,
						 domain.getHost());
	}
}
