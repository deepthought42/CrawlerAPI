package com.minion.api;

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
import com.qanairy.auth.Auth0Client;
import com.qanairy.auth.Auth0ManagementApi;
import com.qanairy.config.WebSecurityConfig;
import com.qanairy.models.Account;
import com.qanairy.models.QanairyUser;
import com.qanairy.models.dto.exceptions.UnknownAccountException;
import com.qanairy.services.AccountService;
import com.segment.analytics.Analytics;
import com.segment.analytics.messages.IdentifyMessage;
import com.segment.analytics.messages.TrackMessage;
import com.stripe.exception.APIConnectionException;
import com.stripe.exception.APIException;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.model.Customer;
import com.stripe.model.Plan;
import com.stripe.model.Subscription;
import com.mashape.unirest.http.HttpResponse;


/**
 *	API endpoints for interacting with {@link User} data
 */
@RestController
@RequestMapping("/accounts")
public class AccountController {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    @Autowired
    protected WebSecurityConfig appConfig;

    @Autowired
    protected AccountService account_service;

    /*@Autowired
    protected UsernameService usernameService;
    */
    
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
    public ResponseEntity<Account> create( @RequestParam(value="user_email", required=true) String username) 
    												throws Exception{        

    	//String auth_access_token = request.getHeader("Authorization").replace("Bearer ", "");
    	//Auth0Client auth = new Auth0Client();
    	//String username = auth.getUsername(auth_access_token);
    	Account acct = this.account_service.find(username);
    	
    	//create account
        if(acct != null){
        	throw new AccountExistsException();
        }
        
        String plan = "4-disc-10000-test-90-trial";
    	
    	Plan new_plan = Plan.retrieve(plan);

    	Map<String, Object> customerParams = new HashMap<String, Object>();
    	customerParams.put("description", "Customer for "+username);
    	Customer customer = this.stripeClient.createCustomer(null, username);
    	Subscription subscription = this.stripeClient.subscribe(new_plan, customer);
    	
    	System.err.println("Subscription :: "+subscription.toJson());
    	acct = new Account(username, plan, customer.getId(), subscription.getId());
        
        //Create user
        QanairyUser user = new QanairyUser(username);
        acct.addUser(user);

        // Connect to Auth0 API and update user metadata
        /*HttpResponse<String> api_resp = Auth0ManagementApi.updateUserAppMetadata(auth0Client.getUserId((Auth0JWTToken) principal), "{\"status\": \"account_owner\"}");
        if(api_resp.getStatus() != 200){
        	throw new Auth0ManagementApiException();
        }
        */
        //printGrantedAuthorities((Auth0JWTToken) principal);
        Account new_account = null;
        //final String username = usernameService.getUsername();
        // log username of user requesting account creation
        new_account = account_service.create(acct);
        
        
        Analytics analytics = Analytics.builder("TjYM56IfjHFutM7cAdAEQGGekDPN45jI").build();
    	Map<String, String> traits = new HashMap<String, String>();
        traits.put("email", username);        
    	analytics.enqueue(IdentifyMessage.builder()
    		    .userId(new_account.getKey())
    		    .traits(traits)
    		);
    	
    	Map<String, String> account_signup_properties = new HashMap<String, String>();
    	account_signup_properties.put("plan", new_account.getServicePackage());
    	analytics.enqueue(TrackMessage.builder("Signed Up")
    		    .userId(new_account.getKey())
    		    .properties(account_signup_properties)
    		);
    	
        return ResponseEntity.accepted().body(new_account);
    }

    @RequestMapping(path ="/onboarding_step", method = RequestMethod.POST)
    public List<String> setOnboardingStep(HttpServletRequest request, @RequestParam(value="step_name", required=true) String step_name) throws UnknownAccountException {
        String auth_access_token = request.getHeader("Authorization").replace("Bearer ", "");
    	Auth0Client auth = new Auth0Client();
    	String username = auth.getUsername(auth_access_token);
        Account acct = this.account_service.find(username);
        if(acct == null){
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}
        
        acct.addOnboardingStep(step_name);
        acct = account_service.save(acct);
        
        return acct.getOnboardedSteps();
    }
    
    @PreAuthorize("hasAuthority('read:accounts')")
    @RequestMapping(path ="/onboarding_steps_completed", method = RequestMethod.GET)
    public List<String> getOnboardingSteps(HttpServletRequest request) throws UnknownAccountException {
        logger.info("get invoked");
        String auth_access_token = request.getHeader("Authorization").replace("Bearer ", "");
    	Auth0Client auth = new Auth0Client();
    	String username = auth.getUsername(auth_access_token);
        Account acct = this.account_service.find(username);
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
     */
    @PreAuthorize("hasAuthority('read:accounts')")
    @RequestMapping(value ="/{id}", method = RequestMethod.GET)
    public Account get(final @PathVariable String key) {
        logger.info("get invoked");
        return account_service.get(key);
    }

	@PreAuthorize("hasAuthority('update:accounts')")
    @RequestMapping(value ="/{id}", method = RequestMethod.PUT)
    public Account update(final @PathVariable String key, 
    					  final @Validated @RequestBody Account account) {
        logger.info("update invoked");
        return account_service.update(account);
    }
    
	@PreAuthorize("hasAuthority('delete:accounts')")
    @RequestMapping(method = RequestMethod.DELETE)
    public void delete(HttpServletRequest request) throws UnirestException, AuthenticationException, InvalidRequestException, APIConnectionException, CardException, APIException {
		String auth_access_token = request.getHeader("Authorization").replace("Bearer ", "");
    	Auth0Client auth = new Auth0Client();
    	String username = auth.getUsername(auth_access_token);
    	Account account = this.account_service.find(username);
    					
		//remove Auth0 account
    	HttpResponse<String> response = Auth0ManagementApi.deleteUser(auth.getUserId(auth_access_token));
    	System.err.println("AUTH0 Response body      :::::::::::      "+response.getBody());
    	System.err.println("AUTH0 Response status      :::::::::::      "+response.getStatus());
    	System.err.println("AUTH0 Response status text      :::::::::::      "+response.getStatusText());
    	
    	//remove stripe subscription
        this.stripeClient.cancelSubscription(account.getSubscriptionToken());
        this.stripeClient.deleteCustomer(account.getCustomerToken());
        
		//remove account
		account_service.delete(account);
        logger.info("update invoked");
    }
    /**
     * Simple demonstration of how Principal info can be accessed
     * 
     * @param principal
     */
    /*private void printGrantedAuthorities(final Auth0JWTToken principal) {
        for(final GrantedAuthority grantedAuthority: principal.getAuthorities()) {
            final String authority = grantedAuthority.getAuthority();
            logger.info(authority);
        }
    }
    */
}

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
class AccountExistsException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7200878662560716216L;

	public AccountExistsException() {
		super("Account already exists");
	}
}