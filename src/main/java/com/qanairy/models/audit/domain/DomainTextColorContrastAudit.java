package com.qanairy.models.audit.domain;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.neo4j.ogm.annotation.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.qanairy.models.Domain;
import com.qanairy.models.Element;
import com.qanairy.models.audit.Audit;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditLevel;
import com.qanairy.models.enums.AuditSubcategory;


/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
@Component
public class DomainTextColorContrastAudit implements IExecutableDomainAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(DomainTextColorContrastAudit.class);
	
	@Relationship(type="FLAGGED")
	List<Element> flagged_elements = new ArrayList<>();
	
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

		int score = 0;
		List<Audit> audits = new ArrayList<>();
		for(Audit audit : audits) {
			score += audit.getPoints();
		}
		
		return new Audit(AuditCategory.COLOR_MANAGEMENT, AuditSubcategory.TEXT_BACKGROUND_CONTRAST, score, new ArrayList<>(), AuditLevel.DOMAIN, audits.size());
	}
}