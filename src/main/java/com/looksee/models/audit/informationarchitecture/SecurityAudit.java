package com.looksee.models.audit.informationarchitecture;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.neo4j.ogm.annotation.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.looksee.models.Element;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.IExecutablePageStateAudit;
import com.looksee.models.audit.UXIssueMessage;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.AuditSubcategory;
import com.looksee.models.enums.ObservationType;
import com.looksee.models.enums.Priority;


/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
@Component
public class SecurityAudit implements IExecutablePageStateAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(SecurityAudit.class);
	
	@Relationship(type="FLAGGED")
	List<Element> flagged_elements = new ArrayList<>();
	
	public SecurityAudit() {
		//super(buildBestPractices(), getAdaDescription(), getAuditDescription(), AuditSubcategory.TEXT_BACKGROUND_CONTRAST);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Identifies colors used on page, the color scheme type used, and the ultimately the score for how the colors used conform to scheme
	 *  
	 * @throws MalformedURLException 
	 * @throws URISyntaxException 
	 */
	@Override
	public Audit execute(PageState page_state, AuditRecord audit_record) {
		assert page_state != null;
		Set<UXIssueMessage> issue_messages = new HashSet<>();
		int score = 0;
		int max_score = 0;
		String why_it_matters = "Sites that don't use HTTPS are highly insecure and are more likley to leak personal identifiable information(PII). Modern users are keenly aware of this fact and are less likely to trust sites that aren't secured.";

		boolean is_secure = page_state.isSecure();
		if(!is_secure) {
				String title = "Page isn't secure";
				String description = page_state.getUrl() + " doesn't use https";
				String wcag_compliance = "";
				String recommendation = "Enable encryption(SSL) for your site by getting a signed certificate from a certificate authority and enabling ssl on the server that hosts your website.";
				UXIssueMessage ux_issue = new UXIssueMessage(
												recommendation,
												Priority.HIGH,
												description,
												ObservationType.SECURITY,
												AuditCategory.INFORMATION_ARCHITECTURE,
												wcag_compliance,
												new HashSet<>(),
												why_it_matters,
												title, 
												0, 
												1);
				issue_messages.add(ux_issue);
		}
		else {
			score++;
		}
		max_score++;
		
		String description = "";
		
		log.warn("FONT AUDIT SCORE   ::   "+(score) +" / " +(max_score));
		return new Audit(AuditCategory.INFORMATION_ARCHITECTURE,
						 AuditSubcategory.SECURITY,
						 AuditName.FONT,
						 score,
						 issue_messages,
						 AuditLevel.PAGE,
						 max_score,
						 page_state.getUrl(), 
						 why_it_matters, 
						 description,
						 page_state,
						 false);
	}
	

	public static List<String> makeDistinct(List<String> from){
		return from.stream().distinct().sorted().collect(Collectors.toList());
	}
	
}