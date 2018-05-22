package com.minion.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Calendar;
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
import com.qanairy.persistence.Account;
import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.Domain;
import com.qanairy.persistence.Group;
import com.qanairy.persistence.OrientConnectionFactory;
import com.qanairy.persistence.Test;
import com.qanairy.persistence.TestRecord;
import com.qanairy.services.AccountService;
import com.segment.analytics.Analytics;
import com.segment.analytics.messages.IdentifyMessage;
import com.segment.analytics.messages.TrackMessage;
import com.stripe.exception.APIConnectionException;
import com.stripe.exception.APIException;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.model.Plan;
import com.stripe.model.Subscription;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

import com.minion.browsing.Browser;
import com.minion.structs.Message;
import com.qanairy.auth.Auth0Client;
import com.qanairy.models.GroupPOJO;
import com.qanairy.models.StripeClient;
import com.qanairy.models.dao.DomainDao;
import com.qanairy.models.dao.GroupDao;
import com.qanairy.models.dao.TestDao;
import com.qanairy.models.dao.impl.DomainDaoImpl;
import com.qanairy.models.dao.impl.GroupDaoImpl;
import com.qanairy.models.dao.impl.TestDaoImpl;

/**
 * REST controller that defines endpoints to access tests
 */
@Controller
@RequestMapping("/tests")
public class TestController {
	private static Logger log = LoggerFactory.getLogger(TestController.class);

    private StripeClient stripeClient;

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
		DomainDao domain_repo = new DomainDaoImpl();
    	
		Domain idomain = domain_repo.find(url);
		Iterator<Test> tests = idomain.getTests().iterator();
		TestDao test_dao = new TestDaoImpl();
		List<Test> verified_tests = new ArrayList<Test>();
		
		while(tests.hasNext()){
			Test test = tests.next();
			if(test.getCorrect() != null){
				verified_tests.add(test_dao.find(test.getKey()));
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
		DomainDao domain_repo = new DomainDaoImpl();
    	
		int failed_tests = 0;
		Domain idomain = domain_repo.find(url);
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
		TestDao test_dao = new TestDaoImpl();
		Test test = test_dao.findByName(name);
		
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
    	
		DomainDao domain_repo = new DomainDaoImpl();
		Domain idomain = domain_repo.find(url);

		Iterator<Test> tests = idomain.getTests().iterator();
		TestDao test_repo = new TestDaoImpl();
		List<Test> unverified_tests = new ArrayList<Test>();
		while(tests.hasNext()){
			Test itest = tests.next();
			if(itest.getCorrect() == null){
				unverified_tests.add(test_repo.find(itest.getKey()));
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
	@RequestMapping(path="/setDiscoveredPassingStatus", method=RequestMethod.PUT)
	public @ResponseBody Test setInitialCorrectness(HttpServletRequest request, 
													@RequestParam(value="key", required=true) String key, 
													@RequestParam(value="browser", required=true) String browser_name,
													@RequestParam(value="correct", required=true) boolean correct)
															throws UnknownAccountException{
    	
    	//make sure domain belongs to user account first
    	String auth_access_token = request.getHeader("Authorization").replace("Bearer ", "");
    	Auth0Client auth = new Auth0Client();
    	String username = auth.getUsername(auth_access_token);

    	Account acct = AccountService.find(username);
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
    	
		TestDao test_dao = new TestDaoImpl();
		Test test = test_dao.find(key);
		test.setCorrect(correct);
		
		Map<String, Boolean> browser_statuses = test.getBrowserStatuses();
		browser_statuses.put(browser_name, correct);
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
	private void updateLastTestRecordPassingStatus(Test itest) {
		Iterator<TestRecord> itest_records = itest.getRecords().iterator();
		TestRecord record = null;
		while(itest_records.hasNext()){
			record = itest_records.next();
		}
		if(record != null){
			record.setPassing(itest.getCorrect());
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
										@RequestBody(required=true) Test test){
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		@SuppressWarnings("unchecked")
		Iterable<Test> tests = (Iterable<Test>) DataAccessObject.findByKey(test.getKey(), orient_connection, Test.class);
		Iterator<Test> iter = tests.iterator();
		Test test_record = null;
		if(iter.hasNext()){
			test_record = iter.next();
			test_record.setName(test.getName());
			test_record.setBrowserStatuses(test.getBrowserStatuses());
			
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
		TestDao test_dao = new TestDaoImpl();
		Test test = test_dao.find(key);
		test.setName(name);

		return test_dao.save(test);
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
														  @RequestParam(value="browser_type", required=true) String browser_type) 
																  throws MalformedURLException, UnknownAccountException, AuthenticationException, InvalidRequestException, APIConnectionException, CardException, APIException{
    	
    	String auth_access_token = request.getHeader("Authorization").replace("Bearer ", "");
    	Auth0Client auth = new Auth0Client();
    	String username = auth.getUsername(auth_access_token);

    	Account acct = AccountService.find(username);
    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}
    	
    	Subscription subscription = stripeClient.getSubscription(acct.getSubscriptionToken());
    	Plan plan = subscription.getPlan();
    	if(subscription.getTrialEnd() < (new Date()).getTime()/1000){
    		throw new FreeTrialExpiredException();
    	}
    	
    	String plan_name = plan.getId();
    	int test_index = plan_name.indexOf("-test");
    	int disc_index = plan_name.indexOf("-disc-");

    	int monthly_test_count = 0;
    	int allowed_test_cnt = Integer.parseInt(plan_name.substring(disc_index+6, test_index));

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
    	    	
    	if(monthly_test_count > allowed_test_cnt){
    		Analytics analytics = Analytics.builder("TjYM56IfjHFutM7cAdAEQGGekDPN45jI").build();
        	Map<String, String> traits = new HashMap<String, String>();
            traits.put("email", username);     
            traits.put("test_limit_reached", plan.getId());
        	analytics.enqueue(IdentifyMessage.builder()
        		    .userId(acct.getKey())
        		    .traits(traits)
        		);
        	
        	throw new TestLimitReachedException();
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
    	
    	TestDao test_dao = new TestDaoImpl();
    	for(String key : test_keys){
    		Test test = test_dao.find(key);
    		
    		TestRecord record = null;

			Map<String, Boolean> browser_running_status = test.getBrowserStatuses();
			browser_running_status.put(browser_type, null);

			test.setBrowserStatuses(browser_running_status);
			Message<Test> test_msg = new Message<Test>(acct.getKey(), test, new HashMap<String, Object>());
			
			//tell memory worker of test
			ActorSystem actor_system = ActorSystem.create("MinionActorSystem");
			final ActorRef memory_actor = actor_system.actorOf(Props.create(MemoryRegistryActor.class), "MemoryRegistration"+UUID.randomUUID());
			memory_actor.tell(test_msg, null);
			
			Browser browser = new Browser(browser_type.trim());
			record = TestingActor.runTest(test, browser);
			browser.close();

			acct.addTestRecord(record);
			AccountService.save(acct);
			
			test.addRecord(record);
			boolean is_passing = true;
			//update overall passing status based on all browser passing statuses
			for(Boolean status : test.getBrowserStatuses().values()){
				if(status != null && !status){
					is_passing = false;
					break;
				}
			}
			test.setCorrect(is_passing);
			test.setLastRunTimestamp(new Date());
			test_results.put(test.getKey(), record);
			test.setRunTime(record.getRunTime());
	
			//tell memory worker of test
			test_msg = new Message<Test>(acct.getKey(), test, new HashMap<String, Object>());
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
														@RequestParam(value="browser_type", required=true) String browser_type){		
		List<Test> test_list = new ArrayList<Test>();
		List<Test> group_list = new ArrayList<Test>();
		TestDao test_dao = new TestDaoImpl();
		test_list = test_dao.findByUrl(url);
		
		for(Test test : test_list){
			if(test.getGroups() != null && test.getGroups().contains(group)){
				group_list.add(test);
			}
		}
		
		List<TestRecord> group_records = new ArrayList<TestRecord>();

		for(Test group_test : group_list){
			Browser browser;
			try {
				browser = new Browser(browser_type);
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
		Group group = new GroupPOJO(name.toLowerCase(), description);

		TestDao test_dao = new TestDaoImpl();
		Test test = test_dao.find(key);
		
		GroupDao group_repo = new GroupDaoImpl();
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
		TestDao test_dao = new TestDaoImpl();
		Test test = test_dao.find(test_key);
		
		GroupDao group_dao = new GroupDaoImpl();
		Group group = group_dao.find(group_key);
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
		TestDao test_dao = new TestDaoImpl();

		List<Test> test_list = test_dao.findByUrl(url);
		
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