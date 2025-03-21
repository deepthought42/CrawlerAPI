package com.crawlerApi.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.crawlerApi.models.audit.recommend.Recommendation;
import com.crawlerApi.models.repository.RecommendationRepository;

@Service
public class RecommendationService {
	
	@Autowired
	private RecommendationRepository recommendation_repo;
	
	public Recommendation save(Recommendation rec) {
		return recommendation_repo.save(rec);
	}
}
