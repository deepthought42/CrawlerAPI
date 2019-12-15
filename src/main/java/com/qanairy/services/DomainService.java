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

	public Set<ElementState> getElementStates(String host, String user_id) {
		return domain_repo.getElementStates(host, user_id);
	}

	public Set<Action> getActions(String user_id, String host) {
		return domain_repo.getActions(user_id, host);
	}

	public Set<PageState> getPageStates(String user_id, String host) {
		return domain_repo.getPageStates(user_id, host);
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
	 * 
	 * @param host
	 * @param page_state
	 * @return
	 * 
	 * #pre host != null
	 * @pre !host.isEmpty()
	 * @pre page_state != null
	 */
	public Domain addPageState(String url, PageState page_state, String user_id) {
		assert url != null;
		assert !url.isEmpty();
		assert page_state != null;
		assert user_id != null;
		
		Domain domain = domain_repo.findByUrl(url, user_id);
		domain.addPageState(page_state);
		return domain_repo.save(domain);
	}
}
