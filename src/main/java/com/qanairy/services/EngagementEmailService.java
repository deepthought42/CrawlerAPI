package com.qanairy.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qanairy.models.Account;
import com.qanairy.models.DiscoveryRecord;

@Service
public class EngagementEmailService {

	@Autowired
	private DiscoveryRecordService discovery_service;
	
	@Autowired
	private DomainService domain_service;
	
	@Autowired
	private EmailService email_service;
	
	/**
	 * Sends an email to users that started a discovery between 48 and 72 hours ago
	 */
	public void send48HourInactivitySinceDiscoveryEmails() {
		// get all discoveries that were started 2 days ago
		List<DiscoveryRecord> disccoveries = discovery_service.getAllDiscoveriesFromTwoDaysAgo();
		for(DiscoveryRecord record : disccoveries){
			record.getTestCount();
			record.getExaminedPathCount();
			record.getTotalPathCount();
			
			List<Account> accounts = domain_service.getAccountsForDomain(record.getDomainUrl());
			for(Account account : accounts){				
				//send email to user
				//email_service.sendHtmlMessage(account.getUsername(), "Discovery progress report : "+record.getDomainUrl(), "Discovery on "+record.getDomainUrl()+" has so far reviewed "+record.getExaminedPathCount() + " out of " + record.getTotalPathCount() + " possible scenarios and generated "+record.getTestCount()+". Visit the <a href='app.qanairy.com/discovery>Discovery panel</a> to start classifying your tests");
				email_service.sendHtmlMessage("bkindred@qanairy.com", "Discovery progress report : "+record.getDomainUrl(), "Discovery on "+record.getDomainUrl()+" has so far reviewed "+record.getExaminedPathCount() + " out of " + record.getTotalPathCount() + " possible scenarios and generated "+record.getTestCount()+". Visit the <a href='app.qanairy.com/discovery>Discovery panel</a> to start classifying your tests");
			}
		}
	}
	
}
