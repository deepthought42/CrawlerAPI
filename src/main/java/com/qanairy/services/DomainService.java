package com.qanairy.services;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.neo4j.driver.v1.exceptions.ClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
	private static Logger log = LoggerFactory.getLogger(DomainService.class);

	@Autowired
	private TestService test_service;
	
	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private DomainRepository domain_repo;
	
	public Set<TestUser> getTestUsers(Domain domain) {
		return domain_repo.getTestUsers(domain.getKey());
	}

	public Domain findByHost(String host) {
		return domain_repo.findByHost(host);
	}

	public Domain save(Domain domain) {
		return domain_repo.save(domain);	
	}
	
	public Domain addTest(String host, Test test) throws MalformedURLException{
		assert host != null;
		assert !host.isEmpty();
		assert test != null;
		
		log.warn("domain host :: "+host);
		log.warn("test result when adding test :: " + test);
		Domain domain = domain_repo.findByHost(host);
		domain.addTest(test);
		return domain_repo.save(domain);
		//return save(domain);
	}
	
	public int getTestCount(String host_url) {
		return domain_repo.getTestCount(host_url);
	}

	public DiscoveryRecord getMostRecentDiscoveryRecord(String url) {
		return domain_repo.getMostRecentDiscoveryRecord(url);
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

	public Domain findByKey(String key) {
		return domain_repo.findByKey(key);
	}

	public Set<Test> getUnverifiedTests(String url) {
		return domain_repo.getUnverifiedTests(url);
	}

	public Set<Test> getVerifiedTests(String url) {
		return domain_repo.getVerifiedTests(url);
	}

	public Set<Test> getTests(String url) {
		return domain_repo.getTests(url);
	}
	
	public Set<TestRecord> getTestRecords(String url) {
		return domain_repo.getTestRecords(url);
	}

	public Set<PageLoadAnimation> getAnimations(String host) {
		return domain_repo.getAnimations(host);
	}
}
