package com.minion.api.integration;

import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.seratch.jslack.common.http.SlackHttpClient;
import com.minion.api.exception.InvalidApiKeyException;
import com.qanairy.integrations.SlackBot;
import com.qanairy.integrations.SlackService;
import com.qanairy.models.Account;
import com.qanairy.models.repository.AccountRepository;


@RestController
@RequestMapping("/integration/slack")
public class SlackController {
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
	 * @throws InvalidApiKeyException 
	 */
    @RequestMapping(method = RequestMethod.GET)
	public void slackAuthentication(HttpServletRequest request, 
										@RequestParam String code,
										@RequestParam String api_key) throws InvalidApiKeyException{
    	Account acct = account_repo.getAccountByApiKey(api_key);
		if(acct == null){
    		throw new InvalidApiKeyException("Invalid API key");
    	}
    	
		SlackHttpClient client = new SlackHttpClient();
		client.postForm(url, formBody)
		/* UNCOMMENT WHEN READY TO HANDLE SUBSCRIPTIONS
		 
    	if(subscription_service.hasExceededSubscriptionTestRunsLimit(acct, subscription_service.getSubscriptionPlanName(acct))){
    		throw new PaymentDueException("Your plan has 0 test runs available. Upgrade now to run more tests");
        }
    	*/
		SlackService service = new SlackService();
		//send welcome to qanairy meesage
		service.sendMessage(hook_url, "Welcome to Qanairy");
	}
}
