package com.minion.api;

import static com.qanairy.config.SpringExtension.SpringExtProvider;

import java.net.URL;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.minion.structs.Message;
import com.qanairy.models.Account;
import com.qanairy.models.Action;
import com.qanairy.models.Domain;
import com.qanairy.models.PageElement;
import com.qanairy.models.PageState;
import com.qanairy.models.Test;
import com.qanairy.models.repository.AccountRepository;
import com.qanairy.models.repository.DomainRepository;
import com.qanairy.services.BrowserService;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

/**
 *	API for interacting with {@link User} data
 */
@Controller
@RequestMapping("/testIDE")
public class IdeTestExportController {
	@SuppressWarnings("unused")
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
    private ActorSystem actor_system;
   
	@Autowired
	private AccountRepository account_repo;
	
	@Autowired
	private DomainRepository domain_repo;
	
	@Autowired
	private BrowserService browser_service;
    
	/**
     * Updates {@link Test} using an array of {@link JSONObject}s containing info for {@link PageState}s
     *  {@link PageElement}s and {@link Action}s
	 * 
	 * @param json_str JSON String
	 * 
	 * @return A boolean value indicating that the system successfully created a {@link Test} using the provided JSON
	 * 
	 * @throws Exception
	 */
    @RequestMapping(method = RequestMethod.PUT)
    public ResponseEntity<Boolean> update(HttpServletRequest request,
    									  @RequestBody(required=true) String json_str) 
    										throws Exception {
    
    	return new ResponseEntity<>(Boolean.TRUE, HttpStatus.ACCEPTED );
    }
    
    /**
     * Contructs a new {@link Test} using an array of {@link JSONObject}s containing info for {@link PageState}s
     *  {@link PageElement}s and {@link Action}s
	 * 
	 * @param json_str JSON String
	 * 
	 * @return A boolean value indicating that the system successfully created a {@link Test} using the provided JSON
	 * 
	 * @throws Exception
	 */
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<Boolean> create(HttpServletRequest request,
    									  @RequestBody(required=true) String json_str) 
    										throws Exception {
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account acct = account_repo.findByUserId(id);

    	JSONObject test_json = new JSONObject(json_str);

    	String domain_url = test_json.getString("domain_url");
    	Domain domain = domain_repo.findByHost(new URL(domain_url).getHost());
    	
    	Map<String, Object> options = new HashMap<String, Object>();
		options.put("browser", domain.getDiscoveryBrowserName());
    	

		Message<JSONObject> message = new Message<JSONObject>(acct.getUsername(), test_json, options);

		ActorRef testCreationActor = actor_system.actorOf(SpringExtProvider.get(actor_system)
				  .props("testCreationActor"), "test_creation_actor"+UUID.randomUUID());
		
		testCreationActor.tell(message, null);
		
    	return new ResponseEntity<>(Boolean.TRUE, HttpStatus.ACCEPTED );
	}
}