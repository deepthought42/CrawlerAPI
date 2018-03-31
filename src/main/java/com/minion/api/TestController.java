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
import com.qanairy.models.Test;
import com.qanairy.models.TestRecord;
import com.qanairy.models.dto.DomainRepository;
import com.qanairy.models.dto.GroupRepository;
import com.qanairy.models.dto.PathRepository;
import com.qanairy.models.dto.TestRecordRepository;
import com.qanairy.models.dto.TestRepository;
import com.qanairy.models.dto.exceptions.UnknownAccountException;
import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.IDomain;
import com.qanairy.persistence.IGroup;
import com.qanairy.persistence.IPath;
import com.qanairy.persistence.ITest;
import com.qanairy.persistence.ITestRecord;
import com.qanairy.persistence.OrientConnectionFactory;
import com.qanairy.services.AccountService;
import com.qanairy.services.DomainService;
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
import com.qanairy.models.Account;
import com.qanairy.models.Domain;
import com.qanairy.models.Group;
import com.qanairy.models.Page;
import com.qanairy.models.Path;

/**
 * REST controller that defines endpoints to access tests
 */
@Controller
@RequestMapping("/tests")
public class TestController {
	private static Logger log = LoggerFactory.getLogger(TestController.class);

    @Autowired
    protected AccountService accountService;
    
    @Autowired
    protected DomainService domainService;

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

    	String auth_access_token = request.getHeader("Authorization").replace("Bearer ", "");
    	Auth0Client auth = new Auth0Client();
    	String username = auth.getUsername(auth_access_token);

    	Account acct = accountService.find(username);
    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}
    	
       	boolean owned_by_acct = false;

    	for(Domain domain_rec : acct.getDomains()){
    		if(domain_rec.getUrl().equals(url)){
    			owned_by_acct = true;
    			break;
    		}
    	}
    	
    	if(!owned_by_acct){
    		throw new DomainNotOwnedByAccountException();
    	}
		DomainRepository domain_repo = new DomainRepository();
    	
		IDomain idomain = domain_repo.find(url);
		Iterator<ITest> tests = idomain.getTests().iterator();
		TestRepository test_repo = new TestRepository();
		List<Test> verified_tests = new ArrayList<Test>();
		
		while(tests.hasNext()){
			ITest itest = tests.next();
			if(itest.getCorrect() != null){
				verified_tests.add(test_repo.load(itest));
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

    	String auth_access_token = request.getHeader("Authorization").replace("Bearer ", "");
    	Auth0Client auth = new Auth0Client();
    	String username = auth.getUsername(auth_access_token);

    	Account acct = accountService.find(username);
    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}
    	
		DomainRepository domain_repo = new DomainRepository();
    	
		int failed_tests = 0;
		IDomain idomain = domain_repo.find(url);
		try{
			Iterator<ITest> tests = idomain.getTests().iterator();
			
			while(tests.hasNext()){
				ITest itest = tests.next();
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
	public @ResponseBody List<Test> getTestsByName(HttpSession session, HttpServletRequest request, 
			   								 		@RequestParam(value="name", required=true) String name) {
		List<Test> test_list = new ArrayList<Test>();
		test_list = Test.findByName(name);
		
		return test_list;
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
    	//make sure domain belongs to user account first

    	String auth_access_token = request.getHeader("Authorization").replace("Bearer ", "");
    	Auth0Client auth = new Auth0Client();
    	String username = auth.getUsername(auth_access_token);
    	
    	Account acct = accountService.find(username);
    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	
		DomainRepository domain_repo = new DomainRepository();
		IDomain idomain = domain_repo.find(url);

		Iterator<ITest> tests = idomain.getTests().iterator();
		TestRepository test_repo = new TestRepository();
		List<Test> unverified_tests = new ArrayList<Test>();
		while(tests.hasNext()){
			ITest itest = tests.next();
			if(itest.getCorrect() == null){
				unverified_tests.add(test_repo.load(itest));
			}
		}
    	Date end = new Date();
    	long diff = end.getTime() - start.getTime();
    	System.err.println("UNVERIFIED TESTS LOADED IN " + diff + " milliseconds");
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
													@RequestParam(value="correct", required=true) boolean correct)
															throws UnknownAccountException{
    	
    	//make sure domain belongs to user account first
    	String auth_access_token = request.getHeader("Authorization").replace("Bearer ", "");
    	Auth0Client auth = new Auth0Client();
    	String username = auth.getUsername(auth_access_token);

    	Account acct = accountService.find(username);
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
    	
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		Iterator<ITest> itest_iter = Test.findByKey(key, orient_connection).iterator();
		ITest itest = itest_iter.next();
		itest.setCorrect(correct);
		
		IDomain idomain = itest.getDomain();
		
 		String browser_name = idomain.getDiscoveryBrowserName();
		Map<String, Boolean> browser_statuses = itest.getBrowserStatuses();
		System.err.println("browser :::     " +browser_name+" : ##########  : "+correct);
		browser_statuses.put(browser_name, correct);
		itest.setBrowserStatuses(browser_statuses);
		
		//update last TestRecord passes value
		updateLastTestRecordPassingStatus(itest);
		
	   	//Fire discovery started event	
	   	Map<String, String> set_initial_correctness_props= new HashMap<String, String>();
	   	set_initial_correctness_props.put("test_key", itest.getKey());
	   	analytics.enqueue(TrackMessage.builder("Set initial test status")
	   		    .userId(acct.getKey())
	   		    .properties(set_initial_correctness_props)
	   		);
   	
		TestRepository test_record = new TestRepository();
		return test_record.load(itest);
	}

    /**
     * 
     * @param itest
     */
	private void updateLastTestRecordPassingStatus(ITest itest) {
		Iterator<ITestRecord> itest_records = itest.getRecords().iterator();
		ITestRecord record = null;
		while(itest_records.hasNext()){
			record = itest_records.next();
		}
		if(record != null){
			record.setPassing(itest.getCorrect());
		}
	}

	/**
	 * gets {@link Path} for a given test key
	 * 
	 * @param key key for test that path is to be found for
	 * @return path {@link Path} for given test
	 */
    @PreAuthorize("hasAuthority('read:tests')")
	@RequestMapping(path="/tests/paths", method=RequestMethod.GET)
	public @ResponseBody Path getTestPath(@RequestParam(value="key", required=true) String key){
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		Iterator<ITest> itest_iter = Test.findByKey(key, orient_connection).iterator();
		ITest itest = itest_iter.next();
		IPath path_record = itest.getPath();
		
		PathRepository path_repo = new PathRepository();
		Path path = path_repo.load(path_record);

		return path;
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
		Iterable<ITest> tests = (Iterable<ITest>) DataAccessObject.findByKey(test.getKey(), orient_connection, ITest.class);
		Iterator<ITest> iter = tests.iterator();
		ITest test_record = null;
		if(iter.hasNext()){
			test_record = iter.next();
			test_record.setName(test.getName());
			test_record.setBrowserStatuses(test.getBrowserPassingStatuses());
			
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
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		Iterator<ITest> itest_iter = Test.findByKey(key, orient_connection).iterator();
		ITest itest = itest_iter.next();
		itest.setName(name);
		orient_connection.save();

		TestRepository test_record = new TestRepository();
		return test_record.load(itest);
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

    	Account acct = accountService.find(username);
    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}
    	
    	Subscription subscription = stripeClient.getSubscription(acct.getSubscriptionToken());
    	Plan plan = subscription.getPlan();
    	if(subscription.getTrialEnd() < (new Date()).getTime()){
    		throw new FreeTrialExpiredException();
    	}
    	
    	String plan_name = plan.getId();
    	int test_index = plan_name.indexOf("-test");
    	int disc_index = plan_name.indexOf("-disc-");

    	int monthly_test_count = 0;
    	int allowed_test_cnt = Integer.parseInt(plan_name.substring(disc_index+7, test_index));

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
        	
        	throw new DiscoveryLimitReachedException();
    	}
    	
    	if(monthly_test_count > 10000){
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
	   	
    	OrientConnectionFactory connection = new OrientConnectionFactory();
    	Map<String, TestRecord> test_results = new HashMap<String, TestRecord>();
    	
    	for(String key : test_keys){
    		Iterator<ITest> itest_iter = Test.findByKey(key, connection).iterator();
    		
    		while(itest_iter.hasNext()){
    			ITest itest = itest_iter.next();
        		TestRecord record = null;
        		
	    		if(itest.getKey().equals(key)){
	    			TestRepository test_repo = new TestRepository();
	    	
	    			Test test = test_repo.load(itest);

	    			Map<String, Boolean> browser_running_status = itest.getBrowserStatuses();
	    			browser_running_status.put(browser_type, null);
	    			for(String browser : browser_running_status.keySet()){
	    				System.err.println("Browser ::::  "+browser+"  ************   "+test.getBrowserPassingStatuses().get(browser));
	    			}
	    			test.setBrowserPassingStatuses(browser_running_status);
	    			Message<Test> test_msg = new Message<Test>(acct.getKey(), test, new HashMap<String, Object>());
	    			
	    			//tell memory worker of test
	    			ActorSystem actor_system = ActorSystem.create("MinionActorSystem");
	    			final ActorRef memory_actor = actor_system.actorOf(Props.create(MemoryRegistryActor.class), "MemoryRegistration"+UUID.randomUUID());
	    			memory_actor.tell(test_msg, null);
	    			
	    			Browser browser = new Browser(test.getPath().firstPage().getUrl().toString().trim(), browser_type.trim());
	    			record = TestingActor.runTest(test, browser);
	    			acct.addTestRecord(record);
	    			accountService.save(acct);
	    			
	    			System.err.println("Record is passing :::: "+record.getPassing());
	    			browser_running_status.put(browser_type, record.getPassing());
	    			test.setBrowserPassingStatuses(browser_running_status);
	    			for(String browser_1 : browser_running_status.keySet()){
	    				System.err.println("Browser 1  ::::  "+browser_1+"  ************   "+browser_running_status.get(browser_1));
	    			}
	    			test.addRecord(record);
	    			boolean is_passing = true;
					//update overall passing status based on all browser passing statuses
					for(Boolean status : test.getBrowserPassingStatuses().values()){
						if(status != null && !status){
							is_passing = false;
						}
					}
					test.setCorrect(is_passing);
					test.setLastRunTimestamp(new Date());
	    			test_results.put(test.getKey(), record);
	    			test.setRunTime(record.getRunTime());
	    	
	    			TestRecordRepository test_record_record = new TestRecordRepository();
	    			itest.addRecord(test_record_record.save(connection, record));
	    			itest.setCorrect(record.getPassing());

	    			//tell memory worker of test
	    			test_msg = new Message<Test>(acct.getKey(), test, new HashMap<String, Object>());
	    			memory_actor.tell(test_msg, null);
	    			
	    			browser.close();
	    		}
	    		else{
	    			log.warn("test found does not match key :: " + key);
	    		}
    		}
    	}
		connection.close();
		
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
		
		try {
			test_list = Test.findByUrl(url);
			
			for(Test test : test_list){
				if(test.getGroups() != null && test.getGroups().contains(group)){
					group_list.add(test);
				}
			}
		} catch (MalformedURLException e) {
			log.warn("Malformed URL received", e.getMessage());
		}
		
		List<TestRecord> group_records = new ArrayList<TestRecord>();

		for(Test group_test : group_list){
			Browser browser;
			try {
				browser = new Browser(((Page)group_test.getPath().getPath().get(0)).getUrl().toString(), browser_type);
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
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();

		Iterator<ITest> itest_iter = Test.findByKey(key, orient_connection).iterator();
		ITest itest = itest_iter.next();
		Iterator<IGroup> group_iter = itest.getGroups().iterator();
		IGroup igroup = null;

		while(group_iter.hasNext()){
			igroup = group_iter.next();
			if(igroup.getName().equals(name)){
				return null;
				//would be better to return an already exists status/error
			}
		}
		
		GroupRepository group_repo = new GroupRepository();
		igroup = group_repo.save(orient_connection, group);
		itest.addGroup(igroup);
		group.setKey(igroup.getKey());
		
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
	public @ResponseBody Boolean removeGroup(@RequestParam(value="group_key", required=true) String group_key,
										  	 @RequestParam(value="test_key", required=true) String test_key){
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();

		Iterator<ITest> itest_iter = Test.findByKey(test_key, orient_connection).iterator();
		ITest itest = itest_iter.next();
		Iterator<IGroup> group_iter = itest.getGroups().iterator();
		boolean was_removed = false;
		IGroup igroup = null;
		while(group_iter.hasNext()){
			igroup = group_iter.next();
			if(igroup.getKey().equals(group_key)){
				itest.removeGroup(igroup);
				was_removed=true;
			}
		}

		orient_connection.close();
		return was_removed;
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
		List<Test> test_list = new ArrayList<Test>();
		List<Group> groups = new ArrayList<Group>();
		try {
			test_list = Test.findByUrl(url);
			
			for(Test test : test_list){
				if(test.getGroups() != null){
					groups.addAll(test.getGroups());
				}
			}
		} catch (MalformedURLException e) {
			log.warn("Malformed url exception thrown", e.getMessage());
		}
		
		return groups;
	}
}

@ResponseStatus(HttpStatus.SEE_OTHER)
class TestAlreadyRunningException extends RuntimeException {
	private static final long serialVersionUID = 7200878662560716215L;

	public TestAlreadyRunningException() {
		super("Test is already running");
	}
}

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
class EmptyGroupNameException extends RuntimeException {
	private static final long serialVersionUID = 7200878662560716215L;

	public EmptyGroupNameException() {
		super("Groups must have a name");
	}
}

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
class TestLimitReachedException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7200878662560716216L;

	public TestLimitReachedException() {
		super("Test run limit reached. Upgrade your account now!");
	}
}