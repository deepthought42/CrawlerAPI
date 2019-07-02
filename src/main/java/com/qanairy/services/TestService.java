package com.qanairy.services;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openqa.grid.common.exception.GridException;
import org.openqa.selenium.WebDriverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.minion.api.MessageBroadcaster;
import com.minion.browsing.Browser;
import com.minion.browsing.Crawler;
import com.qanairy.models.Account;
import com.qanairy.api.exceptions.PagesAreNotMatchingException;
import com.qanairy.models.Action;
import com.qanairy.models.Animation;
import com.qanairy.models.Domain;
import com.qanairy.models.ElementState;
import com.qanairy.models.Group;
import com.qanairy.models.PageLoadAnimation;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Redirect;
import com.qanairy.models.Test;
import com.qanairy.models.TestRecord;
import com.qanairy.models.enums.BrowserEnvironment;
import com.qanairy.models.enums.TestStatus;
import com.qanairy.models.repository.AccountRepository;
import com.qanairy.models.repository.TestRecordRepository;
import com.qanairy.models.repository.TestRepository;
import com.segment.analytics.Analytics;
import com.segment.analytics.messages.IdentifyMessage;
import com.segment.analytics.messages.TrackMessage;

@Component
public class TestService {
	private static Logger log = LoggerFactory.getLogger(TestService.class);

	@Autowired
	private AccountRepository account_repo;

	@Autowired
	private DomainService domain_service;

	@Autowired
	private TestRepository test_repo;

	@Autowired
	private ActionService action_service;

	@Autowired
	private PageStateService page_state_service;

	@Autowired
	private ElementStateService page_element_service;

	@Autowired
	private BrowserService browser_service;

	@Autowired
	private RedirectService redirect_service;

	@Autowired
	private AnimationService animation_service;

	@Autowired
	private TestService test_service;

	@Autowired
	private TestRecordRepository test_record_repo;

	@Autowired
	private PageLoadAnimationService page_load_animation_service;
	
	@Autowired
	private Crawler crawler;

	/**
	 * Runs an {@code Test}
	 *
	 * @param test test to be ran
	 *
	 * @pre test != null
	 * @return	{@link TestRecord} indicating passing status and {@link Page} if not passing
	 * @throws NoSuchAlgorithmException
	 * @throws WebDriverException
	 * @throws GridException
	 */
	 public TestRecord runTest(Test test, String browser_name, TestStatus last_test_status) {
		 assert test != null;

		 TestStatus passing = null;
		 PageState page = null;
		 TestRecord test_record = null;
		 final long pathCrawlStartTime = System.currentTimeMillis();

		 int cnt = 0;
		 boolean pages_dont_match = false;
		 Browser browser = null;
		 Map<Integer, ElementState> visible_element_map = new HashMap<>();
		 List<ElementState> visible_elements = new ArrayList<>();
		 
		 do{
			 try {
				browser = browser_service.getConnection(browser_name.trim(), BrowserEnvironment.TEST);
				page = crawler.crawlPath(test.getPathKeys(), test.getPathObjects(), browser, null, visible_element_map, visible_elements);
			 } catch(PagesAreNotMatchingException e){
				 log.warn(e.getLocalizedMessage());
				 pages_dont_match = true;
			 }
			 catch (Exception e) {
				 log.error(e.getLocalizedMessage());
			 }
			 finally{
				 if(browser != null){
					 browser.close();
				 }
			 }

			 cnt++;
		 }while(cnt < Integer.MAX_VALUE && page == null);

		 final long pathCrawlEndTime = System.currentTimeMillis();
		 long pathCrawlRunTime = pathCrawlEndTime - pathCrawlStartTime;

		 if(pages_dont_match){
			return new TestRecord(new Date(), TestStatus.FAILING, browser_name.trim(), page, pathCrawlRunTime);
		 }
		 else{
			 passing = Test.isTestPassing(test.getResult(), page, last_test_status);
	 		 test_record = new TestRecord(new Date(), passing, browser_name.trim(), page, pathCrawlRunTime);

			 return test_record;
		 }
	 }

	 public Test save(Test test, String host_url){
		Test record = test_repo.findByKey(test.getKey());

		if(record == null){
			List<PathObject> path_objects = new ArrayList<PathObject>();
			for(PathObject path_obj : test.getPathObjects()){
				if(path_obj instanceof PageState){
					path_objects.add(page_state_service.save((PageState)path_obj));
				}
				else if(path_obj instanceof ElementState){
					path_objects.add(page_element_service.save((ElementState)path_obj));
				}
				else if(path_obj instanceof Action){
					path_objects.add(action_service.save((Action)path_obj));
				}
				else if(path_obj instanceof Redirect){
					path_objects.add(redirect_service.save((Redirect)path_obj));
				}
				else if(path_obj instanceof Animation){
					path_objects.add(animation_service.save((Animation)path_obj));
				}
				else if(path_obj instanceof PageLoadAnimation){
					path_objects.add(page_load_animation_service.save((PageLoadAnimation)path_obj));
				}
			}
			test.setPathObjects(path_objects);
			test.setResult(page_state_service.save(test.getResult()));

			if(test.getName() == null || test.getName().isEmpty()){
				test.setName("Test #" + (domain_service.getTestCount(host_url)+1));
			}

	  		test = test_repo.save(test);
			domain_service.addTest(host_url, test);

			try {
				MessageBroadcaster.broadcastDiscoveredTest(test, host_url);
			} catch (JsonProcessingException e) {
				log.error(e.getLocalizedMessage());
			}

			for(PathObject path_obj : test.getPathObjects()){
				try {
					MessageBroadcaster.broadcastPathObject(path_obj, host_url);
				} catch (JsonProcessingException e) {
					log.error(e.getLocalizedMessage());
				}
			}
		}
		else{
			log.info("Test already exists  !!!!!!!");
			try {
				MessageBroadcaster.broadcastTest(test, host_url);
			} catch (JsonProcessingException e) {
				log.error(e.getLocalizedMessage());
			}

			List<PathObject> path_objects = test_repo.getPathObjects(test.getKey());
			record.setPathObjects(path_objects);
			record.setResult(page_state_service.findByKey(test.getResult().getKey()));
			record.setGroups(test.getGroups());

			test = test_repo.save(record);

		}

		return test;
	}

	 public List<TestRecord> runAllTests(Account acct, Domain domain) {
		Analytics analytics = Analytics.builder("TjYM56IfjHFutM7cAdAEQGGekDPN45jI").build();
    	Map<String, String> traits = new HashMap<String, String>();
        traits.put("account", acct.getUsername());
        traits.put("api_key", acct.getApiToken());
    	analytics.enqueue(IdentifyMessage.builder()
		    .userId(acct.getUsername())
		    .traits(traits)
		);

		//Fire discovery started event
    	Set<Test> tests = domain_service.getVerifiedTests(domain.getUrl());
	   	Map<String, String> run_test_batch_props= new HashMap<String, String>();
	   	run_test_batch_props.put("total tests", Integer.toString(tests.size()));
	   	analytics.enqueue(TrackMessage.builder("Running tests")
   		    .userId(acct.getUsername())
   		    .properties(run_test_batch_props)
   		);

    	Map<String, TestRecord> test_results = new HashMap<String, TestRecord>();
    	List<TestRecord> test_records = new ArrayList<TestRecord>();

    	for(Test test : tests){
			TestRecord record = test_service.runTest(test, domain.getDiscoveryBrowserName(), test.getStatus());

			test_results.put(test.getKey(), record);
			TestStatus is_passing = TestStatus.PASSING;
			//update overall passing status based on all browser passing statuses
			for(String status : test.getBrowserStatuses().values()){
				if(status.equals(TestStatus.UNVERIFIED) || status.equals(TestStatus.FAILING)){
					is_passing = TestStatus.FAILING;
					break;
				}
			}

    		record = test_record_repo.save(record);
    		test_records.add(record);

	    	test.getBrowserStatuses().put(record.getBrowser(), record.getStatus().toString());

	    	test.addRecord(record);
			test.setStatus(is_passing);
			test.setLastRunTimestamp(new Date());
			test.setRunTime(record.getRunTime());
			test_repo.save(test);

			acct.addTestRecord(record);
			account_repo.save(acct);
   		}

    	return test_records;
	 }

   public List<Test> findTestsWithElementState(String page_state_key, String element_state_key){
     return test_repo.findTestWithElementState(page_state_key, element_state_key);
   }

   public void init(Crawler crawler, BrowserService browser_service){
     this.crawler = crawler;
     this.browser_service = browser_service;
   }

   public Test findByKey(String key){
     return test_repo.findByKey(key);
   }

   public List<Test> findTestsWithPageState(String key) {
     return test_repo.findTestWithPageState(key);
   }

   public List<PathObject> getPathObjects(String test_key) {
     return test_repo.getPathObjects(test_key);
   }

   public Set<Group> getGroups(String key) {
     return test_repo.getGroups(key);
   }
}
