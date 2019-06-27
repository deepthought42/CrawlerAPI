package com.minion.api;

import java.security.Principal;
import java.util.Calendar;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.qanairy.api.exceptions.MissingSubscriptionException;
import com.qanairy.auth.Auth0ManagementApi;
import com.qanairy.config.WebSecurityConfig;
import com.qanairy.models.Account;
import com.qanairy.models.AccountUsage;
import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.StripeClient;
import com.qanairy.models.TestRecord;
import com.qanairy.models.dto.exceptions.UnknownAccountException;
import com.qanairy.services.AccountService;
import com.qanairy.services.DomainService;
import com.segment.analytics.Analytics;
import com.segment.analytics.messages.IdentifyMessage;
import com.segment.analytics.messages.TrackMessage;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.mashape.unirest.http.HttpResponse;

/**
 *	API for interacting with {@link User} data
 */
@RestController
@RequestMapping("/accounts")
public class AccountController {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected WebSecurityConfig appConfig;

    @Autowired
    private AccountService account_service;

    @Autowired
    private DomainService domain_service;

    private StripeClient stripeClient;

    @Autowired
    AccountController(StripeClient stripeClient) {
        this.stripeClient = stripeClient;
    }

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
    @CrossOrigin(origins = "138.91.154.99, 54.183.64.135, 54.67.77.38, 54.67.15.170, 54.183.204.205, 54.173.21.107, 54.85.173.28, 35.167.74.121, 35.160.3.103, 35.166.202.113, 52.14.40.253, 52.14.38.78, 52.14.17.114, 52.71.209.77, 34.195.142.251, 52.200.94.42", maxAge = 3600)
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<Account> create( @RequestParam(value="user_id", required=true) String user_id,
    										@RequestParam(value="username", required=true) String username)
    												throws Exception{
    	Account acct = account_service.findByUsername(username);

    	//create account
        if(acct != null){
        	throw new AccountExistsException();
        }

    	Map<String, Object> customerParams = new HashMap<String, Object>();
    	customerParams.put("description", "Customer for "+username);
    	Customer customer = this.stripeClient.createCustomer(null, username);
    	//Subscription subscription = this.stripeClient.subscribe(pro_tier, customer);
      acct = new Account(user_id, username, customer.getId(), "");
    	acct.setSubscriptionType("FREE");
    	acct.setApiToken(UUID.randomUUID().toString());
      acct.setSubscriptionType("FREE");

        //final String username = usernameService.getUsername();
        // log username of user requesting account creation
        acct = account_service.save(acct);

        Analytics analytics = Analytics.builder("TjYM56IfjHFutM7cAdAEQGGekDPN45jI").build();
    	Map<String, String> traits = new HashMap<String, String>();
        traits.put("email", username);
    	analytics.enqueue(IdentifyMessage.builder()
    		    .userId(acct.getUsername())
    		    .traits(traits)
    		);

    	Map<String, String> account_signup_properties = new HashMap<String, String>();
    	account_signup_properties.put("plan", "FREE");
    	analytics.enqueue(TrackMessage.builder("Signed Up")
    		    .userId(acct.getUsername())
    		    .properties(account_signup_properties)
    		);

        return ResponseEntity.accepted().body(acct);
    }

    @RequestMapping(path ="/onboarding_step", method = RequestMethod.POST)
    public List<String> setOnboardingStep(HttpServletRequest request, @RequestParam(value="step_name", required=true) String step_name) throws UnknownAccountException {
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
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

    @PreAuthorize("hasAuthority('read:accounts')")
    @RequestMapping(path ="/onboarding_steps_completed", method = RequestMethod.GET)
    public List<String> getOnboardingSteps(HttpServletRequest request) throws UnknownAccountException {
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
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
    @RequestMapping(path="/find", method = RequestMethod.GET)
    public Account get(HttpServletRequest request) throws UnknownAccountException {
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account acct = account_service.findByUserId(id);

        if(acct == null){
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
        logger.info("update invoked");
        return account_service.save(account);
    }

	@PreAuthorize("hasAuthority('update:accounts')")
    @RequestMapping(value ="/{id}/refreshToken", method = RequestMethod.PUT)
    public Account updateApiToken(final @PathVariable long id) throws AccountNotFoundException {
        logger.info("update invoked");
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
	 * @throws StripeException
	 */
	@PreAuthorize("hasAuthority('delete:accounts')")
    @RequestMapping(method = RequestMethod.DELETE)
    public void delete(HttpServletRequest request) throws UnirestException, StripeException{
		Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account account = account_service.findByUserId(id);
		//remove Auth0 account
    	HttpResponse<String> response = Auth0ManagementApi.deleteUser(account.getUserId());
    	//log.info("AUTH0 Response body      :::::::::::      "+response.getBody());
    	//log.info("AUTH0 Response status      :::::::::::      "+response.getStatus());
    	//log.info("AUTH0 Response status text      :::::::::::      "+response.getStatusText());


    	//remove stripe subscription
    	if(account.getSubscriptionToken() != null && !account.getSubscriptionToken().isEmpty()){
    		this.stripeClient.cancelSubscription(account.getSubscriptionToken());
    	}
    	if(account.getCustomerToken() != null && !account.getCustomerToken().isEmpty()){
    		this.stripeClient.deleteCustomer(account.getCustomerToken());
    	}
		//remove account
        account_service.deleteAccount(account.getUserId());
    }

    @PreAuthorize("hasAuthority('read:accounts')")
	@RequestMapping(path ="/usage", method = RequestMethod.GET)
    public AccountUsage getUsageStats(HttpServletRequest request,@RequestParam(value="domain_host", required=true) String domain_host) throws UnknownAccountException{
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account acct = account_service.findByUserId(id);
        if(acct == null){
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}

		Calendar c = Calendar.getInstance();
		int month_now = c.get(Calendar.MONTH);
		int year_now = c.get(Calendar.YEAR);

    	int monthly_test_count = 0;
    	for(TestRecord record : account_service.getTestRecords(acct.getUsername())){
    		Calendar cal = Calendar.getInstance();
    		cal.setTime(record.getRanAt());
    		int month_started = cal.get(Calendar.MONTH);
    		int year_started = cal.get(Calendar.YEAR);

    		if(month_started == month_now && year_started == year_now){
    			monthly_test_count++;
    		}
    	}

    	//get count of monthly tests discovered
    	int monthly_discovery_count = 0;
        int total_discovered_tests = 0;
        int domain_discovery_count = 0;
        int domain_total_discovered_tests = 0;
    	for(DiscoveryRecord record :  account_service.getDiscoveryRecordsByMonth(acct.getUsername(), month_now)){
    		Calendar cal = Calendar.getInstance();
    		cal.setTime(record.getStartTime());
    		int month_started = cal.get(Calendar.MONTH);
    		int year_started = cal.get(Calendar.YEAR);

    		if(month_started == month_now && year_started == year_now){
    			monthly_discovery_count++;
    			total_discovered_tests += record.getTestCount();
    		}

    		if(record.getDomainUrl().equals(domain_host)){
    			domain_discovery_count++;
    			domain_total_discovered_tests += record.getTestCount();
    		}
    	}

    	//calculate number of test records for domain
        int domain_tests_ran = 0;
    	for(TestRecord record : domain_service.getTestRecords(domain_host)){
    		Calendar cal = Calendar.getInstance();
    		cal.setTime(record.getRanAt());
    		int month_started = cal.get(Calendar.MONTH);
    		int year_started = cal.get(Calendar.YEAR);

    		if(month_started == month_now && year_started == year_now){
    			domain_tests_ran++;
    		}
    	}

    	DiscoveryRecord most_recent_discovery = domain_service.getMostRecentDiscoveryRecord(domain_host);

    	long discovery_run_time = System.currentTimeMillis() - most_recent_discovery.getStartTime().getTime();

        return new AccountUsage(monthly_discovery_count, monthly_test_count, total_discovered_tests,
        							domain_discovery_count, domain_tests_ran, domain_total_discovered_tests,
        							most_recent_discovery.getStartTime(), discovery_run_time, most_recent_discovery.getLastPathRanAt());
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
