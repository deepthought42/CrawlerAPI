package com.qanairy.models.audit.domain;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.neo4j.ogm.annotation.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.qanairy.models.Domain;
import com.qanairy.models.Element;
import com.qanairy.models.audit.Audit;
import com.qanairy.models.audit.Observation;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditLevel;
import com.qanairy.models.enums.AuditSubcategory;
import com.qanairy.services.AuditService;
import com.qanairy.services.DomainService;


/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
@Component
public class DomainTextColorContrastAudit implements IExecutableDomainAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(DomainTextColorContrastAudit.class);
	
	@Relationship(type="FLAGGED")
	private List<Element> flagged_elements = new ArrayList<>();
	
	@Autowired
	private DomainService domain_service;
	
	@Autowired
	private AuditService audit_service;
	
	public DomainTextColorContrastAudit() {}
	
	/**
	 * {@inheritDoc}
	 * 
	 * Scores contrast of text with background across all audits
	 *  
	 * @throws MalformedURLException
	 * @throws URISyntaxException 
	 */
	@Override
	public Audit execute(Domain domain) {
		assert domain != null;

		List<Observation> observations = new ArrayList<>();
		
		//get all color palette audits associated with most recent audit record for domain host
		Set<Audit> text_contrast_audits = domain_service.getMostRecentAuditRecordTextColorContrast(domain.getHost());
		int points = 0;
		int max_points = 0;
		
		for(Audit audit : text_contrast_audits) {
			points += audit.getPoints();
			max_points += audit.getTotalPossiblePoints();
			observations.addAll(audit_service.getObservations(audit.getKey()));
		}
		
		return new Audit(AuditCategory.COLOR_MANAGEMENT, 
						 AuditSubcategory.TEXT_BACKGROUND_CONTRAST, 
						 points, 
						 observations, 
						 AuditLevel.DOMAIN, 
						 max_points, domain.getHost());
	}
}