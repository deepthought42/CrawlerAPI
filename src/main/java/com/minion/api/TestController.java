package com.minion.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.minion.persistence.ITest;
import com.minion.persistence.OrientConnectionFactory;
import com.minion.tester.Test;
import com.minion.tester.TestRecord;
import com.minion.tester.Tester;

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
	 * @return
	 */
	@RequestMapping(method = RequestMethod.GET)
	public @ResponseBody List<Test> getTests(HttpServletRequest request, 
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
	@RequestMapping(path="/updateCorrectness", method = RequestMethod.PUT)
	public @ResponseBody Test updateTest(HttpServletRequest request, 
										 @RequestParam(value="test_key", required=true) String test_key, 
										 @RequestParam(value="correct", required=true) boolean correct){

		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		Iterator<ITest> itest_iter = Test.findTestByKey(test_key, orient_connection).iterator();
		ITest itest = itest_iter.next();
		itest.setCorrect(correct);
		orient_connection.save();

		return Test.convertFromRecord(itest);
	}
	
	/**
	 * Updates the correctness of a test with the given test key
	 * 
	 * @param test
	 * @return
	 */
	@RequestMapping(path="/runTest", method = RequestMethod.POST)
	public @ResponseBody TestRecord runTest(HttpServletRequest request,
										 @RequestBody Test test){

		//OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		//Iterator<ITest> itest_iter = Test.findTestByKey(test_key, orient_connection).iterator();
		//ITest itest = itest_iter.next();
		Gson gson = new Gson();

	    Test test_pojo = gson.fromJson(test, Test.class);

		//Test test = Test.convertFromRecord(itest);
		log.info("Received Test :: " + test);
		TestRecord record = Tester.runTest(test);

		return record;
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
