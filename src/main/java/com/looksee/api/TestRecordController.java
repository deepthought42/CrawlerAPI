package com.looksee.api;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.looksee.api.exceptions.DomainNotOwnedByAccountException;
import com.looksee.models.Test;
import com.looksee.models.TestRecord;
import com.looksee.models.dto.exceptions.UnknownAccountException;
import com.looksee.models.repository.TestRecordRepository;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections4.IterableUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;


/**
 * REST controller that defines endpoints to access data for path's experienced in the past
 */
@RestController
@RequestMapping("/testrecords")
public class TestRecordController {

	@Autowired
	private TestRecordRepository test_record_repo;
	
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

    
    /**
	 * Retrieves list of all tests from the database 
	 * 
	 * @param name test name to lookup
	 * 
	 * @return all tests matching name passed
	 * @throws DomainNotOwnedByAccountException 
	 * @throws UnknownAccountException 
	 */
    @PreAuthorize("hasAuthority('read:test_records')")
	@RequestMapping(path="$key", method = RequestMethod.GET)
	public @ResponseBody TestRecord getTestRecord(HttpServletRequest request, 
														@PathVariable(value="key", required=true) String key) {
    	return test_record_repo.findByKey(key);
	}
}
