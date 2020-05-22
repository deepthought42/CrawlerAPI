package com.qanairy.services;

import java.net.MalformedURLException;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qanairy.models.Action;
import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.Domain;
import com.qanairy.models.Form;
import com.qanairy.models.Page;
import com.qanairy.models.PageLoadAnimation;
import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.Test;
import com.qanairy.models.TestRecord;
import com.qanairy.models.TestUser;
import com.qanairy.models.repository.DomainRepository;

@Service
public class DomainService {

	@Autowired
	private DomainRepository domain_repo;
	
	@Autowired
	private DomainRepository page_service;
	
	public Set<TestUser> getTestUsers(String user_id, Domain domain) {
		return domain_repo.getTestUsers(user_id, domain.getKey());
	}

	public Domain findByHost(String host, String user_id) {
		return domain_repo.findByHost(host, user_id);
	}

	public Domain findByUrl(String url, String user_id) {
		return domain_repo.findByUrl(url, user_id);
	}
	
	public Domain save(Domain domain) {
		return domain_repo.save(domain);	
	}
	
	public Domain addTest(String url, Test test, String user_id) throws MalformedURLException{
		assert url != null;
		assert !url.isEmpty();
		assert test != null;
		assert user_id != null;

		Domain domain = domain_repo.findByUrl(url, user_id);
		domain.addTest(test);
		return domain_repo.save(domain);
	}
	
	public int getTestCount(String user_id, String url) {
		return domain_repo.getTestCount(user_id, url);
	}

	public DiscoveryRecord getMostRecentDiscoveryRecord(String url, String user_id) {
		return domain_repo.getMostRecentDiscoveryRecord(url, user_id);
	}

	public Set<DiscoveryRecord> getDiscoveryRecords(String user_id, String url) {
		return domain_repo.getDiscoveryRecords(user_id, url);
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

	public Set<ElementState> getElementStates(String url, String user_id) {
		return domain_repo.getElementStates(url, user_id);
	}

	public Set<Action> getActions(String user_id, String url) {
		return domain_repo.getActions(user_id, url);
	}

	public Set<PageState> getPageStates(String user_id, String url) {
		return domain_repo.getPageStates(user_id, url);
	}

	public Domain findByKey(String key, String user_id) {
		return domain_repo.findByKey(key, user_id);
	}

	public Set<Test> getUnverifiedTests(String url, String user_id) {
		return domain_repo.getUnverifiedTests(url, user_id);
	}

	public Set<Test> getVerifiedTests(String url, String user_id) {
		return domain_repo.getVerifiedTests(url, user_id);
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
	 * Creates a relationship between existing {@link Page} and {@link Domain} records
	 * 
	 * @param url {@link Domain} url
	 * @param page_key key of {@link Page} object
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
	public boolean addPage(String url, Page page, String user_id) {
		assert url != null;
		assert !url.isEmpty();
		assert page != null;
		assert user_id != null;
		
		Domain domain = findByUrl(url, user_id);
		
		Page page_record = page_service.getPage(user_id, url, page.getKey());
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
	public Set<Page> getPages(String user_id, String url) {
		assert url != null;
		assert !url.isEmpty();
		assert user_id != null;
		assert !user_id.isEmpty();
		
		return domain_repo.getPages(user_id, url);
	}
}
