package com.qanairy.models.audit.domain;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jsoup.nodes.Element;
import org.neo4j.ogm.annotation.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.qanairy.models.Domain;
import com.qanairy.models.ElementState;
import com.qanairy.models.audit.Audit;
import com.qanairy.models.audit.ElementStateObservation;
import com.qanairy.models.audit.Observation;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditLevel;
import com.qanairy.models.enums.AuditSubcategory;
import com.qanairy.services.AuditService;
import com.qanairy.services.DomainService;


/**
 * Responsible for executing an audit on the titles and headers on a page for the information architecture audit category
 */
@Component
public class DomainTitleAndHeaderAudit implements IExecutableDomainAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(DomainTitleAndHeaderAudit.class);

	@Relationship(type="FLAGGED")
	private List<Element> flagged_elements = new ArrayList<>();
	
	@Autowired
	private DomainService domain_service;

	@Autowired
	private AuditService audit_service;
	
	public DomainTitleAndHeaderAudit() {}

	/**
	 * {@inheritDoc}
	 * 
	 *  
	 * @throws MalformedURLException 
	 * @throws URISyntaxException 
	 */
	@Override
	public Audit execute(Domain domain) {
		assert domain != null;
		
		List<Observation> observations = new ArrayList<>();

		Set<Audit> title_and_header_audits = domain_service.getMostRecentAuditRecordTitleAndHeader(domain.getHost());
		int points = 0;
		int max_points = 0;
		
		for(Audit audit : title_and_header_audits) {
			points += audit.getPoints();
			max_points += audit.getTotalPossiblePoints();
			observations.addAll(audit_service.getObservations(audit.getKey()));
		}
		
		//merge observations
		Map<String, List<ElementState>> observation_map = new HashMap<>();
		for(Observation observation : observations) {
			if( observation instanceof ElementStateObservation){
				ElementStateObservation element_obs = (ElementStateObservation)observation;
				String description = element_obs.getDescription();
				if(observation_map.containsKey(description)) {
					List<ElementState> elements = observation_map.get(description);
					elements.addAll(element_obs.getElements());
				}
				else {
					observation_map.put(description, new ArrayList<>(element_obs.getElements()));
				}
			}
		}
		
		List<Observation> compressed_observations = new ArrayList<>();
		for(String key : observation_map.keySet()) {
			ElementStateObservation observation = new ElementStateObservation(observation_map.get(key), key);
			compressed_observations.add(observation);
		}
		
		
		String why_it_matters = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Etiam semper vel eros ut fermentum. Aenean scelerisque tincidunt arcu, in pulvinar enim dapibus et. Sed pharetra lorem et maximus cursus. Pellentesque aliquam ex quis sapien sollicitudin ornare. Curabitur sit amet metus eu mi interdum pretium. Praesent lobortis ligula id tortor finibus, non tincidunt nisi pretium. Fusce nisi justo, condimentum sed eros ut, volutpat tincidunt sem. Vestibulum vestibulum enim et viverra pharetra. Vivamus aliquam pulvinar facilisis. Quisque nec nisi est. Suspendisse quis scelerisque nulla. Integer sit amet egestas eros. Duis sagittis elit mi. Suspendisse diam sem, sodales et sodales a, posuere eget massa. Vestibulum ligula justo, ultrices vitae nisi nec, finibus posuere tortor.\n";
		
		String ada_compliance = "Nunc nulla odio, accumsan ac mauris quis, efficitur mattis sem. Maecenas mattis non urna nec malesuada. Nullam felis risus, interdum vel turpis non, elementum lobortis nulla. Sed laoreet sagittis maximus. Vestibulum ac sollicitudin lectus, vitae viverra arcu. Donec imperdiet sit amet lorem non tempor. Phasellus velit leo, vestibulum at justo ac, viverra scelerisque massa. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae; Morbi rutrum nunc et turpis facilisis gravida. Vivamus nec ipsum sed nunc efficitur mattis sed pulvinar metus. Morbi vitae nisi sit amet purus efficitur mattis. Class aptent taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos. Pellentesque accumsan, nisi eu dignissim convallis, elit libero dictum dui, eu euismod mauris dui nec odio.";
		
		return new Audit(AuditCategory.INFORMATION_ARCHITECTURE,
					     AuditSubcategory.TITLES,
					     points,
					     compressed_observations,
					     AuditLevel.DOMAIN,
					     max_points,
					     domain.getHost(),
					     why_it_matters,
					     ada_compliance,
					     new HashSet<>());
	}
}