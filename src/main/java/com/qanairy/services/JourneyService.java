package com.qanairy.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qanairy.models.journeys.Journey;
import com.qanairy.models.repository.JourneyRepository;

@Service
public class JourneyService {

	@Autowired
	private JourneyRepository journey_repo;
	
	public void save(Journey journey) {
		journey_repo.save(journey);	
	}
	
}
