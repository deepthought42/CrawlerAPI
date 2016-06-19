package api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;

import tester.Test;
import tester.TestRecord;


/**
 * REST controller that defines endpoints to access data for path's experienced in the past
 * 
 * @author Brandon Kindred
 */
@Controller
@RequestMapping("/tests/records")
public class TestRecordController {

	/**
 	 * Retrieves list of all test records for a test from the database
 	 * 
	 * @param url
	 * @return
	 */
	@RequestMapping(method = RequestMethod.GET)
	public @ResponseBody List<TestRecord> getTestRecords(@RequestParam(value="url", required=true) Test test) {
		ArrayList<TestRecord> test_records = new ArrayList<TestRecord>();
		return test_records;
	}

	/**
	 * Updates a {@link TestRecord}
	 * 
	 * @param test
	 * @return
	 */
	@RequestMapping(method = RequestMethod.PUT)
	public @ResponseBody TestRecord updateTest(@RequestParam(value="test", required=true) TestRecord test_record){
		
		
		//Memory
		//Find test
		//update test correctness
		// return test
		return test_record;
	}

}

@ResponseStatus(HttpStatus.NOT_FOUND)
class TestCoordinatorNotFoundException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7200878662560716215L;

	public TestCoordinatorNotFoundException() {
		super("could not find user .");
	}
}
