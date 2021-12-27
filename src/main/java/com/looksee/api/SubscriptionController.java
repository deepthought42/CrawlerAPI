package com.looksee.api;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.JsonSyntaxException;
import com.looksee.models.Account;
import com.looksee.models.dto.Subscription;
import com.looksee.models.pricing.StripeCheckoutSession;
import com.looksee.services.AccountService;
import com.looksee.services.StripeService;
import com.looksee.services.SubscriptionService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.ApiResource;
import com.stripe.net.Webhook;

@RestController
@RequestMapping("/subscribe")
public class SubscriptionController {
	private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private AccountService account_service;
    
    @Autowired
    private StripeService stripe_service;
    
    @Autowired
    private SubscriptionService subscription_service;
    
    /**
     * Cancels any existing subscription and sets account subscription to Free
     * 
     * @param request
     * @param plan
     * 
     * @throws Exception
     */
    @RequestMapping(method = RequestMethod.PUT)
    public void getFreeSubscription(HttpServletRequest request,
					 		@RequestBody Subscription subscription) throws Exception {
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account acct = account_service.findByUserId(id);
    	if(acct.getSubscriptionToken() != null && !acct.getSubscriptionToken().isEmpty()) {
    		stripe_service.cancelSubscription(acct.getSubscriptionToken());
    	}
    	acct.setSubscriptionToken("");
    	acct.setSubscriptionType("Free");
    	account_service.save(acct);
    }
    
    /**
     * Creates checkout session. This endpoint is authenticated and is only called 
     * after a user creates an account
     * 
     * @param request
     * @param subscription
     * 
     * @throws Exception
     */
    @RequestMapping(method = RequestMethod.POST)
    public StripeCheckoutSession createCheckoutSession(
    						HttpServletRequest request,
					 		@RequestBody Subscription subscription
	) throws Exception {
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account acct = account_service.findByUserId(id);
    	
    	if(acct.getCustomerToken() == null || acct.getCustomerToken().isEmpty()) {
    		acct.setCustomerToken(stripe_service.createCustomer(null, acct.getEmail()).getId());
    		acct = account_service.save(acct);
    	}
    	
    	Session session = subscription_service.createCheckoutSession(subscription.getPriceId(), 
    																 acct.getCustomerToken(), 
    																 acct.getEmail());
    	return new StripeCheckoutSession(session.getUrl());
    }
    
    /**
     * Webhook endpoint registered with Stripe. This webhook validates the event signature so that only
     * Stripe can access it and we use it to manage payment issues
     * 
     * @param request
     * @param response
     * @param request_body
     * @return
     * @throws Exception 
     */
    @RequestMapping(path="/stripe_webhook", method = RequestMethod.POST)
    public String handleWebhook(HttpServletRequest request, 
    							HttpServletResponse response,
    				    		@RequestBody String request_body
	) throws Exception {
        String payload = request_body;
        String sigHeader = request.getHeader("Stripe-Signature");
        String endpointSecret = "whsec_sEiL15qA9qMKulNQg8rCRZK3BrRoC1fK";

        Event event = null;

        try {
        	event = ApiResource.GSON.fromJson(payload, Event.class);
            //event = Webhook.constructEvent(payload.toString(), sigHeader, endpointSecret);
        } catch (JsonSyntaxException e) {
            // Invalid signature
            response.setStatus(400);
            return "";
        }
        
        if(endpointSecret != null && sigHeader != null) {
            // Only verify the event if you have an endpoint secret defined.
            // Otherwise use the basic event deserialized with GSON.
            try {
                event = Webhook.constructEvent(
                    payload, sigHeader, endpointSecret
                );
            } catch (SignatureVerificationException e) {
                // Invalid signature
                log.warn("⚠️  Webhook error while validating signature.");
                response.setStatus(400);
                return "";
            }
        }

        // Deserialize the nested object inside the event
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject = null;
        if (dataObjectDeserializer.getObject().isPresent()) {
            stripeObject = dataObjectDeserializer.getObject().get();
        } else {
            // Deserialization failed, probably due to an API version mismatch.
            // Refer to the Javadoc documentation on `EventDataObjectDeserializer` for
            // instructions on how to handle this case, or return an error here.
        }
        
        switch (event.getType()) {
          case "checkout.session.async_payment_failed":
        	  log.warn("checkout session completed webhook recieved");
        	  Session session = (Session) stripeObject;
        	  // Then define and call a function to handle the event checkout.session.async_payment_failed
        	  break;
          case "checkout.session.async_payment_succeeded":
        	  Session session1 = (Session) stripeObject;
        	  // Then define and call a function to handle the event checkout.session.async_payment_succeeded
        	  break;
          case "checkout.session.completed":
        	  Session session2 = (Session) stripeObject;
        	  log.warn("checkout session completed webhook recieved : "+session2.getCustomer());
        	  Account account = account_service.findByEmail(session2.getCustomerDetails().getEmail());
        	  subscription_service.changeSubscription(account, session2.getSubscription());

        	  // Then define and call a function to handle the event checkout.session.completed
        	  break;
          case "checkout.session.expired":
        	  Object session3 = event.getData().getObject();
        	  // Then define and call a function to handle the event checkout.session.expired
        	  break;
          default:
            // System.out.println("Unhandled event type: " + event.getType());
        }

        response.setStatus(200);
        return "";
    }
}

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
class UnknownSubscriptionPlanException extends RuntimeException {
	private static final long serialVersionUID = 7200878662560715915L;

	public UnknownSubscriptionPlanException() {
		super("Could not find the requested plan.");
	}
}