package com.qanairy.services;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qanairy.models.Redirect;
import com.qanairy.models.repository.DomainRepository;
import com.qanairy.models.repository.RedirectRepository;

@Service
public class RedirectService {
	private static Logger log = LoggerFactory.getLogger(PageService.class.getName());

	@Autowired
	private RedirectRepository redirect_repo;
	
	@Autowired
	private DomainRepository domain_repo;
	
	public Redirect findByKey(String key){
		return redirect_repo.findByKey(key);
	}
	
	public Redirect save(Redirect redirect){
		Redirect record = findByKey(redirect.getKey());
		if(record == null){
			log.warn("redirect key   :: "+redirect.getKey());
			log.warn("redirect type  :: " + redirect.getType());
			log.warn("redirect urls  ::  " + redirect.getUrls());
			log.warn("redirect repo :: " + redirect_repo);
			record = redirect_repo.save(redirect);
		}
		return record;
	}

	public Set<Redirect> getRedirects(String url) {
		return domain_repo.getRedirects(url);
	}
}
