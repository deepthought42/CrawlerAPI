package com.looksee.services;

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

import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.PageStateAudits;
import com.looksee.models.SimpleElement;
import com.looksee.models.SimplePage;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.ColorContrastIssueMessage;
import com.looksee.models.audit.ElementIssueMap;
import com.looksee.models.audit.ElementStateIssueMessage;
import com.looksee.models.audit.IssueElementMap;
import com.looksee.models.audit.UXIssueMessage;
import com.looksee.models.enums.ObservationType;
import com.looksee.models.repository.AuditRepository;
import com.looksee.utils.BrowserUtils;

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
		
		List<PageStateAudits> page_audits = new ArrayList<>();
		for(String url : audit_url_map.keySet()) {
			//load page state by url
			PageState page_state = page_state_service.findByUrl(url);
			SimplePage simple_page = new SimplePage(
											page_state.getUrl(), 
											page_state.getViewportScreenshotUrl(), 
											page_state.getFullPageScreenshotUrl(), 
											page_state.getFullPageWidth(), 
											page_state.getFullPageHeight(),
											page_state.getSrc(),
											page_state.getKey());
			PageStateAudits page_state_audits = new PageStateAudits(simple_page, audit_url_map.get(url));
			page_audits.add( page_state_audits ) ;
		}
		
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
				for(UXIssueMessage issue_msg : audit.getMessages()) {
					IssueElementMap observation_element = null;

					if(issue_msg.getType().equals(ObservationType.ELEMENT)) {

						ElementState element = ((ElementStateIssueMessage)issue_msg).getElement();
						
						SimpleElement simple_element = new SimpleElement(element.getKey(),
																   element.getScreenshotUrl(), 
																   element.getXLocation(), 
																   element.getYLocation(), 
																   element.getWidth(), 
																   element.getHeight(),
																   element.getCssSelector(),
																  element.getAllText());
						observation_element = new IssueElementMap(issue_msg, simple_element);
					}
					else{
						observation_element = new IssueElementMap(issue_msg, null);
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
		Set<ElementIssueMap> element_issues = new HashSet<>();
		
		Map<String, Set<UXIssueMessage>> issue_map = new HashMap<>(); 
		Map<String, SimpleElement> element_state_map = new HashMap<>();
		
		for(Audit audit : audits) {
			URL url = new URL(BrowserUtils.sanitizeUrl(audit.getUrl()));
			
			if(url.getHost().contentEquals(page_url.getHost()) 
				&& url.getPath().contentEquals(page_url.getPath())
			) {
				for(UXIssueMessage issue_msg : audit.getMessages()) {
					//NOTE: color contrast is first because it inherits form EleementIssueMessage
					if(issue_msg.getType().equals(ObservationType.COLOR_CONTRAST)) {
						ElementState element = ((ColorContrastIssueMessage)issue_msg).getElement();
						
						if(!element_state_map.containsKey(element.getKey())) {
							SimpleElement simple_element = 	new SimpleElement(element.getKey(),
																			  element.getScreenshotUrl(), 
																			  element.getXLocation(), 
																			  element.getYLocation(), 
																			  element.getWidth(), 
																			  element.getHeight(),
																			  element.getCssSelector(),
																			  element.getAllText());
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
					else if(issue_msg.getType().equals(ObservationType.ELEMENT)) {
						ElementState element = ((ElementStateIssueMessage)issue_msg).getElement();
						
						if(!element_state_map.containsKey(element.getKey())) {
							SimpleElement simple_element = 	new SimpleElement(element.getKey(),
																			  element.getScreenshotUrl(), 
																			  element.getXLocation(), 
																			  element.getYLocation(), 
																			  element.getWidth(), 
																			  element.getHeight(),
																			  element.getCssSelector(),
																			  element.getAllText());
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
		
		return audit_repo.addIssueMessage(key, issue_key);
	}

}
