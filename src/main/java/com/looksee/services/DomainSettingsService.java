package com.looksee.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.looksee.models.DomainSettings;
import com.looksee.models.repository.DomainSettingsRepository;

@Service
public class DomainSettingsService {
	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private DomainSettingsRepository domain_settings_repo;	
	
	public DomainSettings save(DomainSettings domain_settings) {
		return domain_settings_repo.save(domain_settings);	
	}

}
