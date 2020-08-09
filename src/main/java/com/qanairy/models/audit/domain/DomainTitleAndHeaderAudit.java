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
import com.qanairy.models.ElementState;
import com.qanairy.models.audit.Audit;
import com.qanairy.models.audit.Observation;
import com.qanairy.models.audit.Score;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditLevel;
import com.qanairy.models.enums.AuditSubcategory;


/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
@Component
public class DomainTitleAndHeaderAudit implements IExecutableDomainAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(DomainTitleAndHeaderAudit.class);

	@Relationship(type="FLAGGED")
	private List<ElementState> flagged_elements = new ArrayList<>();
	
	public DomainTitleAndHeaderAudit() {}

	/**
	 * {@inheritDoc}
	 * 
	 * Identifies colors used on page, the color scheme type used, and the ultimately the score for how the colors used conform to scheme
	 *  
	 * @throws MalformedURLException 
	 * @throws URISyntaxException 
	 */
	@Override
	public Audit execute(Domain domain) {
		assert domain != null;

		Score score = scorePageTitles(domain);
		List<Observation> observations = new ArrayList<>();
		
		log.warn("TITLE FONT AUDIT SCORE   ::   "+score.getPointsAchieved() +" / " +score.getMaxPossiblePoints());
		return new Audit(AuditCategory.TYPOGRAPHY, AuditSubcategory.FONT, score.getPointsAchieved(), observations, AuditLevel.PAGE, score.getMaxPossiblePoints());
	}

	/**
	 * Generate a score for page titles across all pages in this domain
	 * @param domain
	 * @return
	 */
	private Score scorePageTitles(Domain domain) {
		assert domain != null;
		//find all pages for domain
		//for each page 
			//find most recent page state
			//score title of page state
		return null;
	}
}