package com.minion.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;

import com.minion.actors.MemoryRegistryActor;
import com.minion.actors.TestingActor;
import com.qanairy.api.exceptions.DomainNotOwnedByAccountException;
import com.qanairy.api.exceptions.FreeTrialExpiredException;
import com.qanairy.api.exceptions.MissingSubscriptionException;
import com.qanairy.models.dto.exceptions.UnknownAccountException;
import com.qanairy.models.repository.AccountRepository;
import com.qanairy.models.repository.DomainRepository;
import com.qanairy.models.repository.GroupRepository;
import com.qanairy.models.repository.TestRecordRepository;
import com.qanairy.models.repository.TestRepository;
import com.segment.analytics.Analytics;
import com.segment.analytics.messages.IdentifyMessage;
import com.segment.analytics.messages.TrackMessage;
import com.stripe.exception.APIConnectionException;
import com.stripe.exception.APIException;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.UsageRecord;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

import com.minion.browsing.Browser;
import com.minion.structs.Message;
import com.qanairy.auth.Auth0Client;
import com.qanairy.models.Account;
import com.qanairy.models.Domain;
import com.qanairy.models.Group;
import com.qanairy.models.StripeClient;
import com.qanairy.models.Test;
import com.qanairy.models.TestRecord;

/**
 * REST controller that defines endpoints to access tests
 */
@Controller
@RequestMapping("/tests")
public class TestController {
	private static Logger log = LoggerFactory.getLogger(TestController.class);

    private StripeClient stripeClient;

    @Autowired
    DomainRepository domain_repo;
    
    @Autowired
    AccountRepository account_repo;
    
    @Autowired
    TestRepository test_repo;
    
    @Autowired
    TestRecordRepository test_record_repo;
    
    @Autowired
    GroupRepository group_repo;
    
    @Autowired
    TestController(StripeClient stripeClient) {
        this.stripeClient = stripeClient;
    }
    
	/**
	 * Retrieves list of all tests from the database 
	 * @param url
	 * 
	 * @return list of tests previously discovered for given url
	 * @throws UnknownAccountException 
	 * @throws DomainNotOwnedByAccountException 
	 */
    @PreAuthorize("hasAuthority('read:tests')")
	@RequestMapping(method = RequestMethod.GET)
	public @ResponseBody List<Test> getTestByDomain(HttpServletRequest request, 
													@RequestParam(value="url", required=true) String url) 
															throws UnknownAccountException, DomainNotOwnedByAccountException {    	
		Domain domain = domain_repo.findByUrl(url);
		Iterator<Test> tests = domain.getTests().iterator();
		List<Test> verified_tests = new ArrayList<Test>();
		
		while(tests.hasNext()){
			Test test = tests.next();
			if(test.getCorrect() != null){
				verified_tests.add(test_repo.findByKey(test.getKey()));
			}
		}
		
		return verified_tests;
    }

    /**
	 * Retrieves list of all tests from the database 
	 * @param url
	 * 
	 * @return list of tests previously discovered for given url
	 * @throws UnknownAccountException 
	 * @throws DomainNotOwnedByAccountException 
	 */
    @PreAuthorize("hasAuthority('read:tests')")
	@RequestMapping(path="/failing", method = RequestMethod.GET)
	public @ResponseBody Map<String, Integer> getFailingTestByDomain(HttpServletRequest request, 
			   								 	 	@RequestParam(value="url", required=true) String url) 
			   										 throws UnknownAccountException, DomainNotOwnedByAccountException {    	
		int failed_tests = 0;
		Domain idomain = domain_repo.findByUrl(url);
		try{
			Iterator<Test> tests = idomain.getTests().iterator();
			
			while(tests.hasNext()){
				Test itest = tests.next();
				if(itest.getCorrect() != null && itest.getCorrect() == false){
					failed_tests++;
				}
			}
		}catch(NullPointerException e){
			log.error("Null pointer exception occurred while getting failing test count", e.getMessage());
		}
				
        Map<String, Integer> result = new HashMap<String, Integer>();
        result.put("failing", failed_tests);
		return result;
    }

	/**
	 * Retrieves list of all tests from the database 
	 * 
	 * @param name test name to lookup
	 * 
	 * @return all tests matching name passed
	 */
    @PreAuthorize("hasAuthority('read:tests')")
	@RequestMapping(path="/name", method = RequestMethod.GET)
	public @ResponseBody Test getTestsByName(HttpSession session, HttpServletRequest request, 
			   								 		@RequestParam(value="name", required=true) String name) {
		Test test = test_repo.findByName(name);		
		return test;
	}
	
	/**
	 * Retrieves list of all tests from the database 
	 * 
	 * @param name test name to lookup
	 * 
	 * @return all tests matching name passed
	 * @throws DomainNotOwnedByAccountException 
	 * @throws UnknownAccountException 
	 */
    @PreAuthorize("hasAuthority('read:tests')")
	@RequestMapping(path="/unverified", method = RequestMethod.GET)
	public @ResponseBody List<Test> getUnverifiedTests(HttpServletRequest request, 
														@RequestParam(value="url", required=true) String url) 
																throws DomainNotOwnedByAccountException, UnknownAccountException {
    	Date start = new Date();
   		Domain domain = domain_repo.findByUrl(url);
		
		List<Test> tests = domain.getTests();
		List<Test> unverified_tests = new ArrayList<Test>();

		for(Test test : tests){
			if(test.getCorrect() == null){
				unverified_tests.add(test_repo.findByKey(test.getKey()));
			}
		}
    	
    	Date end = new Date();
    	long diff = end.getTime() - start.getTime();
    	log.info("UNVERIFIED TESTS LOADED IN " + diff + " milliseconds");
		return unverified_tests;
	}

	/**
	 * Updates the correctness of a test for a specific browser with the given test key
	 * 
	 * @param test
	 * @return
	 * @throws UnknownAccountException 
	 */
    @PreAuthorize("hasAuthority('update:tests')")
	@RequestMapping(path="/setPassingStatus", method=RequestMethod.PUT)
	public @ResponseBody Test setPassingStatus(HttpServletRequest request, 
													@RequestParam(value="key", required=true) String key, 
													@RequestParam(value="browser", required=true) String browser_name,
													@RequestParam(value="correct", required=true) boolean correct)
															throws UnknownAccountException{
    	
    	//make sure domain belongs to user account first
    	String auth_access_token = request.getHeader("Authorization").replace("Bearer ", "");
    	Auth0Client auth = new Auth0Client();
    	String username = auth.getUsername(auth_access_token);

    	Account acct = account_repo.findByUsername(username);
    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}
    	
    	Analytics analytics = Analytics.builder("TjYM56IfjHFutM7cAdAEQGGekDPN45jI").build();
    	Map<String, String> traits = new HashMap<String, String>();
        traits.put("name", auth.getNickname(auth_access_token));
        traits.put("email", username);        
    	analytics.enqueue(IdentifyMessage.builder()
    		    .userId(acct.getKey())
    		    .traits(traits)
    		);
    	
		Test test = test_repo.findByKey(key);
		Map<String, Boolean> browser_statuses = test.getBrowserStatuses();
		browser_statuses.put(browser_name, correct);
		test.setCorrect(correct);
		test.setBrowserStatuses(browser_statuses);
		
		//update last TestRecord passes value
		updateLastTestRecordPassingStatus(test);
		
	   	//Fire discovery started event	
	   	Map<String, String> set_initial_correctness_props= new HashMap<String, String>();
	   	set_initial_correctness_props.put("test_key", test.getKey());
	   	analytics.enqueue(TrackMessage.builder("Set initial test status")
	   		    .userId(acct.getKey())
	   		    .properties(set_initial_correctness_props)
	   		);
   	
		return test;
	}

    /**
     * 
     * @param itest
     */
	private void updateLastTestRecordPassingStatus(Test test) {
		Iterator<TestRecord> test_records = test.getRecords().iterator();
		TestRecord record = null;
		while(test_records.hasNext()){
			record = test_records.next();
		}
		if(record != null){
			record.setPassing(test.getCorrect());
		}
	}

	/**
	 * Updates a test
	 * 
	 * @param test
	 * @return
	 */
    @PreAuthorize("hasAuthority('update:tests')")
	@RequestMapping(method=RequestMethod.PUT)
	public @ResponseBody void update(HttpServletRequest request,
									@RequestParam(value="key", required=true) String key, 
									@RequestParam(value="name", required=true) String name, 
									@RequestParam(value="firefox", required=false) Boolean firefox,
									@RequestParam(value="chrome", required=false) Boolean chrome){
		Test test = test_repo.findByKey(key);
		
		Map<String, Boolean> browser_statuses = new HashMap<String, Boolean>();
		browser_statuses.put("firefox", firefox);
		browser_statuses.put("chrome", chrome);
		
		if(test != null){
			test.setName(name);
			test.setBrowserStatuses(browser_statuses);
		}
	}

    
	/**
	 * Updates the correctness of a test with the given test key
	 * 
	 * @param test
	 * @return
	 */
    @PreAuthorize("hasAuthority('update:tests')")
	@RequestMapping(path="/updateName/{key}", method=RequestMethod.PUT)
	public @ResponseBody Test updateName(HttpServletRequest request, 
										 @PathVariable(value="key", required=true) String key, 
										 @RequestParam(value="name", required=true) String name){
		Test test = test_repo.findByKey(key);
		test.setName(name);

		return test_repo.save(test);
	}
    
    /**
	 * Runs test with a given key
	 * 
	 * @param key
	 * @return
	 * @throws MalformedURLException 
     * @throws UnknownAccountException 
     * @throws APIException 
     * @throws CardException 
     * @throws APIConnectionException 
     * @throws InvalidRequestException 
     * @throws AuthenticationException 
	 */
    @PreAuthorize("hasAuthority('run:tests')")
	@RequestMapping(path="/run", method = RequestMethod.POST)
	public @ResponseBody Map<String, TestRecord> runTests(HttpServletRequest request,
														  @RequestParam(value="test_keys", required=true) List<String> test_keys, 
														  @RequestParam(value="browser_name", required=true) String browser_name) 
																  throws MalformedURLException, UnknownAccountException, AuthenticationException, InvalidRequestException, APIConnectionException, CardException, APIException{
    	
    	String auth_access_token = request.getHeader("Authorization").replace("Bearer ", "");
    	Auth0Client auth = new Auth0Client();
    	String username = auth.getUsername(auth_access_token);

    	Account acct = account_repo.findByUsername(username);
    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}
    	
    	Subscription subscription = stripeClient.getSubscription(acct.getSubscriptionToken());
    	String subscription_item = null;
    	for(SubscriptionItem item : subscription.getSubscriptionItems().getData()){
    		if(item.getPlan().getNickname().equals("test_runs")){
    			subscription_item = item.getId();
    		}
    	}
    	
    	if(subscription_item==null){
    		throw new MissingDiscoveryPlanException();
    	}
    	
    	if(subscription.getTrialEnd() < (new Date()).getTime()/1000){
    		throw new FreeTrialExpiredException();
    	}
    	    	    	
    	Analytics analytics = Analytics.builder("TjYM56IfjHFutM7cAdAEQGGekDPN45jI").build();
    	Map<String, String> traits = new HashMap<String, String>();
        traits.put("name", auth.getNickname(auth_access_token));
        traits.put("email", username);        
    	analytics.enqueue(IdentifyMessage.builder()
    		    .userId(acct.getKey())
    		    .traits(traits)
    		);
    	
    	//Fire discovery started event	
	   	Map<String, String> run_test_batch_props= new HashMap<String, String>();
	   	run_test_batch_props.put("total tests", Integer.toString(test_keys.size()));
	   	analytics.enqueue(TrackMessage.builder("Running tests")
	   		    .userId(acct.getKey())
	   		    .properties(run_test_batch_props)
	   		);
	   	
    	Map<String, TestRecord> test_results = new HashMap<String, TestRecord>();
    	
    	for(String key : test_keys){
    		Test test = test_repo.findByKey(key);
    		
    		TestRecord record = null;
    		Date date = new Date();
			long date_millis = date.getTime();
			Map<String, Object> usageRecordParams = new HashMap<String, Object>();
	    	usageRecordParams.put("quantity", 1);
	    	usageRecordParams.put("timestamp", date_millis/1000);
	    	usageRecordParams.put("subscription_item", subscription_item);
	    	usageRecordParams.put("action", "increment");

	    	UsageRecord.create(usageRecordParams, null);

			Map<String, Boolean> browser_running_status = test.getBrowserStatuses();
			browser_running_status.put(browser_name, null);

			test.setBrowserStatuses(browser_running_status);
			
			Map<String, Object> options = new HashMap<String, Object>();
			options.put("host", test.firstPage().getUrl().getHost());
			Message<Test> test_msg = new Message<Test>(acct.getKey(), test, options);
			System.err.println("Test message created for host :: "+options.get("host"));
			//tell memory worker of test
			ActorSystem actor_system = ActorSystem.create("MinionActorSystem");
			final ActorRef memory_actor = actor_system.actorOf(Props.create(MemoryRegistryActor.class), "MemoryRegistration"+UUID.randomUUID());
			memory_actor.tell(test_msg, null);
			
			Browser browser = new Browser(browser_name.trim());
			record = TestingActor.runTest(test, browser);
			browser.close();
			
			System.err.println("TEST RUN RECORD PASSING STATUS  ??????     "+record.getPassing());
			record = test_record_repo.save(record);
			acct.addTestRecord(record);
			account_repo.save(acct);
			
    		test = test_repo.findByKey(key);
    		
			test_results.put(test.getKey(), record);
    		boolean is_passing = true;
			//update overall passing status based on all browser passing statuses
			for(Boolean status : test.getBrowserStatuses().values()){
				if(status != null && !status){
					is_passing = false;
					break;
				}
			}
			Map<String, Boolean> browser_statuses = test.getBrowserStatuses();
			browser_statuses.put(browser_name, is_passing);
			
			test.addRecord(record);
			test.setCorrect(is_passing);
			test.setLastRunTimestamp(new Date());
			test.setRunTime(record.getRunTime());
			test.setBrowserStatuses(browser_statuses);
			
			//tell memory worker of test
			test_msg = new Message<Test>(acct.getKey(), test, options);
			memory_actor.tell(test_msg, null);
   		}
		
		return test_results;
	}

	/**
	 * Finds all tests by group and runs them
	 * 
	 * @param group name of group that is associated with tests that should be ran
	 * @param url domain that group should be ran for
	 * 
	 * @return {@link TestRecord records} that define the results of the tests. 
	 */
    @PreAuthorize("hasAuthority('run:tests')")
	@RequestMapping(path="/runTestGroup/{group}", method = RequestMethod.POST)
	public @ResponseBody List<TestRecord> runTestByGroup(@PathVariable("group") String group,
														@RequestParam(value="url", required=true) String url,
														@RequestParam(value="browser_name", required=true) String browser_name){		
		List<Test> test_list = new ArrayList<Test>();
		List<Test> group_list = new ArrayList<Test>();
		test_list = test_repo.findByUrl(url);
		
		for(Test test : test_list){
			if(test.getGroups() != null && test.getGroups().contains(group)){
				group_list.add(test);
			}
		}
		
		List<TestRecord> group_records = new ArrayList<TestRecord>();

		for(Test group_test : group_list){
			Browser browser;
			try {
				browser = new Browser(browser_name);
				TestRecord record = TestingActor.runTest(group_test, browser);
				group_records.add(record);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return group_records;
	}
	
	/**
	 * Adds given group to test with given key
	 * 
	 * @param group String representing name of group to add to test
	 * @param test_key key for test that will have group added to it
	 * 	
	 * @return the updated test
	 */
    @PreAuthorize("hasAuthority('create:groups')")
	@RequestMapping(path="/addGroup", method = RequestMethod.POST)
	public @ResponseBody Group addGroup(@RequestParam(value="name", required=true) String name,
										@RequestParam(value="description", required=true) String description,
										@RequestParam(value="key", required=true) String key){
    	if(name == null || name.isEmpty()){
    		throw new EmptyGroupNameException();
    	}
		Group group = new Group(name.toLowerCase(), description);
		Test test = test_repo.findByKey(key);
		
		group = group_repo.save(group);
		test.addGroup(group);
		
		return group;
	}

    /**
	 * Adds given group to test with given key
	 * 
	 * @param group_key String key representing group to add to test
	 * @param test_key key for test that will have group added to it
	 * 	
	 * @return the updated test
	 */
    @PreAuthorize("hasAuthority('delete:groups')")
	@RequestMapping(path="/remove/group", method = RequestMethod.POST)
	public @ResponseBody void removeGroup(@RequestParam(value="group_key", required=true) String group_key,
										  	 @RequestParam(value="test_key", required=true) String test_key){
		Test test = test_repo.findByKey(test_key);
		Group group = group_repo.findByKey(group_key);
		test.removeGroup(group);
	}

	/**
	 * Retrieves list of all tests from the database
	 * 
	 * @param url
	 * 
	 * @return
	 */
    @PreAuthorize("hasAuthority('read:groups')")
	@RequestMapping(path="/groups", method = RequestMethod.GET)
	public @ResponseBody List<Group> getGroups(HttpServletRequest request, 
			   								   @RequestParam(value="url", required=true) String url) {
		List<Group> groups = new ArrayList<Group>();
		List<Test> test_list = test_repo.findByUrl(url);
		
		for(Test test : test_list){
			if(test.getGroups() != null){
				groups.addAll(test.getGroups());
			}
		}

		return groups;
	}
}

@ResponseStatus(HttpStatus.SEE_OTHER)
class TestAlreadyRunningException extends RuntimeException {
	private static final long serialVersionUID = 7200878662560716215L;

	public TestAlreadyRunningException() {
		super("This test is already running. Please wait for it to return.");
	}
}

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
class EmptyGroupNameException extends RuntimeException {
	private static final long serialVersionUID = 7200878662560716215L;

	public EmptyGroupNameException() {
		super("Groups must have a name.");
	}
}

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
class TestLimitReachedException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7200878662560716216L;

	public TestLimitReachedException() {
		super("You reached your test run limit. Upgrade your account now!");
	}
}