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
import com.qanairy.models.PageElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.Test;
import com.qanairy.models.repository.AccountRepository;
import com.qanairy.models.repository.TestRepository;
import com.qanairy.services.AccountService;
import com.qanairy.services.DomainService;

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
	private TestRepository test_repo;

	@Autowired
	private AccountService account_service;

	@Autowired
	private DomainService domain_service;

	/**
     * Updates {@link Test} using an array of {@link JSONObject}s containing info for {@link PageState}s
     *  {@link PageElementState}s and {@link Action}s
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
     *  {@link PageElementState}s and {@link Action}s
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

    	if(test_json.has("key")){
    		String test_key = test_json.getString("key");
    	
	    	Test test = test_repo.findByKey(test_key);
	    	if(test != null){
	    		test.setArchived(true);
	    		test_repo.save(test);
	    	}
    	}
    	URL domain_url = new URL(test_json.getString("domain_url"));
    	String host = domain_url.getHost();
    	int dot_idx = host.indexOf('.');
    	int last_dot_idx = host.lastIndexOf('.');
    	String formatted_url = host;
    	if(dot_idx == last_dot_idx){
    		formatted_url = "www."+host;
    	}
    	Domain domain = domain_service.findByHost(formatted_url);
    	if(domain == null){
    		domain = new Domain(domain_url.getProtocol(), formatted_url,"chrome","");
    		domain = domain_service.save(domain);
    	}

    	Map<String, Object> options = new HashMap<String, Object>();
		options.put("browser", domain.getDiscoveryBrowserName());
		options.put("domain_key", domain.getKey());
		
		account_service.addDomainToAccount(acct, domain);

		Message<JSONObject> message = new Message<JSONObject>(acct.getUsername(), test_json, options);

		ActorRef testCreationActor = actor_system.actorOf(SpringExtProvider.get(actor_system)
				  .props("testCreationActor"), "test_creation_actor"+UUID.randomUUID());

		testCreationActor.tell(message, null);

    	return new ResponseEntity<>(Boolean.TRUE, HttpStatus.ACCEPTED );
	}
}
