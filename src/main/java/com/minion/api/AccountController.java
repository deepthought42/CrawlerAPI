package com.minion.api;

import java.security.Principal;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.qanairy.models.repository.AccountRepository;
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
    private AccountRepository account_repo;
    
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
    	Account acct = account_repo.findByUsername(username);
    	
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

        // log username of user requesting account creation
        acct = account_repo.save(acct);
        
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
    	Account acct = account_repo.findByUserId(id);
    	
    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}
        List<String> onboarding = acct.getOnboardedSteps();
        onboarding.add(step_name);
        acct.setOnboardedSteps(onboarding);
        account_repo.save(acct);
        
        return acct.getOnboardedSteps();
    }
    
    @PreAuthorize("hasAuthority('read:accounts')")
    @RequestMapping(path ="/onboarding_steps_completed", method = RequestMethod.GET)
    public List<String> getOnboardingSteps(HttpServletRequest request) throws UnknownAccountException {
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account acct = account_repo.findByUserId(id);
    	
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
    	Account acct = account_repo.findByUserId(id);
    	
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
        return account_repo.save(account);
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
    	Account account = account_repo.findByUserId(id);
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
        account_repo.deleteAccountEdges(account.getUserId());
        account_repo.deleteAccount(account.getUserId());
    }
	
    @PreAuthorize("hasAuthority('read:accounts')")
	@RequestMapping(path ="/usage", method = RequestMethod.GET)
    public AccountUsage getUsageStats(HttpServletRequest request) throws UnknownAccountException{
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account acct = account_repo.findByUserId(id);
        if(acct == null){
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}
        
    	int monthly_test_count = 0;
    	//Check if account has exceeded test run limit
    	for(TestRecord record : acct.getTestRecords()){
    		Calendar cal = Calendar.getInstance(); 
    		cal.setTime(record.getRanAt()); 
    		int month_started = cal.get(Calendar.MONTH);
    		int year_started = cal.get(Calendar.YEAR);
   
    		Calendar c = Calendar.getInstance();
    		int month_now = c.get(Calendar.MONTH);
    		int year_now = c.get(Calendar.YEAR);

    		if(month_started == month_now && year_started == year_now){
    			monthly_test_count++;
    		}
    	}
    	int tests_used = acct.getTestRecords().size();
    	
    	int monthly_discovery_count = 0;
    	//check if account has exceeded allowed discovery threshold
    	for(DiscoveryRecord record : acct.getDiscoveryRecords()){
    		Calendar cal = Calendar.getInstance(); 
    		cal.setTime(record.getStartTime()); 
    		int month_started = cal.get(Calendar.MONTH);
    		int year_started = cal.get(Calendar.YEAR);
   
    		Calendar c = Calendar.getInstance();
    		int month_now = c.get(Calendar.MONTH);
    		int year_now = c.get(Calendar.YEAR);

    		if(month_started == month_now && year_started == year_now){
    			monthly_discovery_count++;
    		}
    	}
    	
        int discoveries_used = acct.getDiscoveryRecords().size();
        
        return new AccountUsage(discoveries_used, tests_used);
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