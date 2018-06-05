package com.minion.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.qanairy.persistence.Test;
import com.qanairy.persistence.TestRecord;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;


/**
 * REST controller that defines endpoints to access data for path's experienced in the past
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
    @PreAuthorize("hasAuthority('read:test_records')")
	@RequestMapping(method = RequestMethod.GET)
	public @ResponseBody List<TestRecord> getTestRecords(@RequestParam(value="url", required=true) Test test) {
		ArrayList<TestRecord> test_records = new ArrayList<TestRecord>();
		return test_records;
	}

}
