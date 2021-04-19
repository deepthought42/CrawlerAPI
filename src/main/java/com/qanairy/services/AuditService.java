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

import org.apache.commons.collections4.IterableUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.PageStateAudits;
import com.qanairy.models.SimpleElement;
import com.qanairy.models.SimplePage;
import com.qanairy.models.audit.Audit;
import com.qanairy.models.audit.ElementIssueMap;
import com.qanairy.models.audit.IssueElementMap;
import com.qanairy.models.audit.UXIssueMessage;
import com.qanairy.models.audit.ElementStateIssueMessage;
import com.qanairy.models.enums.ObservationType;
import com.qanairy.models.repository.AuditRepository;
import com.qanairy.utils.BrowserUtils;

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
			log.warn(" :: "+audit.getMessages());
			for(UXIssueMessage issue_msg : audit.getMessages()) {
				log.warn(" observation description :  "+issue_msg.getDescription());
				log.warn(" observation type :  "+issue_msg.getType());
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

	public Set<UXIssueMessage> getIssues(String audit_key) {
		assert audit_key != null;
		assert !audit_key.isEmpty();
		return audit_repo.findIssueMessages(audit_key);
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
	 * Creates a {@link List} of {@linkplain IssueElementMap} objects based on a {@link Set} of {@link Audit audits}
	 * @param audits
	 * @param page_url
	 * @return
	 * @throws MalformedURLException
	 */
	public Set<IssueElementMap> generateIssueElementMap(
			Set<Audit> audits, 
			URL page_url
	) throws MalformedURLException {
		Set<IssueElementMap> audit_elements = new HashSet<>();
		
		for(Audit audit : audits) {
			URL url = new URL(BrowserUtils.sanitizeUrl(audit.getUrl()));
			
			if(url.getHost().contentEquals(page_url.getHost()) 
					&& url.getPath().contentEquals(page_url.getPath())
			) {
				log.warn("preparing to process audit messages :: "+audit.getMessages());

				for(UXIssueMessage issue_msg : audit.getMessages()) {
					log.warn("issue message :: "+issue_msg.getDescription());

					Set<SimpleElement> elements = new HashSet<>();
					IssueElementMap observation_element = null;

					if(issue_msg.getType().equals(ObservationType.ELEMENT)) {
						log.warn("issue is an element type");

						ElementState element = ((ElementStateIssueMessage)issue_msg).getElement();
						
						elements.add(new SimpleElement(element.getKey(),
													   element.getScreenshotUrl(), 
													   element.getXLocation(), 
													   element.getYLocation(), 
													   element.getWidth(), 
													   element.getHeight(),
													   element.getXpath()));
						observation_element = new IssueElementMap(issue_msg, elements);
					}
					else{
						observation_element = new IssueElementMap(issue_msg, elements);
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
	public Set<ElementIssueMap> generateElementIssueMap(Set<Audit> audits, URL page_url) throws MalformedURLException {
		log.warn("generating element observation map.....");
		
		Set<ElementIssueMap> element_issues = new HashSet<>();
		
		Map<String, Set<UXIssueMessage>> issue_map = new HashMap<>(); 
		Map<String, SimpleElement> element_state_map = new HashMap<>();
		
		for(Audit audit : audits) {
			log.warn("checking if "+audit.getUrl()+"   matches  "+page_url);
			URL url = new URL(BrowserUtils.sanitizeUrl(audit.getUrl()));
			
			if(url.getHost().contentEquals(page_url.getHost()) 
				&& url.getPath().contentEquals(page_url.getPath())
			) {
				log.warn("preparing to process audit messages :: "+audit.getMessages());
				for(UXIssueMessage issue_msg : audit.getMessages()) {
					log.warn("issue message :: "+issue_msg.getDescription());
					if(issue_msg.getType().equals(ObservationType.ELEMENT)) {
						log.warn("issue is an element type");
						ElementState element = ((ElementStateIssueMessage)issue_msg).getElement();
						
						if(!element_state_map.containsKey(element.getKey())) {
							log.warn("element hasn't been encountered before. building new SimpleElement...");
							SimpleElement simple_element = 	new SimpleElement(element.getKey(),
																			  element.getScreenshotUrl(), 
																			  element.getXLocation(), 
																			  element.getYLocation(), 
																			  element.getWidth(), 
																			  element.getHeight(),
																			  element.getXpath());
							element_state_map.put(element.getKey(), simple_element);
						}

							//associate issue with element
						if(issue_map.containsKey(element.getKey())) {
							issue_map.get(element.getKey()).add(issue_msg);
						}
						else {
							Set<UXIssueMessage> issue_messages = new HashSet<>();
							issue_messages.add(issue_msg);
							issue_map.put(element.getKey(), issue_messages);
						}
					}
				}
			}
		}
		log.warn("bundling maps together...");
		//associate simple elements and issues
		for(String element_key : element_state_map.keySet()) {
			
			ElementIssueMap issue_element = new ElementIssueMap(issue_map.get(element_key), element_state_map.get(element_key));
			element_issues.add(issue_element);
		}
		
		return element_issues;
	}

	public UXIssueMessage addIssue(
			String key, 
			String issue_key) {
		assert key != null;
		assert !key.isEmpty();
		assert issue_key != null;
		assert !issue_key.isEmpty();
		
		log.warn("ADD OBSERVATION KEY :: "+issue_key);
		log.warn("ADD OBSERVATION audit key :: "+key);
		return audit_repo.addIssueMessage(key, issue_key);
	}

}
