package com.qanairy.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qanairy.models.Account;
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
}
