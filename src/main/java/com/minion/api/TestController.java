package com.minion.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
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
import com.qanairy.models.dto.TestRepository;
import com.qanairy.models.dto.exceptions.UnknownAccountException;
import com.qanairy.persistence.IDomain;
import com.qanairy.persistence.IGroup;
import com.qanairy.persistence.ITest;
import com.qanairy.persistence.OrientConnectionFactory;
import com.qanairy.services.AccountService;
import com.qanairy.services.DomainService;
import com.minion.browsing.Browser;
import com.qanairy.api.exception.DomainNotOwnedByAccountException;
import com.qanairy.models.Account;
import com.qanairy.models.Domain;
import com.qanairy.models.Group;
import com.qanairy.models.Page;

/**
 * REST controller that defines endpoints to access tests
 */
@Controller
@Scope("session")
@RequestMapping("/tests")
public class TestController {
	private static Logger log = LogManager.getLogger(TestController.class);

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
    @PreAuthorize("hasAuthority('trial') or hasAuthority('qanairy')")
	@RequestMapping(method = RequestMethod.GET)
	public @ResponseBody List<Test> getTestByDomain(HttpServletRequest request, 
			   								 @RequestParam(value="url", required=true) String url) throws UnknownAccountException, DomainNotOwnedByAccountException {
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

		/*
		Domain domain = domain_repo.find(new OrientConnectionFactory(), url);
    	List<Test> tests = domain.getTests();
    	
		List<Test> verified_tests = new ArrayList<Test>();
		for(Test test : tests){
			//ITest test = test_records.next();
			if(test.isCorrect() != null){
				verified_tests.add(test);
			}
		}
		*/
		return verified_tests;
    }
	
	/**
	 * Retrieves list of all tests from the database 
	 * 
	 * @param name test name to lookup
	 * 
	 * @return all tests matching name passed
	 */
    @PreAuthorize("hasAuthority('trial') or hasAuthority('qanairy')")
	@RequestMapping(path="/name", method = RequestMethod.GET)
	public @ResponseBody List<Test> getTestsByName(HttpSession session, HttpServletRequest request, 
			   								 		@RequestParam(value="name", required=true) String name) {
		//session.setAttribute(Constants.FOO, new Domain());
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
    @PreAuthorize("hasAuthority('trial') or hasAuthority('qanairy')")
	@RequestMapping(path="/unverified", method = RequestMethod.GET)
	public @ResponseBody List<Test> getUnverifiedTests(HttpServletRequest request, 
			   								 @RequestParam(value="url", required=true) String url) throws DomainNotOwnedByAccountException, UnknownAccountException {
		
    	//make sure domain belongs to user account first
    	final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        final Auth0UserDetails currentUser = (Auth0UserDetails) authentication.getPrincipal();

    	Account acct = accountService.find(currentUser.getUsername());
    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	
       /*	boolean owned_by_acct = false;
    	for(Domain domain_rec : acct.getDomains()){
    		if(domain_rec.getUrl().equals(url)){
    			owned_by_acct = true;
    			break;
    		}
    	}
    	if(!owned_by_acct){
    		throw new DomainNotOwnedByAccountException();
    	}
        	*/

		DomainRepository domain_repo = new DomainRepository();
		IDomain idomain = domain_repo.find(url);

    	Domain domain = domain_repo.find(new OrientConnectionFactory(), url);
		Iterator<ITest> tests = idomain.getTests().iterator();
		TestRepository test_repo = new TestRepository();
		
		//	List<Test> tests = domain.getTests();
    	//
		List<Test> unverified_tests = new ArrayList<Test>();
		/*for(Test test : tests){
			//ITest test = test_records.next();
			if(test.isCorrect() == null){
				unverified_tests.add(test);
			}
		}
*/
		while(tests.hasNext()){
			ITest itest = tests.next();
			if(itest.getCorrect() == null){
				unverified_tests.add(test_repo.convertFromRecord(itest));
			}
		}
		System.out.println("# of unverified tests for domain" + unverified_tests.size());
		return unverified_tests;
	}

	/**
	 * Updates the correctness of a test with the given test key
	 * 
	 * @param test
	 * @return
	 */
    @PreAuthorize("hasAuthority('trial') or hasAuthority('qanairy')")
	@RequestMapping(path="/updateCorrectness/{key}", method=RequestMethod.PUT)
	public @ResponseBody Test updateTest(HttpServletRequest request, 
										 @PathVariable(value="key") String key, 
										 @RequestParam(value="correct", required=true) boolean correct){
		System.out.println("updating correctness of test with key : " +key);
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		Iterator<ITest> itest_iter = Test.findByKey(key, orient_connection).iterator();
		ITest itest = itest_iter.next();
		itest.setCorrect(correct);
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
    @PreAuthorize("hasAuthority('trial') or hasAuthority('qanairy')")
	@RequestMapping(path="/runTest/{key}", method = RequestMethod.POST)
	public @ResponseBody TestRecord runTest(@PathVariable("key") String key, 
											@RequestParam("browser_type") String browser_type) throws MalformedURLException{
		System.out.println("RUNNING TEST WITH KEY : " + key);
		Iterator<ITest> itest_iter = Test.findByKey(key, new OrientConnectionFactory()).iterator();
		ITest itest = itest_iter.next();
		TestRepository test_record = new TestRepository();

		Test test = test_record.convertFromRecord(itest);
		TestRecord record = null;
		Browser browser = new Browser(((Page)test.getPath().getPath().get(0)).getUrl().toString(), browser_type);
		log.info(" Test Received :: " + test);
		record = TestingActor.runTest(test, browser);
		browser.close();

		return record;
	}

	/**
	 * Finds all tests by group and runs them
	 * 
	 * @param group name of group that is associated with tests that should be ran
	 * @param url domain that group should be ran for
	 * 
	 * @return {@link TestRecord records} that define the results of the tests. 
	 */
    @PreAuthorize("hasAuthority('trial') or hasAuthority('qanairy')")
	@RequestMapping(path="/runTestGroup/{group}", method = RequestMethod.POST)
	public @ResponseBody List<TestRecord> runTestByGroup(@PathVariable("group") String group,
														@RequestParam(value="url", required=true) String url,
														@RequestParam(value="browser_type", required=true) String browser_type){
		System.out.println("RUNNING TEST IN GROUP  : " + group);
		
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
			log.info("ERROR GETTING TEST ");
			e.printStackTrace();
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
    @PreAuthorize("hasAuthority('trial') or hasAuthority('qanairy')")
	@RequestMapping(path="/addGroup", method = RequestMethod.POST)
	public @ResponseBody Test addGroup(@RequestParam(value="name", required=true) String name,
										@RequestParam(value="description", required=true) String description,
										@RequestParam(value="key", required=true) String key){
		System.out.println("Adding GROUP to test  : " + name);
		Group group = new Group(name, description);
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();

		Iterator<ITest> itest_iter = Test.findByKey(key, orient_connection).iterator();
		ITest itest = itest_iter.next();
		Iterator<IGroup> group_iter = itest.getGroups().iterator();
		
		GroupRepository group_repo = new GroupRepository();
		while(itest_iter.hasNext()){
			itest = itest_iter.next();
		}
		//if(group_iter == null || !group_iter.hasNext()){
			itest.addGroup(group_repo.convertToRecord(orient_connection, group));
			orient_connection.save();
		//}
		
		TestRepository test_record = new TestRepository();

		return test_record.convertFromRecord(itest);
	}
	

	/**
	 * Retrieves list of all tests from the database 
	 * @param url
	 * 
	 * @return
	 */
    @PreAuthorize("hasAuthority('trial') or hasAuthority('qanairy')")
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
			log.info("ERROR GETTING TEST ");
			e.printStackTrace();
		}
		
		return groups;
	}
}

@ResponseStatus(HttpStatus.NOT_FOUND)
class TestControllerNotFoundException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7200878662560716215L;

	public TestControllerNotFoundException() {
		super("An error occurred accessing tests endpoint.");
	}
}
