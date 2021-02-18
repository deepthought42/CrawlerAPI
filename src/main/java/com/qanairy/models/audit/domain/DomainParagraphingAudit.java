package com.qanairy.models.audit.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.qanairy.models.Domain;
import com.qanairy.models.audit.Audit;
import com.qanairy.models.audit.Observation;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditLevel;
import com.qanairy.models.enums.AuditName;
import com.qanairy.services.AuditService;
import com.qanairy.services.DomainService;

/**
 * Responsible for executing an audit on the page audits for hyperlinks
 */
@Component
public class DomainParagraphingAudit implements IExecutableDomainAudit {

	@Autowired
	private AuditService audit_service;
	
	@Autowired
	private DomainService domain_service;
	
	public DomainParagraphingAudit() {	}

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

		Set<Audit> link_audits = domain_service.getMostRecentAuditRecordParagraphing(domain.getHost());
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
		
		String why_it_matters = "The way users experience content has changed in the mobile phone era." + 
				" Attention spans are shorter, and users skim through most information." + 
				" Presenting information in small, easy to digest chunks makes their" + 
				" experience easy and convenient. ";
		
		String ada_compliance = "Even though there are no ADA compliance requirements specifically for" + 
				" this category, reading level needs to be taken into consideration when" + 
				" writing content and paragraphing. ";
		
		return new Audit(AuditCategory.INFORMATION_ARCHITECTURE,
						 AuditName.LINKS,
						 points,
						 observations,
						 AuditLevel.DOMAIN,
						 max_points,
						 domain.getHost(),
						 why_it_matters,
						 ada_compliance,
						 new HashSet<>());
	}
}
