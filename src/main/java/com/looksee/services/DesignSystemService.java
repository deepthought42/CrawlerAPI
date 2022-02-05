package com.looksee.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.looksee.models.designsystem.DesignSystem;
import com.looksee.models.repository.DesignSystemRepository;

/**
 * Contains business logic for interacting with and managing accounts
 *
 */
@Service
public class DesignSystemService {

	@Autowired
	private DesignSystemRepository design_system_repo;

	public DesignSystem save(DesignSystem design_system) {
		return design_system_repo.save(design_system);
	}
}
