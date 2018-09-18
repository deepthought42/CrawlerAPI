package com.qanairy.services;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.minion.browsing.Crawler;
import com.qanairy.models.Domain;
import com.qanairy.models.TestUser;
import com.qanairy.models.repository.DomainRepository;

@Component
public class DomainService {

	@Autowired
	private DomainRepository domain_repo;
	
	public Set<TestUser> getTestUsers(Domain domain) {
		return domain_repo.getTestUsers(domain.getUrl());
	}

	public Domain findByHost(String host) {
		return domain_repo.findByHost(host);
	}

	public Domain save(Domain domain) {
		return domain_repo.save(domain);	
	}

}
