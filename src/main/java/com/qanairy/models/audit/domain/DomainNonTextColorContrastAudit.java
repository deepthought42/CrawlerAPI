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
 * Compiles average score across domain using audits on the color constrast of non text elements against their parent element's background
 */
@Component
public class DomainNonTextColorContrastAudit implements IExecutableDomainAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(DomainNonTextColorContrastAudit.class);
	
	@Relationship(type="FLAGGED")
	List<Element> flagged_elements = new ArrayList<>();
	
	public DomainNonTextColorContrastAudit() {}

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
		
		return new Audit(AuditCategory.COLOR_MANAGEMENT, AuditSubcategory.NON_TEXT_BACKGROUND_CONTRAST, score, new ArrayList<>(), AuditLevel.DOMAIN, audits.size());
	}
}