package com.looksee.services;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.looksee.models.ActionOLD;
import com.looksee.models.Domain;
import com.looksee.models.Element;
import com.looksee.models.Form;
import com.looksee.models.PageLoadAnimation;
import com.looksee.models.PageState;
import com.looksee.models.Test;
import com.looksee.models.TestRecord;
import com.looksee.models.TestUser;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.DomainAuditRecord;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.models.competitiveanalysis.Competitor;
import com.looksee.models.designsystem.DesignSystem;
import com.looksee.models.repository.AuditRecordRepository;
import com.looksee.models.repository.CompetitorRepository;
import com.looksee.models.repository.DesignSystemRepository;
import com.looksee.models.repository.DomainRepository;
import com.looksee.models.repository.ElementRepository;
import com.looksee.models.repository.FormRepository;
import com.looksee.models.repository.PageStateRepository;
import com.looksee.models.repository.TestUserRepository;

/**
 * 
 * 
 */
@Service
public class DomainService {
	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private AccountService account_service;

	@Autowired
	private DesignSystemService design_system_service;

	@Autowired
	private DomainRepository domain_repo;
	
	@Autowired
	private AuditRecordRepository audit_record_repo;
	
	@Autowired
	private PageStateRepository page_state_repo;
	
	@Autowired
	private TestUserRepository test_user_repo;
	
	@Autowired
	private FormRepository form_repo;

	@Autowired
	private DesignSystemRepository design_system_repo;
	
	@Autowired
	private CompetitorRepository competitor_repo;
	
	@Autowired
	private ElementRepository element_repo;
	
	public Set<Domain> getDomains() {
		return domain_repo.getDomains();
	}
	
	public Set<TestUser> getTestUsers(long domain_id) {
		return test_user_repo.getTestUsers(domain_id);
	}

	public Domain findByHostForUser(String host, String username) {
		return domain_repo.findByHostForUser(host, username);
	}
	
	public Domain findByHost(String host) {
		return domain_repo.findByHost(host);
	}

	public Domain findByUrl(String url) {
		return domain_repo.findByUrl(url);
	}
	
	public Domain save(Domain domain) {
		return domain_repo.save(domain);	
	}
	
	public int getTestCount(long account_id, String url) {
		return domain_repo.getTestCount(account_id, url);
	}

	public Optional<Domain> findById(long domain_id) {
		return domain_repo.findById(domain_id);
	}

	public boolean deleteTestUser(long domain_id, long user_id) {
		return domain_repo.deleteTestUser(domain_id, user_id) > 0;
	}

	public Set<Form> getForms(long account_id, String url) {
		return form_repo.getForms(account_id, url);
	}
	
	public int getFormCount(long account_id, String url) {
		return domain_repo.getFormCount(account_id, url);
	}

	public Set<Element> getElementStates(String url, String username) {
		return element_repo.getElementStates(url, username);
	}

	public Set<ActionOLD> getActions(long account_id, String url) {
		return domain_repo.getActions(account_id, url);
	}

	public Set<PageState> getPageStates(long domain_id) {
		return page_state_repo.getPageStates(domain_id);
	}

	public Domain findByKey(String key, String username) {
		return domain_repo.findByKey(key, username);
	}

	public Set<Test> getTests(long account_id, String url) {
		return domain_repo.getTests(account_id, url);
	}
	
	public Set<TestRecord> getTestRecords(long account_id, String url) {
		return domain_repo.getTestRecords(account_id, url);
	}

	public Set<PageLoadAnimation> getAnimations(long account_id, String url) {
		return domain_repo.getAnimations(account_id, url);
	}

	public Set<Domain> getDomainsForAccount(long account_id) {		
		return domain_repo.getDomainsForAccount(account_id);
	}
	
	/**
	 * Creates a relationship between existing {@link PageVersion} and {@link Domain} records
	 * 
	 * @param url {@link Domain} url
	 * @param page_key key of {@link PageVersion} object
	 * @return
	 * 
	 * @pre host != null
	 * @pre !host.isEmpty()
	 * @pre page_version_key != null
	 * @pre !page_version_key.isEmpty()
	 * 
	 */
	public boolean addPage(long domain_id, long page_id) {
		//check if page already exists. If it does then return true;
		Optional<PageState> page = page_state_repo.getPage(domain_id, page_id);
		if(page.isPresent()) {
			return true;
		}
		
		return page_state_repo.addPage(domain_id, page_id) != null;
	}


	public Set<PageState> getPages(String domain_host) {
		return page_state_repo.getPages(domain_host);
	}

	public Domain findByPageState(String page_state_key) {
		return domain_repo.findByPageState(page_state_key);
	}

	/**
	 * Creates graph edge connection {@link AuditRecord} to {@link Domain domain} 
	 * 
	 * @param domain_key
	 * @param audit_record_key
	 * 
	 * @pre domain_key != null;
	 * @pre !domain_key.isEmpty();
	 * @pre audit_record_key != null;
	 * @pre !audit_record_key.isEmpty();
	 */
	public void addAuditRecord(long domain_id, String audit_record_key) {
		assert audit_record_key != null;
		assert !audit_record_key.isEmpty();
		//check if audit record is already attached to domain

		domain_repo.addAuditRecord(domain_id, audit_record_key);
	}

	public Set<AuditRecord> getAuditRecords(String domain_key) {
		return audit_record_repo.getAuditRecords(domain_key);
	}

	public Domain findByAuditRecord(long audit_record_id) {
		return domain_repo.findByAuditRecord(audit_record_id);
	}

	public Optional<PageAuditRecord> getMostRecentPageAuditRecord(String page_url) {
		assert page_url != null;
		assert !page_url.isEmpty();
		
		return audit_record_repo.getMostRecentPageAuditRecord(page_url);
	}

	public DesignSystem updateExpertiseSettings(long domain_id, String expertise) {
		return design_system_repo.updateExpertiseSetting(domain_id, expertise);
	}

	public List<DomainAuditRecord> getAuditRecordHistory(long domain_id) {
		return audit_record_repo.getAuditRecordHistory(domain_id);
	}

	public Competitor addCompetitor(long domain_id, long competitor_id) {
		return competitor_repo.addCompetitor(domain_id, competitor_id);
	}

	public Optional<DesignSystem> getDesignSystem(long domain_id) {
		return design_system_repo.getDesignSystem(domain_id);
	}

	public DesignSystem addDesignSystem(long domain_id, long design_system_id) {
		return design_system_repo.addDesignSystem(domain_id, design_system_id);
	}

	public DesignSystem updateWcagSettings(long domain_id, String wcag_level) {
		return design_system_repo.updateWcagSettings(domain_id, wcag_level);
	}

	public DesignSystem updateAllowedImageCharacteristics(long domain_id, List<String> allowed_image_characteristics) {
		return design_system_repo.updateAllowedImageCharacteristics(domain_id, allowed_image_characteristics);
	}

	public List<Competitor> getCompetitors(long domain_id) {
		return competitor_repo.getCompetitors(domain_id);
	}

	public List<TestUser> findTestUsers(long domain_id) {
		return test_user_repo.findTestUsers(domain_id);
	}

	public void addTestUser(long domain_id, long test_user_id) {
		domain_repo.addTestUser(domain_id, test_user_id);	
	}

	/**
	 * Creates a new {@link Domain} in the database and attaches it to the account
	 * as well as adding a default {@link DesignSystem design system
	 * 
	 * @param url
	 * @param account_id
	 * @return
	 */
    public Domain createDomain(URL url, long account_id) {
        String formatted_url = url.toString().replace("http://", "").replace("www.", "");
		Domain domain = domain_repo.findByAccountId(account_id, formatted_url);
		
		if(domain == null){
			domain = new Domain(url.getProtocol(), url.getHost(), url.getPath(), null);
			domain = save(domain);
			account_service.addDomainToAccount(domain.getId(), account_id);
			
			DesignSystem domain_settings = new DesignSystem();
			domain_settings = design_system_service.save(domain_settings);
			domain.setDesignSystem(domain_settings);
			log.warn("adding domain = "+domain.getId() + "  to account = "+account_id);
			addDesignSystem(domain.getId(), domain_settings.getId());
		}
		log.warn("domain record not found. Creating new domain");

		return domain;
    }
}
