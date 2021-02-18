package com.qanairy.models.audit.domain;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import com.qanairy.models.enums.AuditName;
import com.qanairy.models.enums.AuditSubcategory;
import com.qanairy.services.AuditService;
import com.qanairy.services.DomainService;



/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
@Component
public class DomainTypefaceAudit implements IExecutableDomainAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(DomainTypefaceAudit.class);
	
	@Autowired
	private DomainService domain_service;
	
	@Autowired
	private AuditService audit_service;
	
	@Relationship(type="FLAGGED")
	private List<Element> flagged_elements = new ArrayList<>();
	
	public DomainTypefaceAudit() {	}

	/**
	 * {@inheritDoc} 

	 *  
	 * @throws MalformedURLException 
	 * @throws URISyntaxException 
	 */
	@Override
	public Audit execute(Domain domain) {
		assert domain != null;
		List<Observation> observations = new ArrayList<>();

		//get all color palette audits associated with most recent audit record for domain host
		Set<Audit> text_contrast_audits = domain_service.getMostRecentAuditRecordTypeface(domain.getHost());
		int points = 0;
		int max_points = 0;
		
		
		
		for(Audit audit : text_contrast_audits) {
			points += audit.getPoints();
			max_points += audit.getTotalPossiblePoints();
			observations.addAll(audit_service.getObservations(audit.getKey()));
		}
		
		//reduce observations to unique
		Map<String, Observation> observation_map = new HashMap<>();
		for(Observation obs : observations){
			if(!observation_map.containsKey(obs.getKey())) {
				observation_map.put(obs.getKey(), obs);
			}
		}
		
		String why_it_matters = "Clean typography, with the use of only 1 to 2 typefaces, invites users to" + 
				" the text on your website. It plays an important role in how clear, distinct" + 
				" and legible the textual content is.";
		
		String ada_compliance = "Your typography meets ADA requirements." + 
				" Images of text are not used and text is resizable. San-Serif typeface has" + 
				" been used across the pages.";
		
		return new Audit(AuditCategory.AESTHETICS,
						 AuditSubcategory.TYPOGRAPHY,
						 AuditName.TYPEFACES, 
						 points, 
						 new ArrayList<>(observation_map.values()), 
						 AuditLevel.DOMAIN, 
						 max_points, 
						 domain.getHost(),
						 why_it_matters,
						 ada_compliance,
						 new HashSet<>());
	}
	

	public static List<String> makeDistinct(List<String> from){
		assert from != null;
	    from.removeAll(Collections.singleton(null));

		return from.stream().distinct().collect(Collectors.toList());
	}
	
}