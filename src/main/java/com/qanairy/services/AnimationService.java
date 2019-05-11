package com.qanairy.services;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qanairy.models.Animation;
import com.qanairy.models.Redirect;
import com.qanairy.models.repository.AnimationRepository;
import com.qanairy.models.repository.DomainRepository;

@Service
public class AnimationService {
	private static Logger log = LoggerFactory.getLogger(PageService.class.getName());

	@Autowired
	private AnimationRepository animation_repo;
	
	@Autowired
	private DomainRepository domain_repo;
	
	public Animation findByKey(String key){
		return animation_repo.findByKey(key);
	}
	
	public Animation save(Animation animation){
		Animation record = findByKey(animation.getKey());
		if(record == null){
			log.warn("animation key   :: "+animation.getKey());
			log.warn("animation type  :: " + animation.getType());
			log.warn("animation urls  ::  " + animation.getImageUrls());
			log.warn("animation repo :: " + animation_repo);
			record = animation_repo.save(animation);
		}
		return record;
	}

	public Set<Redirect> getRedirects(String host) {
		return domain_repo.getRedirects(host);
	}
}
