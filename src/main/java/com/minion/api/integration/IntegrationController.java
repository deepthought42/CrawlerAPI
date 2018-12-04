package com.minion.api.integration;

import org.json.XML;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.qanairy.models.Account;
import com.qanairy.models.Domain;
import com.qanairy.models.repository.AccountRepository;


/**
 * 
 */
@RestController
@RequestMapping("/accounts")
public class IntegrationController {

	@Autowired
	private AccountRepository account_repo;
	
	/**
	 * Runs all tests for a given domain and account using an api key to locate the account
	 * In the event that the domain is not registered with the {@link Account} then the system throws
	 * an exception
	 * 
	 * @param domain
	 * @param api_key
	 * 
	 * @return
	 */
    @RequestMapping(method = RequestMethod.GET)
	public XML runAllTests(@RequestBody String host,
						   @RequestBody String api_key){
		Domain domain = account_repo.getAccountDomainByApiKeyAndHost(api_key, host);
		if(domain != null){
			//run all tests for the domain
		}
		return null;
	}
}
