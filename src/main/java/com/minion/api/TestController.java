package com.minion.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.openqa.grid.common.exception.GridException;
import org.openqa.selenium.WebDriverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import com.qanairy.api.exceptions.DomainNotOwnedByAccountException;
import com.qanairy.api.exceptions.MissingSubscriptionException;
import com.qanairy.models.dto.exceptions.UnknownAccountException;
import com.qanairy.models.enums.TestStatus;
import com.qanairy.models.repository.AccountRepository;
import com.qanairy.models.repository.DomainRepository;
import com.qanairy.models.repository.GroupRepository;
import com.qanairy.models.repository.TestRepository;
import com.qanairy.services.SubscriptionService;
import com.qanairy.services.TestService;

import com.segment.analytics.Analytics;
import com.segment.analytics.messages.IdentifyMessage;
import com.segment.analytics.messages.TrackMessage;
import com.stripe.exception.StripeException;
import com.minion.api.exception.PaymentDueException;
import com.minion.browsing.Browser;
import com.qanairy.auth.Auth0Client;
import com.qanairy.models.Account;
import com.qanairy.models.Domain;
import com.qanairy.models.Group;
import com.qanairy.models.Test;
import com.qanairy.models.TestRecord;

/**
 * REST controller that defines endpoints to access tests
 */
@Controller
@RequestMapping("/tests")
public class TestController {
	private static Logger log = LoggerFactory.getLogger(TestController.class);

    @Autowired
    private DomainRepository domain_repo;
    
    @Autowired
    private AccountRepository account_repo;
    
    @Autowired
    private TestRepository test_repo;
    
    @Autowired
    private GroupRepository group_repo;
    
    @Autowired
    private TestService test_service;
    
    @Autowired
    private SubscriptionService subscription_service;
    
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
	public @ResponseBody Set<Test> getTestByDomain(HttpServletRequest request, 
													@RequestParam(value="url", required=true) String url) 
			throws UnknownAccountException, DomainNotOwnedByAccountException {    	
		return domain_repo.getVerifiedTests(url);
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
		Domain domain = domain_repo.findByHost(url);
		try{
			Iterator<Test> tests = domain.getTests().iterator();
			
			while(tests.hasNext()){
				Test test = tests.next();
				if(!test.getStatus().equals("UNVERIFIED") && !test.getStatus().equals("FAILING")){
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
	public @ResponseBody Set<Test> getUnverifiedTests(HttpServletRequest request, 
														@RequestParam(value="url", required=true) String url) 
																throws DomainNotOwnedByAccountException, UnknownAccountException {
    	return domain_repo.getUnverifiedTests(url);
    	
   		/*Domain domain = domain_repo.findByHost(url);
		
		Set<Test> tests = domain.getTests();
		Set<Test> unverified_tests = new HashSet<Test>();

		for(Test test : tests){
			if(test.getCorrect() == null){
				unverified_tests.add(test);
			}
		}
    	
    	Date end = new Date();
    	long diff = end.getTime() - start.getTime();
    	log.info("UNVERIFIED TESTS LOADED IN " + diff + " milliseconds");
    	*/
		//return unverified_tests;
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
													@RequestParam(value="status", required=true) TestStatus status)
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
    		    .userId(acct.getUsername())
    		    .traits(traits)
    		);
    	
		Test test = test_repo.findByKey(key);
		test.setStatus(status);
		test.getBrowserStatuses().put(browser_name, status.toString());
		//update last TestRecord passes value
		updateLastTestRecordPassingStatus(test);
		test_repo.save(test);
		
	   	//Fire discovery started event	
	   	Map<String, String> set_initial_correctness_props= new HashMap<String, String>();
	   	set_initial_correctness_props.put("test_key", test.getKey());
	   	analytics.enqueue(TrackMessage.builder("Set initial test status")
	   		    .userId(acct.getUsername())
	   		    .properties(set_initial_correctness_props)
	   		);
   	
		return test;
	}

    /**
     * 
     * @param itest
     */
	private void updateLastTestRecordPassingStatus(Test test) {
		Set<TestRecord> test_records = test.getRecords();
		Date last_ran_at = new Date(0L);
		TestRecord last_record = null;
		for(TestRecord test_record : test_records){
			Date time = test_record.getRanAt();
			if(time.after(last_ran_at)){
				last_ran_at = time;
				last_record = test_record;
			}
		}
		
		if(last_record != null){
			last_record.setPassing(test.getStatus());
		}
	}

	/**
	 * Updates a test
	 * 
	 * @param test
	 * @return
	 */
    @PreAuthorize("hasAuthority('update:tests')")
	@RequestMapping(path="/archive", method=RequestMethod.PUT)
	public @ResponseBody void archiveTest(HttpServletRequest request,
									@RequestParam(value="key", required=true) String key){
		Test test = test_repo.findByKey(key);

		test.setArchived(true);
		test_repo.save(test);
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
									@RequestParam(value="firefox", required=false) String firefox_status,
									@RequestParam(value="chrome", required=false) String chrome_status){
		Test test = test_repo.findByKey(key);
		
		Map<String, String> browser_statuses = new HashMap<String, String>();
		if(firefox_status!=null && !firefox_status.isEmpty()){
			browser_statuses.put("firefox", TestStatus.valueOf(firefox_status.toUpperCase()).toString());
		}
		if(chrome_status!=null && !chrome_status.isEmpty()){
			browser_statuses.put("chrome", TestStatus.valueOf(chrome_status.toUpperCase()).toString());
		}
		test.setName(name);
		test.setBrowserStatuses(browser_statuses);
		test_repo.save(test);
    }

    
	/**
	 * Updates the correctness of a test with the given test key
	 * 
	 * @param test
	 * @return
	 */
    @PreAuthorize("hasAuthority('update:tests')")
	@RequestMapping(path="/updateName", method=RequestMethod.PUT)
	public @ResponseBody Test updateName(HttpServletRequest request, 
										 @RequestParam(value="key", required=true) String key, 
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
     * @throws NoSuchAlgorithmException 
     * @throws WebDriverException 
     * @throws GridException 
     * @throws PaymentDueException 
     * @throws StripeException 
	 */
    @PreAuthorize("hasAuthority('run:tests')")
	@RequestMapping(path="/run", method = RequestMethod.POST)
	public @ResponseBody Map<String, TestRecord> runTests(HttpServletRequest request,
														  @RequestParam(value="test_keys", required=true) List<String> test_keys, 
														  @RequestParam(value="browser", required=true) String browser,
														  @RequestParam(value="host_url", required=true) String host) 
																  throws MalformedURLException, UnknownAccountException, GridException, WebDriverException, NoSuchAlgorithmException, PaymentDueException, StripeException{
    	
    	String auth_access_token = request.getHeader("Authorization").replace("Bearer ", "");
    	Auth0Client auth = new Auth0Client();
    	String username = auth.getUsername(auth_access_token);

    	Account acct = account_repo.findByUsername(username);
    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	
    	if(subscription_service.hasExceededSubscriptionTestRunsLimit(acct, subscription_service.getSubscriptionPlanName(acct))){
    		throw new PaymentDueException("Your plan has 0 test runs available. Upgrade now to run more tests");
        }
    	    	Analytics analytics = Analytics.builder("TjYM56IfjHFutM7cAdAEQGGekDPN45jI").build();
    	Map<String, String> traits = new HashMap<String, String>();
        traits.put("name", auth.getNickname(auth_access_token));
        traits.put("email", username);        
    	analytics.enqueue(IdentifyMessage.builder()
    		    .userId(acct.getUsername())
    		    .traits(traits)
    		);
    	
    	//Fire discovery started event	
	   	Map<String, String> run_test_batch_props= new HashMap<String, String>();
	   	run_test_batch_props.put("total tests", Integer.toString(test_keys.size()));
	   	analytics.enqueue(TrackMessage.builder("Running tests")
	   		    .userId(acct.getUsername())
	   		    .properties(run_test_batch_props)
	   		);
	   	
    	Map<String, TestRecord> test_results = new HashMap<String, TestRecord>();
    	
    	for(String key : test_keys){
    		Test test = test_repo.findByKey(key);
    		TestRecord record = null;
    		
    		/*
    		Date date = new Date();
			long date_millis = date.getTime();
			Map<String, Object> usageRecordParams = new HashMap<String, Object>();
	    	usageRecordParams.put("quantity", 1);
	    	usageRecordParams.put("timestamp", date_millis/1000);
	    	usageRecordParams.put("subscription_item", subscription_item);
	    	usageRecordParams.put("action", "increment");

	    	UsageRecord.create(usageRecordParams, null);
*/
			Browser browser_dto = new Browser(browser.trim());
			record = test_service.runTest(test, browser_dto);
			browser_dto.close();
			
			test.addRecord(record);
	    	test.getBrowserStatuses().put(record.getBrowser(), record.getPassing().toString());			
			    		
			test_results.put(test.getKey(), record);
			TestStatus is_passing = TestStatus.PASSING;
			//update overall passing status based on all browser passing statuses
			for(String status : test.getBrowserStatuses().values()){
				if(status.equals(TestStatus.UNVERIFIED) || status.equals(TestStatus.FAILING)){
					is_passing = TestStatus.FAILING;
					break;
				}
			}
			Map<String, String> browser_statuses = test.getBrowserStatuses();
			browser_statuses.put(browser, is_passing.toString());
			
			test.addRecord(record);
			test.setStatus(is_passing);
			test.setLastRunTimestamp(new Date());
			test.setRunTime(record.getRunTime());
			test.setBrowserStatuses(browser_statuses);
			test_repo.save(test);

			acct.addTestRecord(record);
			account_repo.save(acct);
			acct.addTestRecord(record);
			account_repo.save(acct);
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
    /*@PreAuthorize("hasAuthority('run:tests')")
	@RequestMapping(path="/runTestGroup/{group}", method = RequestMethod.POST)
	public @ResponseBody List<TestRecord> runTestByGroup(@PathVariable("group") String group,
														@RequestParam(value="url", required=true) String url,
														@RequestParam(value="browser_name", required=true) String browser_name){		
		Set<Test> test_list = new HashSet<Test>();
		Set<Test> group_list = new HashSet<Test>();
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
	*/
    
    
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
		
		Group group_record = group_repo.findByKey(group.getKey());
		if(group_record == null){
			group = group_repo.save(group);
		}
		else{
			group = group_record;
		}
		Test test = test_repo.findByKey(key);
		test.getGroups().add(group);
		
		test = test_service.save(test, test.firstPage().getUrl());
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
		test_repo.save(test);
	}

	/**
	 * Retrieves list of all tests from the database
	 * 
	 * @param url
	 * 
	 * @return
	 */
    /*@PreAuthorize("hasAuthority('read:groups')")
	@RequestMapping(path="/groups", method = RequestMethod.GET)
	public @ResponseBody List<Group> getGroups(HttpServletRequest request, 
			   								   @RequestParam(value="url", required=true) String url) {
		List<Group> groups = new ArrayList<Group>();
		Set<Test> test_list = test_repo.findByUrl(url);
		
		for(Test test : test_list){
			if(test.getGroups() != null){
				groups.addAll(test.getGroups());
			}
		}

		return groups;
	}
	*/
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