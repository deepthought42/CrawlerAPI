package com.minion.api;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.qanairy.models.Account;
import com.qanairy.models.PageElement;
import com.qanairy.models.PageState;
import com.qanairy.models.ScreenshotSet;
import com.qanairy.models.repository.PageStateRepository;

/**
 *	API for interacting with {@link User} data
 */
@RestController
@RequestMapping("/pagestates")
public class PageStateController {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	private PageStateRepository page_repo;
	/**
     * Create new account
     * 
     * @param authorization_header
     * @param url 
     * @param screenshot_url
     * @param browser_name
     * 
     * @return
	 * @throws IOException 
     * @throws Exception 
     */
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<PageState> create( @RequestParam(value="url", required=true) String url,
    										  @RequestParam(value="screenshot_url", required=true) String screenshot_url,
    										  @RequestParam(value="browser", required=true) String browser_name) throws IOException {
		ScreenshotSet screenshot = new ScreenshotSet(screenshot_url, browser_name);
		Set<ScreenshotSet> scrnshots = new HashSet<ScreenshotSet>();
		scrnshots.add(screenshot);
		Set<PageElement> page_elements = new HashSet<PageElement>();
    	PageState page_state = new PageState("", url, scrnshots, page_elements);
    	PageState page = page_repo.save(page_state);
    	return new ResponseEntity<>(page, HttpStatus.ACCEPTED );
	}
}