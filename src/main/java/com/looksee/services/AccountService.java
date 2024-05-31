package com.looksee.services;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.looksee.api.exception.MissingSubscriptionException;
import com.looksee.models.Account;
import com.looksee.models.DiscoveryRecord;
import com.looksee.models.Domain;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.dto.exceptions.UnknownAccountException;
import com.looksee.models.repository.AccountRepository;

/**
 * Contains business logic for interacting with and managing accounts
 *
 */
@Service
public class AccountService {

	@Autowired
	private AccountRepository account_repo;
	
	@Deprecated
	public void addDomainToAccount(Account acct, Domain domain){
		account_repo.addDomain(domain.getId(), acct.getId());
	}

	public void addDomainToAccount(long account_id, long domain_id){
		account_repo.addDomain(domain_id, account_id);
	}

	public Account findByEmail(String email) {
		assert email != null;
		assert !email.isEmpty();
		
		return account_repo.findByEmail(email);
	}

	public Account save(Account acct) {
		return account_repo.save(acct);
	}

	public Account findByUserId(String id) {
		return account_repo.findByUserId(id);
	}

	public void deleteAccount(long account_id) {
        account_repo.deleteAccount(account_id);
	}
	
	public void removeDomain(long account_id, long domain_id) {
		account_repo.removeDomain(account_id, domain_id);
	}
	
	public Set<DiscoveryRecord> getDiscoveryRecordsByMonth(String username, int month) {
		return account_repo.getDiscoveryRecordsByMonth(username, month);
	}

	public int getTestCountByMonth(String username, int month) {
		return account_repo.getTestCountByMonth(username, month);
	}

	public Optional<Account> findById(long id) {
		return account_repo.findById(id);
	}

	public Domain findDomain(String email, String url) {
		assert email != null;
		assert !email.isEmpty();
		assert url != null;
		assert !url.isEmpty();
		
		return account_repo.findDomain(email, url);
	}
	
	public Account addAuditRecord(long id, long audit_record_id) {
		return account_repo.addAuditRecord(id, audit_record_id);
	}

	public Set<Account> findForAuditRecord(long id) {
		return account_repo.findAllForAuditRecord(id);
	}

	public List<AuditRecord> findMostRecentPageAudits(long account_id, int limit) {
		return account_repo.findMostRecentAuditsByAccount(account_id, limit);
	}

	public int getPageAuditCountByMonth(long account_id, int month) {
		return account_repo.getPageAuditCountByMonth(account_id, month);
	}

	public Account findByCustomerId(String customer_id) {
		return account_repo.findByCustomerId(customer_id);
	}
	
	public int getDomainAuditCountByMonth(long account_id, int month) {
		return account_repo.getDomainAuditRecordCountByMonth(account_id, month);
	}

	/**
	 * Checks that there is an account associated with the given Principal and 
	 *  that the account has a subscription assigned
	 * 
	 * @param userPrincipal user {@link Principal}
	 * @throws UnknownAccountException
	 * @throws MissingSubscriptionException
	 */
    public Account retrieveAndValidateAccount(Principal userPrincipal) throws UnknownAccountException, MissingSubscriptionException {
		String acct_id = userPrincipal.getName();
		Account acct = findByUserId(acct_id);
		
		if(acct == null){
			throw new UnknownAccountException();
		}
		else if(acct.getSubscriptionToken() == null){
			throw new MissingSubscriptionException();
		}

		return acct;
    }
}
