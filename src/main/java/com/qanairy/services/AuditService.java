package com.qanairy.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.util.IterableUtils;
import org.springframework.stereotype.Service;

import com.qanairy.models.Element;
import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.PageStateAudits;
import com.qanairy.models.SimpleElement;
import com.qanairy.models.audit.Audit;
import com.qanairy.models.audit.AuditElementMap;
import com.qanairy.models.audit.ElementObservation;
import com.qanairy.models.audit.ElementStateObservation;
import com.qanairy.models.audit.Observation;
import com.qanairy.models.enums.ObservationType;
import com.qanairy.models.repository.AuditRepository;
import com.qanairy.models.repository.PageStateRepository;

/**
 * Contains business logic for interacting with and managing audits
 *
 */
@Service
public class AuditService {
	private static Logger log = LoggerFactory.getLogger(AuditService.class);

	@Autowired
	private AuditRepository audit_repo;
	
	@Autowired
	private PageStateService page_state_service;

	public Audit save(Audit audit) {
		assert audit != null;
		return audit_repo.save(audit);
	}

	public Optional<Audit> findById(long id) {
		return audit_repo.findById(id);
	}
	
	public Audit findByKey(String key) {
		return audit_repo.findByKey(key);
	}

	public List<Audit> saveAll(List<Audit> audits) {
		assert audits != null;
		
		List<Audit> audits_saved = new ArrayList<Audit>();
		
		for(Audit audit : audits) {
			if(audit == null) {
				continue;
			}
			
			Audit audit_record = audit_repo.findByKey(audit.getKey());
			if(audit_record != null) {
				log.warn("audit already exists!!!");
				audits_saved.add(audit_record);
				continue;
			}
			log.warn("------------------------------------------------------------------------------");
			log.warn("saving audit ;;: "+audit);
			log.warn("Audit key :: "+audit.getKey());
			log.warn("points :: "+audit.getPoints() + " / " + audit.getTotalPossiblePoints());
			log.warn(" :: "+audit.getCategory());
			log.warn(" :: "+audit.getLevel());
			log.warn(" :: "+audit.getObservations());
			for(Observation observation : audit.getObservations()) {
				log.warn(" observation description :  "+observation.getDescription());
				log.warn(" observation type :  "+observation.getType());
			}
			log.warn("Subcategory  :: "+audit.getName());
			
			try {
				Audit saved_audit = audit_repo.save(audit);
				audits_saved.add(saved_audit);
			}catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		return audits_saved;
	}

	public List<Audit> findAll() {
		// TODO Auto-generated method stub
		return IterableUtils.toList(audit_repo.findAll());
	}

	public List<Observation> getObservations(String audit_key) {
		assert audit_key != null;
		assert !audit_key.isEmpty();
		return audit_repo.findObservationsForAudit(audit_key);
	}

	/**
	 * using a list of audits, sorts the list by page and packages results into list 
	 * 	of {@linkplain PageStateAudits}
	 * 
	 * @param audits
	 * @return
	 */
	public List<PageStateAudits> groupAuditsByPage(Set<Audit> audits) {
		Map<String, Set<Audit>> audit_url_map = new HashMap<>();
		
		log.warn("audit size :: "+audits.size());
		for(Audit audit : audits) {
			//if url of pagestate already exists 
			if(audit_url_map.containsKey(audit.getUrl())) {
				
				audit_url_map.get(audit.getUrl()).add(audit);
			}
			else {
				Set<Audit> page_audits = new HashSet<>();
				page_audits.add(audit);
				
				audit_url_map.put(audit.getUrl(), page_audits);
			}
		}
		
		log.warn("total pages :: " + audit_url_map.size());
		List<PageStateAudits> page_audits = new ArrayList<>();
		for(String url : audit_url_map.keySet()) {
			//load page state by url
			PageState page_state = page_state_service.findByUrl(url);
			PageStateAudits page_state_audits = new PageStateAudits(page_state.getUrl(), page_state.getViewportScreenshotUrl(), page_state.getFullPageScreenshotUrl(), audit_url_map.get(url));
			page_audits.add( page_state_audits ) ;
		}
		
		log.warn("page audits :: "+page_audits.size());
		return page_audits;
	}

	public List<AuditElementMap> generateAuditElementMap(Set<Audit> audits) {
		List<AuditElementMap> audit_elements = new ArrayList<>();
		
		for(Audit audit : audits) {
			Set<SimpleElement> elements = new HashSet<>();
			
			for(Observation observation : audit.getObservations()) {
				if(observation.getType().equals(ObservationType.ELEMENT)) {
					List<ElementState> element_states = ((ElementStateObservation)observation).getElements();
					for(ElementState element : element_states) {
						elements.add(new SimpleElement(element.getScreenshotUrl(), 
													   element.getXLocation(), 
													   element.getYLocation(), 
													   element.getWidth(), 
													   element.getHeight()));
					}
				}
			}
			
			Map<Audit, Set<SimpleElement>> audit_element_map = new HashMap<>();
			audit_element_map.put(audit, elements);
			audit_elements.add(new AuditElementMap(audit_element_map));
		}
		
		return audit_elements;
	}
}
