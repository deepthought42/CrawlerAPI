package com.qanairy.models.audit.domain;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.neo4j.ogm.annotation.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qanairy.models.ElementState;
import com.qanairy.models.audit.Audit;
import com.qanairy.models.enums.AuditSubcategory;


/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
public class DomainTextColorContrastAudit extends DomainColorManagementAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(DomainTextColorContrastAudit.class);
	
	@Relationship(type="FLAGGED")
	List<ElementState> flagged_elements = new ArrayList<>();
	
	public DomainTextColorContrastAudit() {
		super(buildBestPractices(), getAdaDescription(), getAuditDescription(), AuditSubcategory.TEXT_BACKGROUND_CONTRAST);
	}
	
	private static String getAuditDescription() {
		return "Color contrast between background and text.";
	}

	private static List<String> buildBestPractices() {
		List<String> best_practices = new ArrayList<>();
		best_practices.add("According to the WCAG, \r\n" + 
				"Text: Contrast of 4.5 - 7 with the background. \r\n" + 
				"Large text/ Headlines: Contrast of 3 - 4.5 with the background. \r\n" + 
				"Black on white or vice versa is not recommended.");
		
		return best_practices;
	}
	
	private static String getAdaDescription() {
		return "1.4.1 - Use of Color \r\n" + 
				"Color is not used as the only visual means of conveying information, indicating an action, prompting a response, or distinguishing a visual element.\r\n";
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * Scores contrast of text with background across all audits
	 *  
	 * @throws MalformedURLException 
	 * @throws URISyntaxException 
	 */
	@Override
	public double execute(List<Audit> audits) {
		assert audits != null;

		double score = 0.0;
		for(Audit audit : audits) {
			score += audit.getScore();
		}
		
		setScore(score/audits.size());
		return getScore();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DomainTextColorContrastAudit clone() {
		DomainTextColorContrastAudit audit = new DomainTextColorContrastAudit();
		audit.setScore(getScore());
		audit.setKey(getKey());
		return audit;
	}

}