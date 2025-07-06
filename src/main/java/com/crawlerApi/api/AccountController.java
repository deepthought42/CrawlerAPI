package com.crawlerApi.api;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.security.auth.login.AccountNotFoundException;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.crawlerApi.analytics.SegmentAnalyticsHelper;
import com.crawlerApi.security.SecurityConfig;
import com.looksee.exceptions.MissingSubscriptionException;
import com.looksee.exceptions.UnknownAccountException;
import com.looksee.models.Account;
import com.looksee.services.AccountService;
import com.mashape.unirest.http.exceptions.UnirestException;

/**
 *	API for interacting with {@link User} data
 */
@RestController
@RequestMapping("/accounts")
public class AccountController {
	private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected SecurityConfig appConfig;

    @Autowired
    private AccountService account_service;

    /**
     * Create new account
     *
     * @param authorization_header
     * @param account
     * @param principal
     *
     * @return
     * @throws Exception
     */

    @CrossOrigin(origins = "18.232.225.224, 34.233.19.82, 52.204.128.250, 3.132.201.78, 3.19.44.88, 3.20.244.231", maxAge = 3600)
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Account create(
    		HttpServletRequest request,
    		@RequestBody(required=true) Account account
    ) throws Exception{
    	log.warn("auth key passed :: " +request.getHeader("Authorization"));
    	Account acct = account_service.findByEmail(account.getEmail());

    	//create account
        if(acct != null){
        	//return error that account already exists
        	return acct;
        }

    	Map<String, Object> customerParams = new HashMap<String, Object>();
    	customerParams.put("description", "Customer for "+account.getEmail());
		
    	acct = new Account(account.getUserId(), account.getEmail(), "", "");
    	acct.setApiToken(UUID.randomUUID().toString());
        //final String username = usernameService.getUsername();
        // log username of user requesting account creation
        acct = account_service.save(acct);

    	
	   	SegmentAnalyticsHelper.identify(Long.toString(acct.getId()));
	   	//SegmentAnalyticsHelper.signupEvent(acct.getUserId());

        return acct;
    }

    /**
     * 
     * @param request
     * @param step_name
     * @return
     * @throws UnknownAccountException
     */
    @RequestMapping(path ="/onboarding_step", method = RequestMethod.POST)
    public List<String> setOnboardingStep(HttpServletRequest request, @RequestParam(value="step_name", required=true) String step_name) throws UnknownAccountException {
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName();
    	Account acct = account_service.findByUserId(id);

    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}
        List<String> onboarding = acct.getOnboardedSteps();
        onboarding.add(step_name);
        acct.setOnboardedSteps(onboarding);
        account_service.save(acct);

        return acct.getOnboardedSteps();
    }

    //@PreAuthorize("hasAuthority('read:accounts')")
    @RequestMapping(path ="/onboarding_steps_completed", method = RequestMethod.GET)
    public List<String> getOnboardingSteps(HttpServletRequest request) throws UnknownAccountException {
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName();
    	Account acct = account_service.findByUserId(id);

        if(acct == null){
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}

        return acct.getOnboardedSteps();
    }

    /**
     * Retrieves {@link Account account} with a given key
     *
     * @param key account key
     * @return {@link Account account}
     * @throws UnknownAccountException
     */
    @PreAuthorize("hasAuthority('read:accounts')")
    @RequestMapping( method = RequestMethod.GET)
    public Account get(HttpServletRequest request)
    		throws UnknownAccountException 
	{
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account acct = account_service.findByUserId(id);

		if(acct == null){
        	log.warn("Unknown account exception thrown");
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}

        return acct;
    }

	@PreAuthorize("hasAuthority('update:accounts')")
    @RequestMapping(value ="/{id}", method = RequestMethod.PUT)
    public Account update(final @PathVariable String key,
    					  final @Validated @RequestBody Account account) {
        log.info("update invoked");
        return account_service.save(account);
    }

	@PreAuthorize("hasAuthority('update:accounts')")
    @RequestMapping(value ="/{id}/refreshToken", method = RequestMethod.PUT)
    public Account updateApiToken(final @PathVariable long id) throws AccountNotFoundException {
        log.info("update invoked");
        Optional<Account> optional_acct = account_service.findById(id);
        if(optional_acct.isPresent()){
        	Account account = optional_acct.get();
        	account.setApiToken(UUID.randomUUID().toString());
        	return account_service.save(account);
        }
        else{
        	throw new AccountNotFoundException();
        }
    }

	/**
	 * Deletes account
	 *
	 * @param request
	 * @throws UnirestException
	 */
	@PreAuthorize("hasAuthority('delete:accounts')")
    @RequestMapping(method = RequestMethod.DELETE)
    public void delete(HttpServletRequest request) throws UnirestException{
		Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account account = account_service.findByUserId(id);
		
		//remove account
        account_service.deleteAccount(account.getId());
    }
}

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
class AccountExistsException extends RuntimeException {
	/**
	 *
	 */
	private static final long serialVersionUID = 7200878662560716216L;

	public AccountExistsException() {
		super("This account already exists.");
	}
}
