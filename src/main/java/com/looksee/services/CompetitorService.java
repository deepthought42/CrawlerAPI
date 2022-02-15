package com.looksee.services;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.looksee.models.Competitor;
import com.looksee.models.repository.CompetitorRepository;

/**
 * Contains business logic for interacting with and managing accounts
 *
 */
@Service
public class CompetitorService {

	@Autowired
	private CompetitorRepository competitor_repo;
	

	public Competitor save(Competitor competitor) {
		return competitor_repo.save(competitor);
	}

	public void delete(Competitor competitor) {
		competitor_repo.delete(competitor);
	}

	public void deleteById(long competitor_id) {
		competitor_repo.deleteById(competitor_id);
	}

	public Iterable<Competitor> getAll() {
		return competitor_repo.findAll();
	}

	public void addBrand(long competitor_id, long brand_id) {
		competitor_repo.addBrand(competitor_id, brand_id);
	}

	public Optional<Competitor> findById(long competitor_id) {
		return competitor_repo.findById(competitor_id);
	}
}
