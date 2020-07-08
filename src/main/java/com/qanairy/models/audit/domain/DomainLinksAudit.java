package com.qanairy.models.audit.domain;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

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
	
	public DomainLinksAudit() {
		//super(buildBestPractices(), getAdaDescription(), getAuditDescription(), AuditSubcategory.LINKS);
	}
	
	private static String getAuditDescription() {
		return "A hyperlink that takes you to a new location should be reactive and result in the user navigating to an existing webpage";
	}

	private static List<String> buildBestPractices() {
		List<String> best_practices = new ArrayList<>();
		best_practices.add("Label should describe what the link is. \"Click here\" should not be used.");
		
		return best_practices;
	}
	
	private static String getAdaDescription() {
		return "\r\n" + 
				"2.4.4 - Descriptive Links\r\n" + 
				"The purpose of each link can be determined from the link text alone or from the link text together with its programmatically determined link context.\r\n" + 
				"\r\n" + 
				"2.4.7 - Visible Focus\r\n" + 
				"When an interactive element (link, button, form field, selectable element, etc.) receives focus, a visual indicator shows so a user can see what element they are currently on.";
	}
	
	
	/**
	 * {@inheritDoc}
	 * 
	 * Scores links across a whole site based on the link scores for the audits provided

	 * 
	 * @pre audits != null
	 */
	@Override
	public Audit execute(List<Audit> audits) {
		assert audits != null;
		double score = 0;
		for(Audit audit : audits) {
			score += audit.getScore();
		}
		score = score/audits.size();
		return new Audit(AuditCategory.INFORMATION_ARCHITECTURE, buildBestPractices(), getAdaDescription(), getAuditDescription(), AuditSubcategory.LINKS, score, new ArrayList<>(), AuditLevel.DOMAIN);
	}
}
