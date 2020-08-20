package com.qanairy.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qanairy.models.journeys.Journey;
import com.qanairy.models.repository.JourneyRepository;

@Service
public class JourneyService {

	@Autowired
	private JourneyRepository journey_repo;
	
	public Journey save(Journey journey) {
		Journey journey_record = journey_repo.findByKey(journey.getKey());
		if(journey_record != null) {
			return journey_record;
		}
		return journey_repo.save(journey);	
	}
	
}
