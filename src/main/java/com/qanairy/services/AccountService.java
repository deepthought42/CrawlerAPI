package com.qanairy.services;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qanairy.models.Account;
import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.Domain;
import com.qanairy.models.repository.AccountRepository;

/**
 * Contains business logic for interacting with and managing accounts
 * @author brand
 *
 */
@Service
public class AccountService {

	@Autowired
	private AccountRepository account_repo;
	
	public void addDomainToAccount(Account acct, Domain domain){
		boolean domain_exists_for_acct = false;
		for(Domain acct_domain : acct.getDomains()){
			if(acct_domain.equals(domain)){
				domain_exists_for_acct = true;
			}
		}
		
		if(!domain_exists_for_acct){
			acct.addDomain(domain);
			account_repo.save(acct);
		}
	}

	public Account findByUsername(String username) {
		return account_repo.findByUsername(username);
	}

	public Account save(Account acct) {
		return account_repo.save(acct);
	}

	public Account findByUserId(String id) {
		return account_repo.findByUserId(id);
	}

	public void deleteAccount(String userId) {
        account_repo.deleteAccountEdges(userId);
        account_repo.deleteAccount(userId);
	}

	public void removeDomain(String username, String domain_key) {
		account_repo.removeDomain(username, domain_key);
	}

	public Set<Domain> getDomains(String user_id) {
		return account_repo.getDomains(user_id);
	}

	public Set<DiscoveryRecord> getDiscoveryRecordsByMonth(String username, int month) {
		return account_repo.getDiscoveryRecordsByMonth(username, month);
	}

	public int getTestCountByMonth(String username, int month) {
		return account_repo.getTestCountByMonth(username, month);
	}
	
}
