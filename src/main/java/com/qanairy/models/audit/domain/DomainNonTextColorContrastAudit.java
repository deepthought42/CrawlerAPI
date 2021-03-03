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
import com.qanairy.models.enums.AuditName;
import com.qanairy.models.enums.AuditSubcategory;
import com.qanairy.services.AuditService;
import com.qanairy.services.DomainService;


/**
 * Compiles average score across domain using audits on the color constrast of non text elements against their parent element's background
 */
@Component
public class DomainNonTextColorContrastAudit implements IExecutableDomainAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(DomainNonTextColorContrastAudit.class);
	
	@Relationship(type="FLAGGED")
	private List<Element> flagged_elements = new ArrayList<>();
	
	@Autowired
	private DomainService domain_service;
	
	@Autowired
	private AuditService audit_service;
	
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
		List<Observation> observations = new ArrayList<>();
		
		//get all color palette audits associated with most recent audit record for domain host
		Set<Audit> text_contrast_audits = domain_service.getMostRecentAuditRecordNonTextColorContrast(domain.getHost());
		int points = 0;
		int max_points = 0;
		
		for(Audit audit : text_contrast_audits) {
			points += audit.getPoints();
			max_points += audit.getTotalPossiblePoints();
			observations.addAll(audit_service.getObservations(audit.getKey()));
		}
		
		String why_it_matters = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Etiam semper vel eros ut fermentum. Aenean scelerisque tincidunt arcu, in pulvinar enim dapibus et. Sed pharetra lorem et maximus cursus. Pellentesque aliquam ex quis sapien sollicitudin ornare. Curabitur sit amet metus eu mi interdum pretium. Praesent lobortis ligula id tortor finibus, non tincidunt nisi pretium. Fusce nisi justo, condimentum sed eros ut, volutpat tincidunt sem. Vestibulum vestibulum enim et viverra pharetra. Vivamus aliquam pulvinar facilisis. Quisque nec nisi est. Suspendisse quis scelerisque nulla. Integer sit amet egestas eros. Duis sagittis elit mi. Suspendisse diam sem, sodales et sodales a, posuere eget massa. Vestibulum ligula justo, ultrices vitae nisi nec, finibus posuere tortor.";
		
		String ada_compliance = "Nunc nulla odio, accumsan ac mauris quis, efficitur mattis sem. Maecenas mattis non urna nec malesuada. Nullam felis risus, interdum vel turpis non, elementum lobortis nulla. Sed laoreet sagittis maximus. Vestibulum ac sollicitudin lectus, vitae viverra arcu. Donec imperdiet sit amet lorem non tempor. Phasellus velit leo, vestibulum at justo ac, viverra scelerisque massa. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae; ";
		
		return new Audit(AuditCategory.AESTHETICS,
						 AuditSubcategory.COLOR_MANAGEMENT, 
						 AuditName.NON_TEXT_BACKGROUND_CONTRAST, 
					 	 points, 
						 observations, 
						 AuditLevel.DOMAIN, 
						 max_points, 
						 domain.getHost(),
						 why_it_matters,
						 ada_compliance);
	}
}