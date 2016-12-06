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
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.minion.actors.TestingActor;
import com.minion.api.models.Test;
import com.minion.api.models.TestRecord;
import com.minion.browsing.Browser;
import com.minion.persistence.ITest;
import com.minion.persistence.OrientConnectionFactory;
import com.qanairy.models.Page;

/**
 * REST controller that defines endpoints to access data for path's experienced in the past
 * 
 * @author Brandon Kindred
 */
@Controller
@RequestMapping("/tester")
public class TestController {
    private static final Logger log = LoggerFactory.getLogger(TestController.class);

	/**
	 * Retrieves list of all tests from the database 
	 * @param url
	 * 
	 * @return list of tests previously discovered for given url
	 */
	@RequestMapping(method = RequestMethod.GET)
	public @ResponseBody List<Test> getTestByUrl(HttpServletRequest request, 
			   								 @RequestParam(value="url", required=true) String url) {
		List<Test> test_list = new ArrayList<Test>();
		try {
			test_list = Test.findByUrl(url);
		} catch (MalformedURLException e) {
			log.info("ERROR GETTING TEST ");
			e.printStackTrace();
		}
		
		return test_list;
	}
	
	/**
	 * Retrieves list of all tests from the database 
	 * 
	 * @param name test name to lookup
	 * 
	 * @return all tests matching name passed
	 */
	@RequestMapping(path="/name", method = RequestMethod.GET)
	public @ResponseBody List<Test> getTestsByName(HttpServletRequest request, 
			   								 @RequestParam(value="name", required=true) String name) {
		List<Test> test_list = new ArrayList<Test>();
		test_list = Test.findByName(name);
		
		return test_list;
	}

	/**
	 * Updates the correctness of a test with the given test key
	 * 
	 * @param test
	 * @return
	 */
	@RequestMapping(path="/updateCorrectness/{key}", method=RequestMethod.PUT)
	public @ResponseBody Test updateTest(HttpServletRequest request, 
										 @PathVariable(value="key") String key, 
										 @RequestParam(value="correct", required=true) boolean correct){
		System.out.println("updating correctness of test with key : " +key);
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		Iterator<ITest> itest_iter = Test.findTestByKey(key, orient_connection).iterator();
		ITest itest = itest_iter.next();
		itest.setCorrect(correct);
		orient_connection.save();

		return Test.convertFromRecord(itest);
	}
	
	/**
	 * Runs test with a given key
	 * 
	 * @param key
	 * @return
	 * @throws MalformedURLException 
	 */
	@RequestMapping(path="/runTest/{key}", method = RequestMethod.POST)
	public @ResponseBody TestRecord runTest(@PathVariable("key") String key) throws MalformedURLException{
		System.out.println("RUNNING TEST WITH KEY : " + key);
		Iterator<ITest> itest_iter = Test.findTestByKey(key).iterator();
		ITest itest = itest_iter.next();
	  
		Test test = Test.convertFromRecord(itest);
		TestRecord record = null;
		Browser browser = new Browser(((Page)test.getPath().getPath().get(0)).getUrl().toString());
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
	@RequestMapping(path="/runTestGroup/{group}", method = RequestMethod.POST)
	public @ResponseBody List<TestRecord> runTestByGroup(@PathVariable("group") String group,
														@RequestParam(value="url", required=true) String url){
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
				browser = new Browser(((Page)group_test.getPath().getPath().get(0)).getUrl().toString());
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
	@RequestMapping(path="/addGroup/{group}/{test_key}", method = RequestMethod.POST)
	public @ResponseBody Test addGroupToTest(@PathVariable("group") String group,
											 @PathVariable("test_key") String test_key){
		System.out.println("Addint GROUP to test  : " + group);
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();

		Iterator<ITest> itest_iter = Test.findTestByKey(test_key, orient_connection).iterator();
		ITest itest = itest_iter.next();
		List<String> group_list = itest.getGroups();
		if(group_list == null){
			group_list = new ArrayList<String>();
		}
		
		if(!group_list.contains(group)){
			log.info("group list doesnt contain group : " +group);
			group_list.add(group);
			itest.setGroups(group_list);
		}
		orient_connection.save();
		
		return Test.convertFromRecord(itest);
	}
	

	/**
	 * Retrieves list of all tests from the database 
	 * @param url
	 * 
	 * @return
	 */
	@RequestMapping(path="/groups", method = RequestMethod.GET)
	public @ResponseBody List<String> getGroups(HttpServletRequest request, 
			   								 @RequestParam(value="url", required=true) String url) {
		List<Test> test_list = new ArrayList<Test>();
		List<String> groups = new ArrayList<String>();
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
