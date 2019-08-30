package com.qanairy.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qanairy.models.DiscoveryRecord;

@Service
public class EngagementEmailService {

	@Autowired
	private DiscoveryRecordService discovery_service;
	/**
	 * Sends an email to users that started a discovery between 48 and 72 hours ago
	 */
	void send48HourInactivitySinceDiscoveryEmails() {
		// get all discoveries that were started 2 days ago
		List<DiscoveryRecord> disccoveries = discovery_service.getAllDiscoveriesFromTwoDaysAgo();
		// for each discovery
			// get count of tests created in discovery
			// get number of paths explored
			// get number of paths identified
			// get email of user that started discovery
			//send email to user
	}
	
}
