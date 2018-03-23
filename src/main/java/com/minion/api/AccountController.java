package com.minion.api;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.qanairy.auth.Auth0Client;
import com.qanairy.config.WebSecurityConfig;
import com.qanairy.models.Account;
import com.qanairy.models.QanairyUser;
import com.qanairy.services.AccountService;
import com.segment.analytics.Analytics;
import com.segment.analytics.messages.IdentifyMessage;
import com.segment.analytics.messages.TrackMessage;
import com.stripe.model.Customer;
import com.stripe.model.Subscription;

/**
 *	API endpoints for interacting with {@link User} data
 */
@RestController
@RequestMapping("/accounts")
public class AccountController {
	private static Logger log = LoggerFactory.getLogger(AccountController.class);

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    @Autowired
    protected WebSecurityConfig appConfig;

    @Autowired
    protected AccountService accountService;

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
    @PreAuthorize("hasAuthority('create:accounts')")
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<Account> create(HttpServletRequest request, 
    										@RequestParam(value="token") String token) 
    												throws Exception{        

    	String auth_access_token = request.getHeader("Authorization").replace("Bearer ", "");
    	Auth0Client auth = new Auth0Client();
    	String username = auth.getUsername(auth_access_token);
    	String plan = "4-disc-10000-test";
    	
    	Map<String, Object> customerParams = new HashMap<String, Object>();
    	customerParams.put("description", "Customer for "+username);
    	//customerParams.put("source", token);
    	// ^ obtained with Stripe.js
    	//Customer customer = Customer.create(customerParams);
    	Customer customer = this.stripeClient.createCustomer(token, username);
    	Subscription subscription = this.stripeClient.subscribe(plan, customer.getId());
    	System.err.println("Subscription :: "+subscription.toJson());
    	
    	//create account
        Account acct = new Account(username, plan, customer.getId());
        
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
        new_account = accountService.create(acct);
        
        
        Analytics analytics = Analytics.builder("TjYM56IfjHFutM7cAdAEQGGekDPN45jI").build();
    	Map<String, String> traits = new HashMap<String, String>();
        traits.put("name", auth.getNickname(auth_access_token));
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
        return accountService.get(key);
    }

	@PreAuthorize("hasAuthority('user') or hasAuthority('qanairy')")
    @RequestMapping(value ="/{id}", method = RequestMethod.PUT)
    public Account update(final @PathVariable String key, 
    					  final @Validated @RequestBody Account account) {
        logger.info("update invoked");
        return accountService.update(account);
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
