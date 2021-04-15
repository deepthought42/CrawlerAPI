package com.qanairy.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qanairy.models.audit.UXIssueMessage;
import com.qanairy.models.repository.UXIssueMessageRepository;

@Service
public class UXIssueMessageService {
	
	@Autowired
	private UXIssueMessageRepository ux_message_repo;
	
	public UXIssueMessage save(UXIssueMessage low_header_contrast_observation) {
		return ux_message_repo.save(low_header_contrast_observation);
	}

}
