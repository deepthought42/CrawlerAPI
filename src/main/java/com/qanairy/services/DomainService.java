package com.qanairy.services;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.validation.constraints.NotBlank;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qanairy.models.Action;
import com.qanairy.models.Domain;
import com.qanairy.models.Form;
import com.qanairy.models.PageVersion;
import com.qanairy.models.PageLoadAnimation;
import com.qanairy.models.Element;
import com.qanairy.models.PageState;
import com.qanairy.models.Test;
import com.qanairy.models.TestRecord;
import com.qanairy.models.TestUser;
import com.qanairy.models.audit.Audit;
import com.qanairy.models.audit.AuditRecord;
import com.qanairy.models.repository.DomainRepository;

@Service
public class DomainService {
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private DomainRepository domain_repo;
	
	@Autowired
	private AuditRecordService audit_record_service;
	

	public Set<Domain> getDomains() {
		return domain_repo.getDomains();
	}
	
	public Set<TestUser> getTestUsers(String user_id, Domain domain) {
		return domain_repo.getTestUsers(user_id, domain.getKey());
	}

	public Domain findByHostForUser(String host, String user_id) {
		return domain_repo.findByHostForUser(host, user_id);
	}
	
	public Domain findByHost(String host) {
		return domain_repo.findByHost(host);
	}

	public Domain findByUrlAndAccountId(String url, String user_id) {
		return domain_repo.findByUrlAndAccountId(url, user_id);
	}
	
	public Domain findByUrl(String url) {
		return domain_repo.findByUrl(url);
	}
	
	public Domain save(Domain domain) {
		return domain_repo.save(domain);	
	}
	
	public Domain addTest(String url, Test test, String user_id) throws MalformedURLException{
		assert url != null;
		assert !url.isEmpty();
		assert test != null;
		assert user_id != null;

		Domain domain = domain_repo.findByUrlAndAccountId(url, user_id);
		domain.addTest(test);
		return domain_repo.save(domain);
	}
	
	public int getTestCount(String user_id, String url) {
		return domain_repo.getTestCount(user_id, url);
	}

	public Optional<Domain> findById(long domain_id) {
		return domain_repo.findById(domain_id);
	}

	public Set<TestUser> getTestUsers(String user_id, String key) {
		return domain_repo.getTestUsers(user_id, key);
	}

	public void deleteTestUser(String user_id, String domain_key, String username) {
		domain_repo.deleteTestUser(user_id, domain_key, username);
	}

	public Set<Form> getForms(String user_id, String url) {
		return domain_repo.getForms(user_id, url);
	}
	
	public int getFormCount(String user_id, String url) {
		return domain_repo.getFormCount(user_id, url);
	}

	public Set<Element> getElementStates(String url, String user_id) {
		return domain_repo.getElementStates(url, user_id);
	}

	public Set<Action> getActions(String user_id, String url) {
		return domain_repo.getActions(user_id, url);
	}

	public Set<PageState> getPageStates(String host) {
		return domain_repo.getPageStates(host);
	}

	public Domain findByKey(String key, String user_id) {
		return domain_repo.findByKey(key, user_id);
	}

	public Set<Test> getTests(String user_id, String url) {
		return domain_repo.getTests(user_id, url);
	}
	
	public Set<TestRecord> getTestRecords(String user_id, String url) {
		return domain_repo.getTestRecords(user_id, url);
	}

	public Set<PageLoadAnimation> getAnimations(String user_id, String url) {
		return domain_repo.getAnimations(user_id, url);
	}

	/**
	 * Creates a relationship between existing {@link PageVersion} and {@link Domain} records
	 * 
	 * @param url {@link Domain} url
	 * @param page_key key of {@link PageVersion} object
	 * @param user_id 
	 * 
	 * @return
	 * 
	 * @pre url != null
	 * @pre !url.isEmpty()
	 * @pre page_key != null
	 * @pre !page_key.isEmpty()
	 * @pre user_id != null
	 * 
	 */
	public boolean addPage(String url, PageVersion page, String user_id) {
		assert url != null;
		assert !url.isEmpty();
		assert page != null;
		assert user_id != null;
		
		Domain domain = findByUrlAndAccountId(url, user_id);
		
		PageVersion page_record = domain_repo.getPage(user_id, url, page.getKey());
		if(page_record == null) {
			domain.addPage(page);
			domain_repo.save(domain);
			return true;
		}
		return false;
	}

	/**
	 * 
	 * @param user_id
	 * @param url
	 * @return
	 * 
	 * @pre url != null;
	 * @pre !url.isEmpty();
	 * @pre user_id != null;
	 * @pre !user_id.isEmpty();
	 */
	public Set<PageVersion> getPagesForUser(String user_id, String url) {
		assert url != null;
		assert !url.isEmpty();
		assert user_id != null;
		assert !user_id.isEmpty();
		
		return domain_repo.getPagesForUserId(user_id, url);
	}

	public AuditRecord getMostRecentDomainAuditRecord(String host) {
		return domain_repo.getMostRecentDomainAuditRecord(host);
	}

	public List<PageVersion> getPages(String domain_host) {
		return domain_repo.getPages(domain_host);
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
	public void addAuditRecord(String domain_key, String audit_record_key) {
		assert domain_key != null;
		assert !domain_key.isEmpty();
		assert audit_record_key != null;
		assert !audit_record_key.isEmpty();
		//check if audit record is already attached to domain

		domain_repo.addAuditRecord(domain_key, audit_record_key);
	}

	public Set<AuditRecord> getAuditRecords(String domain_key) {
		return domain_repo.getAuditRecords(domain_key);
	}
	
	public Set<Audit> getMostRecentAuditRecordColorPaletteAudits(String host) {
		assert host != null;
		assert !host.isEmpty();
		
		AuditRecord record = audit_record_service.findMostRecent(host).get();
        return audit_record_service.getAllColorPaletteAudits(record.getKey());
	}

	public Set<Audit> getMostRecentAuditRecordTextColorContrast(String host) {
		assert host != null;
		assert !host.isEmpty();
		
		AuditRecord record = audit_record_service.findMostRecent(host).get();
        return audit_record_service.getAllTextColorContrastAudits(record.getKey());
	}

	public Set<Audit> getMostRecentAuditRecordNonTextColorContrast(String host) {
		assert host != null;
		assert !host.isEmpty();
		
        AuditRecord record = audit_record_service.findMostRecent(host).get();
		return audit_record_service.getAllNonTextColorContrastAudits(record.getKey());
	}

	public Set<Audit> getMostRecentAuditRecordTypeface(String host) {
		assert host != null;
		assert !host.isEmpty();
		
		AuditRecord record = audit_record_service.findMostRecent(host).get();
        return audit_record_service.getAllTypefaceAudits(record.getKey());
	}

	public Set<Audit> getMostRecentAuditRecordLinks(String host) {
		assert host != null;
		assert !host.isEmpty();
		
		AuditRecord record = audit_record_service.findMostRecent(host).get();
        return audit_record_service.getAllLinkAudits(record.getKey());
	}

	public Set<Audit> getMostRecentAuditRecordTitleAndHeader(String host) {
		assert host != null;
		assert !host.isEmpty();
		
		AuditRecord record = audit_record_service.findMostRecent(host).get();
        return audit_record_service.getAllTitleAndHeaderAudits(record.getKey());
	}

	public Set<Audit> getMostRecentAuditRecordAltText(String host) {
		assert host != null;
		assert !host.isEmpty();
		
		AuditRecord record = audit_record_service.findMostRecent(host).get();
        return audit_record_service.getAllAltTextAudits(record.getKey());
	}

	public Domain findByAuditRecord(String audit_record_key) {
		return domain_repo.findByAuditRecord(audit_record_key);
	}

	public Set<Audit> getMostRecentAuditRecordMargins(@NotBlank String host) {
		assert host != null;
		
		AuditRecord record = audit_record_service.findMostRecent(host).get();
        return audit_record_service.getAllMarginAudits(record.getKey());	
    }
	
	public Set<Audit> getMostRecentAuditRecordPadding(@NotBlank String host) {
		assert host != null;
		
		AuditRecord record = audit_record_service.findMostRecent(host).get();
        return audit_record_service.getAllPagePaddingAudits(record.getKey());
    }

	public Set<Audit> getMostRecentAuditRecordParagraphing(String host) {
		assert host != null;
		
		AuditRecord record = audit_record_service.findMostRecent(host).get();
        return audit_record_service.getAllPageParagraphingAudits(record.getKey());
	}
	
	public Set<Audit> getMostRecentAudits(String host) {
		assert host != null;
		assert !host.isEmpty();
		
		AuditRecord record = audit_record_service.findMostRecent(host).get();
        return audit_record_service.getAllPageAudits(record.getKey());
	}

	public AuditRecord getMostRecentAuditRecord(String host) {
		assert host != null;
		assert !host.isEmpty();
		
		return audit_record_service.findMostRecent(host).get();
	}
}
