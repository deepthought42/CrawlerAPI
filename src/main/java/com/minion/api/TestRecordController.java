package com.minion.api;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import com.qanairy.models.Test;
import com.qanairy.models.TestRecord;
import com.qanairy.models.repository.TestRecordRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.util.IterableUtils;
import org.springframework.security.access.prepost.PreAuthorize;


/**
 * REST controller that defines endpoints to access data for path's experienced in the past
 */
@RestController
@RequestMapping("/tests/records")
public class TestRecordController {

	@Autowired
	TestRecordRepository test_record_repo;
	
	/**
 	 * Retrieves list of all test records for a test from the database
 	 * 
	 * @param url
	 * @return
	 */
    @PreAuthorize("hasAuthority('read:test_records')")
	@RequestMapping(method = RequestMethod.GET)
	public @ResponseBody List<TestRecord> getTestRecords(@RequestParam(value="url", required=true) Test test) {
		return IterableUtils.toList(test_record_repo.findAll());
	}

}
