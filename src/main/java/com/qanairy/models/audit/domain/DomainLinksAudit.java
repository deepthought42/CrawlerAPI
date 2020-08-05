package com.qanairy.models.audit.domain;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.qanairy.models.Domain;
import com.qanairy.models.ElementState;
import com.qanairy.models.audit.Audit;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditLevel;
import com.qanairy.models.enums.AuditSubcategory;

/**
 * Responsible for executing an audit on the page audits for hyperlinks
 */
@Component
public class DomainLinksAudit implements IExecutableDomainAudit {
	
	private List<ElementState> links_without_href =  new ArrayList<>();
	private List<ElementState> invalid_links = new ArrayList<>();
	private List<ElementState> dead_links = new ArrayList<>();
	
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
		int score = 0;
		List<Audit> audits = new ArrayList<>();
		
		for(Audit audit : audits) {
			score += audit.getPoints();
		}
		return new Audit(AuditCategory.INFORMATION_ARCHITECTURE, AuditSubcategory.LINKS, score, new ArrayList<>(), AuditLevel.DOMAIN, audits.size());
	}
}
