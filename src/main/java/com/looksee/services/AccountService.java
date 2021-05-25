package com.looksee.services;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.looksee.models.Account;
import com.looksee.models.DiscoveryRecord;
import com.looksee.models.Domain;
import com.looksee.models.TestRecord;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.repository.AccountRepository;

/**
 * Contains business logic for interacting with and managing accounts
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
			account_repo.addDomain(domain.getKey(), acct.getEmail());
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

	public Set<Domain> getDomainsForUser(String username) {
		assert username != null;
		assert !username.isEmpty();
		
		return account_repo.getDomainsForUser(username);
	}
	
	public Set<DiscoveryRecord> getDiscoveryRecordsByMonth(String username, int month) {
		return account_repo.getDiscoveryRecordsByMonth(username, month);
	}

	public int getTestCountByMonth(String username, int month) {
		return account_repo.getTestCountByMonth(username, month);
	}
	
	public List<TestRecord> getTestRecords(String username, String url) {
		return account_repo.getTestRecords(username, url);
	}

	public Optional<Account> findById(long id) {
		return account_repo.findById(id);
	}

	public Domain findDomain(String username, String url) {
		assert username != null;
		assert !username.isEmpty();
		assert url != null;
		assert !url.isEmpty();
		
		return account_repo.findDomain(username, url);
	}

	public AuditRecord addAuditRecord(String username, long audit_record_id) {
		assert username != null;
		assert !username.isEmpty();
		
		return account_repo.addAuditRecord(username, audit_record_id);
	}
	
	public AuditRecord addAuditRecord(long id, long audit_record_id) {
		
		return account_repo.addAuditRecord(id, audit_record_id);
	}
}
