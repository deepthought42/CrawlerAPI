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
import com.qanairy.models.enums.AuditName;
import com.qanairy.models.enums.AuditSubcategory;
import com.qanairy.services.AuditService;
import com.qanairy.services.DomainService;

/**
 * Responsible for executing an audit on the page audits for hyperlinks
 */
@Component
public class DomainImageAltTextAudit implements IExecutableDomainAudit {

	@Autowired
	private AuditService audit_service;
	
	@Autowired
	private DomainService domain_service;
	
	public DomainImageAltTextAudit() {	}

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

		Set<Audit> link_audits = domain_service.getMostRecentAuditRecordAltText(domain.getHost());
		int points = 0;
		int max_points = 0;
		
		for(Audit audit : link_audits) {
			points += audit.getPoints();
			max_points += audit.getTotalPossiblePoints();
			observations.addAll(audit_service.getObservations(audit.getKey()));
		}		
		
		String why_it_matters = "Alt-text helps with both SEO and accessibility. Search engines use alt-text"
				+ "to help determine how usable and your site is as a way of ranking your site.";
		
		String ada_compliance = "Your website does not meet the level A ADA compliance requirement for" + 
				"‘Alt’ text for images present on the website.";
		
		return new Audit(AuditCategory.CONTENT,
						 AuditSubcategory.IMAGERY,
						 AuditName.ALT_TEXT, 
						 points, 
						 observations, 
						 AuditLevel.DOMAIN, 
						 max_points, 
						 null,
						 why_it_matters,
						 ada_compliance);
	}
}
