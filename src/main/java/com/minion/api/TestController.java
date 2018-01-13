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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.auth0.spring.security.api.Auth0UserDetails;
import com.minion.actors.TestingActor;
import com.qanairy.models.Test;
import com.qanairy.models.TestRecord;
import com.qanairy.models.dto.DomainRepository;
import com.qanairy.models.dto.GroupRepository;
import com.qanairy.models.dto.PathRepository;
import com.qanairy.models.dto.TestRecordRepository;
import com.qanairy.models.dto.TestRepository;
import com.qanairy.models.dto.exceptions.UnknownAccountException;
import com.qanairy.persistence.IDomain;
import com.qanairy.persistence.IGroup;
import com.qanairy.persistence.IPath;
import com.qanairy.persistence.ITest;
import com.qanairy.persistence.ITestRecord;
import com.qanairy.persistence.OrientConnectionFactory;
import com.qanairy.services.AccountService;
import com.qanairy.services.DomainService;
import com.minion.browsing.Browser;
import com.qanairy.api.exception.DomainNotOwnedByAccountException;
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

	/**
	 * Retrieves list of all tests from the database 
	 * @param url
	 * 
	 * @return list of tests previously discovered for given url
	 * @throws UnknownAccountException 
	 * @throws DomainNotOwnedByAccountException 
	 */
    @PreAuthorize("hasAuthority('user') or hasAuthority('qanairy')")
	@RequestMapping(method = RequestMethod.GET)
	public @ResponseBody List<Test> getTestByDomain(HttpServletRequest request, 
													@RequestParam(value="url", required=true) String url) 
															throws UnknownAccountException, DomainNotOwnedByAccountException {
    	//make sure domain belongs to user account first
    	final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        final Auth0UserDetails currentUser = (Auth0UserDetails) authentication.getPrincipal();

    	Account acct = accountService.find(currentUser.getUsername());
    	if(acct == null){
    		throw new UnknownAccountException();
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
				verified_tests.add(test_repo.convertFromRecord(itest));
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
    @PreAuthorize("hasAuthority('user') or hasAuthority('qanairy')")
	@RequestMapping(path="/failing", method = RequestMethod.GET)
	public @ResponseBody Map<String, Integer> getFailingTestByDomain(HttpServletRequest request, 
			   								 	 	@RequestParam(value="url", required=true) String url) 
			   										 throws UnknownAccountException, DomainNotOwnedByAccountException {
    	//make sure domain belongs to user account first
    	final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        final Auth0UserDetails currentUser = (Auth0UserDetails) authentication.getPrincipal();

    	Account acct = accountService.find(currentUser.getUsername());
    	if(acct == null){
    		throw new UnknownAccountException();
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
    @PreAuthorize("hasAuthority('user') or hasAuthority('qanairy')")
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
    //@PreAuthorize("hasAuthority('user') or hasAuthority('qanairy')")
	@RequestMapping(path="/unverified", method = RequestMethod.GET)
	public @ResponseBody List<Test> getUnverifiedTests(HttpServletRequest request, 
														@RequestParam(value="url", required=true) String url) 
																throws DomainNotOwnedByAccountException, UnknownAccountException {
    	Date start = new Date();
    	//make sure domain belongs to user account first
    	final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        final Auth0UserDetails currentUser = (Auth0UserDetails) authentication.getPrincipal();
    	Account acct = accountService.find(currentUser.getUsername());
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
				unverified_tests.add(test_repo.convertFromRecord(itest));
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
	 */
    @PreAuthorize("hasAuthority('user') or hasAuthority('qanairy')")
	@RequestMapping(path="/updateCorrectness", method=RequestMethod.PUT)
	public @ResponseBody Test updateBrowserCorrectness(@RequestParam(value="key", required=true) String key, 
														@RequestParam(value="browser_name", required=true) String browser_name, 
														@RequestParam(value="correct", required=true) boolean correct){
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		Iterator<ITest> itest_iter = Test.findByKey(key, orient_connection).iterator();
		ITest itest = itest_iter.next();
		itest.setCorrect(correct);
		Map<String, Boolean> browser_statuses = itest.getBrowserStatuses();
		browser_statuses.put(browser_name, correct);
		itest.setBrowserStatuses(browser_statuses);
		
		//update last TestRecord passes value
		updateLastTestRecordPassingStatus(itest);
		
		TestRepository test_record = new TestRepository();
		return test_record.convertFromRecord(itest);
	}

	private void updateLastTestRecordPassingStatus(ITest itest) {
		Iterator<ITestRecord> itest_records = itest.getRecords().iterator();
		ITestRecord record = null;
		while(itest_records.hasNext()){
			record = itest_records.next();
		}
		if(record != null){
			record.setPasses(itest.getCorrect());
		}
	}

	/**
	 * gets {@link Path} for a given test key
	 * 
	 * @param key key for test that path is to be found for
	 * @return path {@link Path} for given test
	 */
    @PreAuthorize("hasAuthority('user') or hasAuthority('qanairy')")
	@RequestMapping(path="/tests/paths", method=RequestMethod.GET)
	public @ResponseBody Path getTestPath(@RequestParam(value="key", required=true) String key){
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		Iterator<ITest> itest_iter = Test.findByKey(key, orient_connection).iterator();
		ITest itest = itest_iter.next();
		IPath path_record = itest.getPath();
		
		PathRepository path_repo = new PathRepository();
		Path path = path_repo.convertFromRecord(path_record);

		return path;
	}
    
	/**
	 * Updates the correctness of a test with the given test key
	 * 
	 * @param test
	 * @return
	 */
    @PreAuthorize("hasAuthority('user') or hasAuthority('qanairy')")
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
		return test_record.convertFromRecord(itest);
	}
    
	/**
	 * Runs test with a given key
	 * 
	 * @param key
	 * @return
	 * @throws MalformedURLException 
	 */
    @PreAuthorize("hasAuthority('user') or hasAuthority('qanairy')")
	@RequestMapping(path="/runTest/{key}", method = RequestMethod.POST)
	public @ResponseBody Test runTest(@PathVariable(value="key", required=true) String key, 
									  @RequestParam(value="browser_type", required=true) String browser_type) throws MalformedURLException{
    	OrientConnectionFactory connection = new OrientConnectionFactory();
		Iterator<ITest> itest_iter = Test.findByKey(key, connection).iterator();
		ITest itest = itest_iter.next();
		if(!itest.getRunStatus()){
			itest.setRunStatus(true);
			TestRecord record = null;
			TestRepository test_record = new TestRepository();
			
			if(itest.getKey().equals(key)){
				Test test = test_record.convertFromRecord(itest);
				Browser browser = new Browser(((Page)test.getPath().getPath().get(0)).getUrl().toString(), browser_type);
				record = TestingActor.runTest(test, browser);
				
				TestRecordRepository test_record_record = new TestRecordRepository();
				itest.addRecord(test_record_record.convertToRecord(connection, record));
				itest.setCorrect(record.getPasses());
				Map<String, Boolean> browser_statuses = itest.getBrowserStatuses();
				browser_statuses.put(record.getBrowser(), record.getPasses());
				itest.setBrowserStatuses(browser_statuses);
				itest.setRunStatus(false);
				browser.close();
			}
			else{
				log.warn("test found does not match key :: " + key);
			}
			connection.close();
			return test_record.convertFromRecord(itest);
		}
		
		throw new TestAlreadyRunningException();
	}

    /**
	 * Runs test with a given key
	 * 
	 * @param key
	 * @return
	 * @throws MalformedURLException 
	 */
    @PreAuthorize("hasAuthority('user') or hasAuthority('qanairy')")
	@RequestMapping(path="/runAll", method = RequestMethod.POST)
	public @ResponseBody Map<String, Boolean> runAllTests(@RequestParam(value="test_keys", required=true) List<String> test_keys, 
														  @RequestParam(value="browser_type", required=true) String browser_type) 
																  throws MalformedURLException{
    	OrientConnectionFactory connection = new OrientConnectionFactory();
		
    	Map<String, Boolean> test_results = new HashMap<String, Boolean>();
    	for(String key : test_keys){
    		Iterator<ITest> itest_iter = Test.findByKey(key, connection).iterator();
    		
    		while(itest_iter.hasNext()){
    			ITest itest = itest_iter.next();
        		TestRecord record = null;
        		
	    		if(itest.getKey().equals(key)){
	    			TestRepository test_record = new TestRepository();
	    	
	    			Test test = test_record.convertFromRecord(itest);
	    			Browser browser = new Browser(test.getPath().firstPage().getUrl().toString().trim(), browser_type.trim());
	    			record = TestingActor.runTest(test, browser);
	    			
	    			TestRecordRepository test_record_record = new TestRecordRepository();
	    			itest.addRecord(test_record_record.convertToRecord(connection, record));
	    			itest.setCorrect(record.getPasses());
	    			Map<String, Boolean> browser_statuses = itest.getBrowserStatuses();
					browser_statuses.put(record.getBrowser(), record.getPasses());
					itest.setBrowserStatuses(browser_statuses);
	    			itest.setLastRunTimestamp(new Date());
	    			test_results.put(test.getKey(), record.getPasses());
	    			itest.setRunTime(record.getRunTime());
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
    @PreAuthorize("hasAuthority('user') or hasAuthority('qanairy')")
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
    @PreAuthorize("hasAuthority('user') or hasAuthority('qanairy')")
	@RequestMapping(path="/addGroup", method = RequestMethod.POST)
	public @ResponseBody Group addGroup(@RequestParam(value="name", required=true) String name,
										@RequestParam(value="description", required=true) String description,
										@RequestParam(value="key", required=true) String key){
    	if(name == null || name.isEmpty()){
    		throw new EmptyGroupNameException();
    	}
		Group group = new Group(name, description);
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
		igroup = group_repo.convertToRecord(orient_connection, group);
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
    @PreAuthorize("hasAuthority('user') or hasAuthority('qanairy')")
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
    @PreAuthorize("hasAuthority('user') or hasAuthority('qanairy')")
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

@ResponseStatus(HttpStatus.NOT_FOUND)
class TestControllerNotFoundException extends RuntimeException {
	private static final long serialVersionUID = 7200878662560716215L;

	public TestControllerNotFoundException() {
		super("An error occurred accessing tests endpoint.");
	}
}

@ResponseStatus(HttpStatus.NOT_FOUND)
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
		super("");
	}
}