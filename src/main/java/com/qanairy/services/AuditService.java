package com.qanairy.services;

import java.net.MalformedURLException;
import java.net.URL;
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

import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.PageStateAudits;
import com.qanairy.models.SimpleElement;
import com.qanairy.models.SimplePage;
import com.qanairy.models.audit.Audit;
import com.qanairy.models.audit.ElementObservationMap;
import com.qanairy.models.audit.ObservationElementMap;
import com.qanairy.models.audit.SimpleObservation;
import com.qanairy.models.audit.ElementStateObservation;
import com.qanairy.models.audit.Observation;
import com.qanairy.models.enums.ObservationType;
import com.qanairy.models.repository.AuditRepository;

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
			SimplePage simple_page = new SimplePage(page_state.getUrl(), page_state.getViewportScreenshotUrl(), page_state.getFullPageScreenshotUrl(), page_state.getFullPageWidth(), page_state.getFullPageHeight());
			PageStateAudits page_state_audits = new PageStateAudits(simple_page, audit_url_map.get(url));
			page_audits.add( page_state_audits ) ;
		}
		
		log.warn("page audits :: "+page_audits.size());
		return page_audits;
	}

	/**
	 * Creates a {@link List} of {@linkplain ObservationElementMap} objects based on a {@link Set} of {@link Audit audits}
	 * @param audits
	 * @param page_url
	 * @return
	 * @throws MalformedURLException
	 */
	public Set<ObservationElementMap> generateObservationElementMap(
			Set<Audit> audits, String page_url
	) throws MalformedURLException {
		Set<ObservationElementMap> audit_elements = new HashSet<>();
		
		for(Audit audit : audits) {
			log.warn("checking if "+audit.getUrl()+"   matches  "+page_url);
			URL url = new URL(audit.getUrl());
			URL page_url_obj = new URL(page_url);
			
			if(url.getHost().contentEquals(page_url_obj.getHost()) 
					&& url.getPath().contentEquals(page_url_obj.getPath())
			) {
				log.warn("found audit for page :: "+page_url);

				for(Observation observation : audit.getObservations()) {
					Set<SimpleElement> elements = new HashSet<>();

					if(observation.getType().equals(ObservationType.ELEMENT)) {
						List<ElementState> element_states = ((ElementStateObservation)observation).getElements();
						for(ElementState element : element_states) {
							elements.add(new SimpleElement(element.getKey(),
														   element.getScreenshotUrl(), 
														   element.getXLocation(), 
														   element.getYLocation(), 
														   element.getWidth(), 
														   element.getHeight(),
														   element.getXpath()));
						}
					}
					
					ObservationElementMap observation_element = null;
					if(observation.getType().equals(ObservationType.ELEMENT)) {
						SimpleObservation simple_observation = new SimpleObservation(
																		observation.getDescription(),
																		observation.getWhyItMatters(),
																		observation.getAdaCompliance(),
																		observation.getPriority(),
																		observation.getKey(),
																		observation.getRecommendations());
						observation_element = new ObservationElementMap(simple_observation, elements);
					}
					else{
						observation_element = new ObservationElementMap(observation, elements);
					}
					audit_elements.add(observation_element);
				}
			
				
			}
		}
		
		return audit_elements;
	}
	
	/**
	 * WIP
	 * 
	 * @param audits
	 * @param page_url
	 * @return
	 * @throws MalformedURLException
	 */
	public Set<ElementObservationMap> generateElementObservationMap(Set<Audit> audits, String page_url) throws MalformedURLException {
		Set<ElementObservationMap> element_observations = new HashSet<>();
		
		Map<String, Set<Observation>> observation_map = new HashMap<>(); 
		Map<String, SimpleElement> element_state_map = new HashMap<>();
		for(Audit audit : audits) {
			log.warn("checking if "+audit.getUrl()+"   matches  "+page_url);
			URL url = new URL(audit.getUrl());
			URL page_url_obj = new URL(page_url);
			
			if(url.getHost().contentEquals(page_url_obj.getHost()) 
					&& url.getPath().contentEquals(page_url_obj.getPath())
			) {
				log.warn("found audit for page :: "+page_url);

				for(Observation observation : audit.getObservations()) {
					if(observation.getType().equals(ObservationType.ELEMENT)) {
						List<ElementState> element_states = ((ElementStateObservation)observation).getElements();
						
						for(ElementState element : element_states) {
							if(!element_state_map.containsKey(element.getKey())) {
								SimpleElement simple_element = 	new SimpleElement(element.getKey(),
																				  element.getScreenshotUrl(), 
																				  element.getXLocation(), 
																				  element.getYLocation(), 
																				  element.getWidth(), 
																				  element.getHeight(),
																				  element.getXpath());
								element_state_map.put(element.getKey(), simple_element);
							}
							
							
							if(observation.getType().equals(ObservationType.ELEMENT)) {
								observation = new SimpleObservation(
										observation.getDescription(),
										observation.getWhyItMatters(),
										observation.getAdaCompliance(),
										observation.getPriority(),
										observation.getKey(),
										observation.getRecommendations());
							}

							//associate observation with element
							if(observation_map.containsKey(element.getKey())) {
								observation_map.get(element.getKey()).add(observation);
							}
							else {
								Set<Observation> observations = new HashSet<>();
								observations.add(observation);
								observation_map.put(element.getKey(), observations);
							}
						}
					}
				}
			
				
			}
		}
		
		//associate simple elements and observations
		for(String element_key : element_state_map.keySet()) {
			
			ElementObservationMap observation_element = new ElementObservationMap(observation_map.get(element_key), element_state_map.get(element_key));
			element_observations.add(observation_element);
		}
		
		return element_observations;
	}
}
