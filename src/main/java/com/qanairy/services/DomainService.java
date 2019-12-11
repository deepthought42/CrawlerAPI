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
	
	public Set<TestUser> getTestUsers(Domain domain) {
		return domain_repo.getTestUsers(domain.getKey());
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
	
	public int getTestCount(String url) {
		return domain_repo.getTestCount(url);
	}

	public DiscoveryRecord getMostRecentDiscoveryRecord(String url, String user_id) {
		return domain_repo.getMostRecentDiscoveryRecord(url, user_id);
	}

	public Set<DiscoveryRecord> getDiscoveryRecords(String url) {
		return domain_repo.getDiscoveryRecords(url);
	}

	public Optional<Domain> findById(long domain_id) {
		return domain_repo.findById(domain_id);
	}

	public Set<TestUser> getTestUsers(String key) {
		return domain_repo.getTestUsers(key);
	}

	public void deleteTestUser(String domain_key, String username) {
		domain_repo.deleteTestUser(domain_key, username);
	}

	public Set<Form> getForms(String url) {
		return domain_repo.getForms(url);
	}

	public Set<ElementState> getElementStates(String host) {
		return domain_repo.getElementStates(host);
	}

	public Set<Action> getActions(String host) {
		return domain_repo.getActions(host);
	}

	public Set<PageState> getPageStates(String host) {
		return domain_repo.getPageStates(host);
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

	public Set<Test> getTests(String url) {
		return domain_repo.getTests(url);
	}
	
	public Set<TestRecord> getTestRecords(String url) {
		return domain_repo.getTestRecords(url);
	}

	public Set<PageLoadAnimation> getAnimations(String url) {
		return domain_repo.getAnimations(url);
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
