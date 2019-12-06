package com.minion.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.ArrayList;
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
import com.qanairy.analytics.SegmentAnalyticsHelper;
import com.qanairy.api.exceptions.DomainNotOwnedByAccountException;
import com.qanairy.api.exceptions.MissingSubscriptionException;
import com.qanairy.dto.TestDto;
import com.qanairy.models.dto.exceptions.UnknownAccountException;
import com.qanairy.models.enums.TestStatus;
import com.qanairy.models.repository.GroupRepository;
import com.qanairy.models.repository.TestRecordRepository;
import com.qanairy.models.repository.TestRepository;
import com.qanairy.services.AccountService;
import com.qanairy.services.DomainService;
import com.qanairy.services.SubscriptionService;
import com.qanairy.services.TestService;
import com.stripe.exception.StripeException;
import io.swagger.annotations.ApiOperation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.minion.api.exception.PaymentDueException;
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
    private DomainService domain_service;

    @Autowired
    private AccountService account_service;

    @Autowired
    private TestRepository test_repo;

    @Autowired
    private TestRecordRepository test_record_repo;

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
    	//make sure domain belongs to user account first
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account acct = account_service.findByUserId(id);

    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}
    	
		Set<Test> tests = domain_service.getVerifiedTests(url, acct.getUserId());
		for(Test test : tests){
			test.setGroups(test_service.getGroups(test.getKey()));
		}
		return tests;
    }

    /**
	 * Updates a test
	 *
	 * @param test
	 * @return
     * @throws JsonProcessingException
     * @throws MalformedURLException 
     * @throws UnknownAccountException 
	 */
    @PreAuthorize("hasAuthority('update:tests')")
	@RequestMapping(method=RequestMethod.PUT)
	public @ResponseBody Test update(HttpServletRequest request,
									@RequestParam(value="key", required=true) String key,
									@RequestParam(value="name", required=true) String name,
									@RequestParam(value="firefox", required=false) String firefox_status,
									@RequestParam(value="chrome", required=false) String chrome_status,
									@RequestParam(value="url", required=true) String url
	) throws JsonProcessingException, MalformedURLException, UnknownAccountException{
    	//make sure domain belongs to user account first
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account acct = account_service.findByUserId(id);

    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}
    	
    	Test test = test_service.findByKey(key, url, acct.getUserId());
		Map<String, String> browser_statuses = test.getBrowserStatuses();
		TestStatus status = TestStatus.FAILING;

		if(firefox_status!=null && !firefox_status.isEmpty()){
			browser_statuses.put("firefox", TestStatus.valueOf(firefox_status.toUpperCase()).toString());

			if(firefox_status.equalsIgnoreCase("failing")){
				status = TestStatus.FAILING;
			}
			else{
				status = TestStatus.PASSING;
			}
		}
		if(chrome_status != null && !chrome_status.isEmpty()){
			log.warn("chrome status :: "+chrome_status);
			browser_statuses.put("chrome", TestStatus.valueOf(chrome_status.toUpperCase()).toString());
			if(chrome_status.equalsIgnoreCase("failing")){
				status = TestStatus.FAILING;
			}
			else{
				status = TestStatus.PASSING;
			}
		}
		
		test.setName(name);
		test.setBrowserStatuses(browser_statuses);
		test.setStatus(status);
		//test.setRecords(records);
		//update status of last test record
		test = test_repo.save(test);

		//get last test record
		TestRecord record = test_repo.getMostRecentRecord(test.getKey(), url, acct.getUserId());
		record.setStatus(status);
		test_record_repo.save(record);

		record = test_record_repo.updateStatus(record.getKey(), status.toString());
		return test;
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
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account acct = account_service.findByUserId(id);

    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}
    	
    	int failed_tests = 0;
		Domain domain = domain_service.findByUrl(url, acct.getUserId());
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
    	//make sure domain belongs to user account first
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account acct = account_service.findByUserId(id);

    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}
    	
    	Set<Test> tests = domain_service.getUnverifiedTests(url, acct.getUserId());

    	for(Test test : tests){
    		List<TestRecord> records = test_repo.findAllTestRecords(test.getKey(), url, acct.getUserId());
    		test.setRecords(records);
    	}
    	return tests;
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
													@RequestParam(value="status", required=true) TestStatus status,
													@RequestParam(value="url", required=true) String url)
															throws UnknownAccountException{

    	//make sure domain belongs to user account first
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account acct = account_service.findByUserId(id);

    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}

		Test test = test_repo.findByKey(key, url, acct.getUserId());
		test.setStatus(status);
		test.getBrowserStatuses().put(browser_name, status.toString());
		//update last TestRecord passes value
		updateLastTestRecordPassingStatus(test);
		test = test_repo.save(test);

	   	//Fire discovery started event
	   	Map<String, String> set_initial_correctness_props= new HashMap<String, String>();
	   	set_initial_correctness_props.put("test_key", test.getKey());
		return test;
	}

    /**
     *
     * @param itest
     */
	private void updateLastTestRecordPassingStatus(Test test) {
		List<TestRecord> test_records = test.getRecords();
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
			last_record.setStatus(test.getStatus());
		}
	}

	/**
	 * Updates a test
	 *
	 * @param test
	 * @return
	 * @throws UnknownAccountException 
	 */
    @PreAuthorize("hasAuthority('update:tests')")
	@RequestMapping(path="/archive", method=RequestMethod.PUT)
	public @ResponseBody void archiveTest(HttpServletRequest request,
									@RequestParam(value="key", required=true) String key,
									@RequestParam(value="url", required=true) String url
	) throws UnknownAccountException{
    	//make sure domain belongs to user account first
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account acct = account_service.findByUserId(id);

    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}
    	
		Test test = test_repo.findByKey(key, url, acct.getUserId());

		test.setArchived(true);
		test_repo.save(test);
    }




	/**
	 * Updates the correctness of a test with the given test key
	 *
	 * @param test
	 * @return
	 * @throws UnknownAccountException 
	 */
    @PreAuthorize("hasAuthority('update:tests')")
	@RequestMapping(path="/updateName", method=RequestMethod.PUT)
	public @ResponseBody Test updateName(HttpServletRequest request,
										 @RequestParam(value="key", required=true) String key,
										 @RequestParam(value="name", required=true) String name,
										 @RequestParam(value="url", required=true) String url
	 ) throws UnknownAccountException{
    	
    	//make sure domain belongs to user account first
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account acct = account_service.findByUserId(id);

    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}
		Test test = test_repo.findByKey(key, url, acct.getUserId() );
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
     * @throws JsonProcessingException
	 */
    @PreAuthorize("hasAuthority('run:tests')")
	@RequestMapping(path="/run", method = RequestMethod.POST)
	public @ResponseBody Map<String, TestRecord> runTests(HttpServletRequest request,
														  @RequestParam(value="test_keys", required=true) List<String> test_keys,
														  @RequestParam(value="browser", required=true) String browser,
														  @RequestParam(value="host_url", required=true) String host)
																  throws UnknownAccountException, PaymentDueException, StripeException, JsonProcessingException{

    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account acct = account_service.findByUserId(id);

    	if(acct == null){
    		throw new UnknownAccountException();
    	}

    	/*
    	if(subscription_service.hasExceededSubscriptionTestRunsLimit(acct, subscription_service.getSubscriptionPlanName(acct))){
    		throw new PaymentDueException("Your plan has 0 test runs available. Upgrade now to run more tests");
        }
    	 */
    	
    	SegmentAnalyticsHelper.testRunStarted(acct.getUserId(), test_keys.size());

    	Map<String, TestRecord> test_results = new HashMap<String, TestRecord>();

    	for(String key : test_keys){
    		Test test = test_repo.findByKey(key, host, acct.getUserId());
    		TestStatus last_test_status = test.getStatus();

			test.setStatus(TestStatus.RUNNING);
			test.setBrowserStatus(browser.trim(), TestStatus.RUNNING.toString());
			test = test_repo.save(test);
			
    		TestRecord record = test_service.runTest(test, browser, last_test_status, host, acct.getUserId());
			test_results.put(test.getKey(), record);

			//set browser status first since we use browser statuses to determine overall test status
			test.setBrowserStatus(browser.trim(), record.getStatus().toString());

			TestStatus is_passing = TestStatus.PASSING;
			//update overall passing status based on all browser passing statuses
			for(String status : test.getBrowserStatuses().values()){
				if(status.equals(TestStatus.FAILING.toString())){
					is_passing = TestStatus.FAILING;
					break;
				}
				else if(status.equals(TestStatus.UNVERIFIED.toString())){
					is_passing = TestStatus.UNVERIFIED;
					break;
				}
			}
			
			record = test_record_repo.save(record);

			SegmentAnalyticsHelper.sendTestFinishedRunningEvent(acct.getUserId(), test);
			test = test_service.findByKey(test.getKey(), host, acct.getUserId());

			test.addRecord(record);
			test.setStatus(is_passing);
			test.setBrowserStatus(browser, is_passing.toString());
			test_repo.save(test);

			acct.addTestRecord(record);
			account_service.save(acct);
			MessageBroadcaster.broadcastTestStatus(host, record, test, acct.getUserId());
    	}

    	log.warn("returning test results");
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
				browser = BrowserFactory.buildBrowser(browser_name, BrowserEnvironment.TEST);
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
	 * @throws MalformedURLException
	 * @throws UnknownAccountException 
	 */
    @PreAuthorize("hasAuthority('create:groups')")
	@RequestMapping(path="/addGroup", method = RequestMethod.POST)
	public @ResponseBody Group addGroup(HttpServletRequest request,
										@RequestParam(value="name", required=true) String name,
										@RequestParam(value="description", required=false) String description,
										@RequestParam(value="key", required=true) String test_key,
										@RequestParam(value="url", required=true) String url
	) throws MalformedURLException, UnknownAccountException {
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account acct = account_service.findByUserId(id);

    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	
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

		test_service.addGroup(test_key, group, url, acct.getUserId());
		return group;
	}

    /**
	 * Adds given group to test with given key
	 *
	 * @param group_key String key representing group to add to test
	 * @param test_key key for test that will have group added to it
	 * @param url {@link Domain} url
	 * 
	 * @return the updated test
	 *      
     * @throws UnknownAccountException 
	 */
    @PreAuthorize("hasAuthority('delete:groups')")
	@RequestMapping(path="/remove/group", method = RequestMethod.POST)
	public @ResponseBody void removeGroup(HttpServletRequest request,
										  @RequestParam(value="group_key", required=true) String group_key,
										  @RequestParam(value="test_key", required=true) String test_key,
										  @RequestParam(value="url", required=true) String url) throws UnknownAccountException{
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account acct = account_service.findByUserId(id);

    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	
		Test test = test_service.findByKey(test_key, url, acct.getUserId());
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
    @PreAuthorize("hasAuthority('read:groups')")
	@RequestMapping(path="groups", method = RequestMethod.GET)
	public @ResponseBody List<Group> getGroups(HttpServletRequest request,
			   								   @RequestParam(value="url", required=true) String url) {
		List<Group> groups = new ArrayList<Group>();
		Set<Test> test_list = domain_service.getTests(url);

		for(Test test : test_list){
			if(test.getGroups() != null){
				groups.addAll(test.getGroups());
			}
		}

		return groups;
	}

	/**
	 * Handles pushing a {@link Test} to the current user's Pusher channel in a format compliant with
	 *   the browser extension spec
	 *
	 * @param url
	 *
	 * @return
	 * @throws UnknownAccountException
	 * @throws JsonProcessingException
	 */
    @ApiOperation(value = "Send test to browser extension by publishing test to users real time message channel", response = Iterable.class)
    @PreAuthorize("hasAuthority('read:groups')")
	@RequestMapping(path="{test_key}/edit", method = RequestMethod.POST)
	public @ResponseBody TestDto editTest(HttpServletRequest request,
			   								@PathVariable(value="test_key") String test_key,
			   								@RequestParam(value="url", required=true) String url
    ) throws UnknownAccountException, JsonProcessingException {
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account acct = account_service.findByUserId(id);

    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	Test test = test_service.findByKey(test_key, url, acct.getUserId());
    	test.setPathObjects(test_service.getPathObjects(test.getKey(), url, acct.getUserId()));
		//convert test to ide test
		/*
		 * {
		 *   key: {test_key}
		 * 	 [
		 *     { key: {page_key}, url: {page_url}},
		 *     { element:
		 *     	  {
		 *     		key: {element_key},
		 *     		xpath: {element_xpath}
		 *     	  },
		 *        {
		 *        	key: {action_key},
		 *        	type: {action_type},
		 *          value: {action_value
		 *        }
	     *     }
		 * 	 ]
		 * }
		 */
    	TestDto test_dto = new TestDto(test);

		//send test to ide
		MessageBroadcaster.broadcastIdeTest(test_dto, acct.getUsername());
		return test_dto;
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
