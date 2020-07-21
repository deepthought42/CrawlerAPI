package com.qanairy.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qanairy.models.audit.Observation;
import com.qanairy.models.repository.ObservationRepository;

@Service
public class ObservationService {

	@Autowired
	private ObservationRepository observation_repo;
	
	public Observation save(Observation observation) {
		return observation_repo.save(observation);
	}

}
