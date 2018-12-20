package com.minion.actors;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.minion.api.MessageBroadcaster;
import com.minion.browsing.Browser;
import com.minion.browsing.Crawler;
import com.minion.browsing.form.ElementRuleExtractor;
import com.qanairy.models.Action;
import com.qanairy.models.Attribute;
import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.Form;
import com.qanairy.models.PageElement;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Test;
import com.qanairy.models.repository.ActionRepository;
import com.qanairy.models.repository.DiscoveryRecordRepository;
import com.qanairy.models.repository.PageElementRepository;
import com.qanairy.models.rules.NumericRule;
import com.qanairy.models.rules.Rule;
import com.qanairy.models.rules.RuleType;
import com.qanairy.services.BrowserService;
import com.qanairy.services.TestService;
import com.minion.structs.Message;

import akka.actor.AbstractActor;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class GeneralFormTestDiscoveryActor extends AbstractActor {
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	
	@Autowired
	private Crawler crawler;
	
	@Autowired
	private BrowserService browser_service;
	
	@Autowired
	private TestService test_service;
	
	@Autowired
	private ActionRepository action_repo;
	
	@Autowired
	private PageElementRepository page_element_repo;
	
	@Autowired
	private DiscoveryRecordRepository discovery_repo;
	
	@Autowired
	private ElementRuleExtractor extractor;
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(Message.class, message -> {
					if(message.getData() instanceof Test){
						Test test = ((Test)message.getData());
			
						//get first page in path
						PageState page = test.firstPage();
			
						int cnt = 0;
					  	Browser browser = null;
					  	
					  	while(browser == null && cnt < 50000){
					  		try{
						  		browser = new Browser(message.getOptions().get("browser").toString());
						  		browser.navigateTo(page.getUrl());
						  		
						  		List<Form> forms = browser_service.extractAllForms(test.getResult(), browser);
							  	List<List<PathObject>> path_object_lists = new ArrayList<List<PathObject>>();
							  	for(Form form : forms){
							  		path_object_lists.addAll(generateAllFormTestPaths(test, form));
							  	}
							  	
							  	try{
							  		browser.close();
							  	}
							  	catch(Exception e){}
							  	
							  	//Evaluate all tests now
							  	List<Test> tests = new ArrayList<Test>();
							  	for(List<PathObject> path_obj_list : path_object_lists){
							  		List<String> path_keys = new ArrayList<String>(test.getPathKeys());
							  		
							  		List<PathObject> test_path_objects = new ArrayList<PathObject>(test.getPathObjects());
							  		for(PathObject obj : path_obj_list){
							  			path_keys.add(obj.getKey());
				
							  			if(obj.getType().equals("PageElement")){
							  				PageElement page_elem = (PageElement)obj;
							  				PageElement elem_record = page_element_repo.findByKey(obj.getKey());
							  				if(elem_record == null){
							  					elem_record = page_element_repo.save(page_elem);
							  				}
							  				test_path_objects.add(page_elem);
							  			}
							  			else if(obj.getType().equals("Action")){
							  				Action action = (Action)obj;
							  				Action action_record = action_repo.findByKey(obj.getKey());
							  				if(action_record == null){
							  					action_record = action_repo.save(action);
							  					MessageBroadcaster.broadcastPathObject(action_record, message.getOptions().get("host").toString());
							  				}
							  				test_path_objects.add(action_record);
							  			}
							  		}
							  		
									final long pathCrawlStartTime = System.currentTimeMillis();
									
							  		log.info("Crawling potential form test path");
							  		browser = new Browser(message.getOptions().get("browser").toString());
							  		
							  		cnt = 0;
							  		PageState result_page = null;
							  		do{
							  			try{
							  				result_page = crawler.crawlPath(path_keys, test_path_objects, browser, message.getOptions().get("host").toString());
							  				break;
							  			}catch(Exception e){
							  				log.warning("Exception occurred while crawling path -- "+e.getLocalizedMessage());
							  			}
						  			}while(cnt < 1000 && result_page == null);
							  		
							  		final long pathCrawlEndTime = System.currentTimeMillis();
									long crawl_time_in_ms = pathCrawlEndTime - pathCrawlStartTime;
									
									try{
								  		browser.close();
								  	}
								  	catch(Exception e){}
									
							  		Test new_test = new Test(path_keys, test_path_objects, result_page, null, false, test.getSpansMultipleDomains());
				
							  		new_test.setRunTime(crawl_time_in_ms);
							  		new_test.setLastRunTimestamp(test.getLastRunTimestamp());
							  		
							  		new_test = test_service.save(new_test, message.getOptions().get("host").toString());
							  		tests.add(new_test);
							  		
							  		DiscoveryRecord discovery_record = discovery_repo.findByKey(message.getOptions().get("discovery_key").toString());
									discovery_record.setTestCount(discovery_record.getTestCount()+1);
									discovery_record = discovery_repo.save(discovery_record);
									MessageBroadcaster.broadcastDiscoveryStatus(discovery_record);  	
							  	}
								break;
							}catch(NullPointerException e){
								log.warning(e.getLocalizedMessage());
							}
							cnt++;
						}					  	
					}
				})
				.match(MemberUp.class, mUp -> {
					log.info("Member is Up: {}", mUp.member());
				})
				.match(UnreachableMember.class, mUnreachable -> {
					log.info("Member detected as unreachable: {}", mUnreachable.member());
				})
				.match(MemberRemoved.class, mRemoved -> {
					log.info("Member is Removed: {}", mRemoved.member());
				})	
				.matchAny(o -> {
					log.info("received unknown message");
				})
				.build();
	}
	
	/**
	 * 
	 * @param test
	 * @param form
	 * @return
	 */
	public List<List<PathObject>> generateAllFormTestPaths(Test test, Form form){
		List<List<PathObject>> form_tests = new ArrayList<List<PathObject>>();
		log.info("FORM FIELDS COUNT     :::    "+form.getFormFields().size());
		//for each field in the complex field generate a set of tests for all known rules
		for(PageElement input_elem : form.getFormFields()){
			
			boolean field_exists = false;
			
			//CHECK IF FORM FIELD ALREADY EXISTS IN PATH
			for(PathObject path_obj : test.getPathObjects()){
				if(path_obj instanceof PageElement){
					PageElement page_elem = (PageElement)path_obj;
					if(page_elem.equals(input_elem)){
						field_exists = true;
						break;
					}
				}
			}
			
			if(field_exists){
				log.info("FORM FIELD ALREADY EXISTS IN PATH  :: "+field_exists);
				continue;
			}
			List<Rule> rules = extractor.extractInputRules(input_elem);
			log.info("Total RULES   :::   "+rules.size());
			for(Rule rule : rules){
				List<List<PathObject>> path_list = generateFormRuleTests(input_elem, rule, form.getSubmitField());
				form_tests.addAll(path_list);
			}
			log.info("FORM TESTS    :::   "+form_tests.size());
		}
		return form_tests;
	}
	

	public static List<List<PathObject>> generateLengthBoundaryTests(PageElement input, Rule rule){
		List<List<PathObject>> tests = new ArrayList<List<PathObject>>();

		if(rule.getType().equals(RuleType.MAX_LENGTH)){
			//generate empty string test
			List<PathObject> path_obj_list = new ArrayList<PathObject>();
			path_obj_list.add(input);
			path_obj_list.add(new Action("click", ""));
			path_obj_list.add(input);

			//generate string with length equal to MAX_LENGTH
			String short_str = NumericRule.generateRandomAlphabeticString(Integer.parseInt(rule.getValue()));
			
			path_obj_list.add(new Action("sendKeys", short_str));
			tests.add(path_obj_list);

			//generate single character str test
			path_obj_list = new ArrayList<PathObject>();
			path_obj_list.add(input);
			path_obj_list.add(new Action("click", ""));
			path_obj_list.add(input);

			//generate string with length that is 1 character greater than MAX_LENGTH
			String large_str = NumericRule.generateRandomAlphabeticString(Integer.parseInt(rule.getValue())+1);
			path_obj_list.add(new Action("sendKeys", large_str));
			tests.add(path_obj_list);
		}
		else if(rule.getType().equals(RuleType.MIN_LENGTH)){
			//generate empty string test
			List<PathObject> path_obj_list = new ArrayList<PathObject>();
			path_obj_list.add(input);
			path_obj_list.add(new Action("click", ""));
			path_obj_list.add(input);
			
			//generate string with length equal to MAX_LENGTH
			String short_str = NumericRule.generateRandomAlphabeticString(Integer.parseInt(rule.getValue()));

			path_obj_list.add(new Action("sendKeys", short_str));
			tests.add(path_obj_list);
			
			//generate single character str test
			path_obj_list = new ArrayList<PathObject>();
			path_obj_list.add(input);
			path_obj_list.add(new Action("click", ""));
			path_obj_list.add(input);
			
			//generate string with length that is 1 character greater than MAX_LENGTH
			String large_str = NumericRule.generateRandomAlphabeticString(Integer.parseInt(rule.getValue())-1);

			path_obj_list.add(new Action("sendKeys", large_str));
			tests.add(path_obj_list);
		}
		else if(rule.getType().equals(RuleType.MAX_VALUE)){
			//generate empty string test
			List<PathObject> path_obj_list = new ArrayList<PathObject>();
			path_obj_list.add(input);
			path_obj_list.add(new Action("click", ""));
			path_obj_list.add(input);
			
			//generate string with length equal to MAX_LENGTH
			path_obj_list.add(new Action("sendKeys", Integer.toString(Integer.parseInt(rule.getValue()))));
			tests.add(path_obj_list);
			
			//generate single character str test
			path_obj_list = new ArrayList<PathObject>();
			path_obj_list.add(input);
			path_obj_list.add(new Action("click", ""));
			path_obj_list.add(input);
			
			//generate string with length that is 1 character greater than MAX_LENGTH
			path_obj_list.add(new Action("sendKeys", Integer.toString(Integer.parseInt(rule.getValue())+1)));
			tests.add(path_obj_list);
		}
		else if(rule.getType().equals(RuleType.MIN_VALUE)){
			//generate empty string test
			List<PathObject> path_obj_list = new ArrayList<PathObject>();
			path_obj_list.add(input);
			path_obj_list.add(new Action("click", ""));
			path_obj_list.add(input);
			
			//generate string with length equal to MAX_LENGTH
			path_obj_list.add(new Action("sendKeys", Integer.toString(Integer.parseInt(rule.getValue()))));
			tests.add(path_obj_list);
			
			//generate single character str test
			path_obj_list = new ArrayList<PathObject>();
			path_obj_list.add(input);
			path_obj_list.add(new Action("click", ""));
			path_obj_list.add(input);
			
			//generate string with length that is 1 character greater than MAX_LENGTH
			path_obj_list.add(new Action("sendKeys", Integer.toString(Integer.parseInt(rule.getValue())-1)));
			tests.add(path_obj_list);
		}
		return tests;
		
	}
	

	/**
	 * Generates a test with an alphabetic character to verify an alphabetic restriction
	 * @param input_elem
	 * @param rule
	 * @return
	 */
	private static List<List<PathObject>> generateAlphabeticRestrictionTests(PageElement input_elem, Rule rule) {
		//generate single character str test		
		List<List<PathObject>> tests = new ArrayList<List<PathObject>>();
		List<PathObject> path_obj_list = new ArrayList<PathObject>();
		path_obj_list.add(input_elem);
		path_obj_list.add(new Action("click", ""));
		path_obj_list.add(input_elem);
		path_obj_list.add(new Action("sendKeys", "a"));
		tests.add(path_obj_list);		
		return tests;
	}

	private static List<List<PathObject>> generateNumericRestrictionTests(PageElement input_elem, Rule rule) {
		List<List<PathObject>> tests = new ArrayList<List<PathObject>>();

		List<PathObject> path_obj_list = new ArrayList<PathObject>();
		path_obj_list.add(input_elem);
		path_obj_list.add(new Action("click", ""));
		path_obj_list.add(input_elem);
		path_obj_list.add(new Action("sendKeys", "0"));
		tests.add(path_obj_list);
		
		return tests;
	}

	private static List<List<PathObject>> generateSpecialCharacterRestrictionTests(PageElement input_elem, Rule rule) {
		//generate single character str test
		List<List<PathObject>> tests = new ArrayList<List<PathObject>>();
		List<PathObject> path_obj_list = new ArrayList<PathObject>();
		path_obj_list.add(input_elem);
		path_obj_list.add(new Action("click", ""));
		path_obj_list.add(input_elem);
		path_obj_list.add(new Action("sendKeys", "!"));
		tests.add(path_obj_list);
		
		return tests;
	}

	private static List<List<PathObject>> generateEnabledTests(PageElement input_elem, Rule rule) {
		// TODO Auto-generated method stub
		return null;
	}

	private static List<List<PathObject>> generateReadOnlyTests(PageElement input_elem, Rule rule) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private static List<List<PathObject>> generatePatternTests(PageElement input_elem, Rule rule) {
		// TODO Auto-generated method stub
		return null;
	}

	private static List<List<PathObject>> generateEmailTests(PageElement input_elem, Rule rule) {
		List<List<PathObject>> tests = new ArrayList<List<PathObject>>();

		List<PathObject> path_obj_list = new ArrayList<PathObject>();
		path_obj_list.add(input_elem);
		path_obj_list.add(new Action("click", ""));
		path_obj_list.add(input_elem);
		path_obj_list.add(new Action("sendKeys", "!test@test.com"));
		tests.add(path_obj_list);		

		//generate single character str test	
		List<PathObject> path_obj_list1 = new ArrayList<PathObject>();
		path_obj_list1.add(input_elem);
		path_obj_list1.add(new Action("click", ""));
		path_obj_list1.add(input_elem);
		path_obj_list1.add(new Action("sendKeys", "test!test.com"));
		tests.add(path_obj_list1);

		List<PathObject> path_obj_list2 = new ArrayList<PathObject>();
		path_obj_list2.add(input_elem);
		path_obj_list2.add(new Action("click", ""));
		path_obj_list2.add(input_elem);
		path_obj_list2.add(new Action("sendKeys", "test@test"));
		tests.add(path_obj_list2);
		
		List<PathObject> path_obj_list3 = new ArrayList<PathObject>();
		path_obj_list3.add(input_elem);
		path_obj_list3.add(new Action("click", ""));
		path_obj_list3.add(input_elem);
		path_obj_list3.add(new Action("sendKeys", "test.test@test"));
		tests.add(path_obj_list3);
		
		List<PathObject> path_obj_list4 = new ArrayList<PathObject>();
		path_obj_list4.add(input_elem);
		path_obj_list4.add(new Action("click", ""));
		path_obj_list4.add(input_elem);
		path_obj_list4.add(new Action("sendKeys", "test_test@test"));
		tests.add(path_obj_list4);
		
		return tests;
	}

	/**
	 * Generates rule tests for a given {@link PageElement} and {@link Rule}
	 * 
	 * @param input_elem
	 * @param rule
	 * @return
	 */
	public static List<List<PathObject>> generateFormRuleTests(PageElement input_elem, Rule rule, PageElement submitField){
		assert rule != null;
		
		List<List<PathObject>> tests = new ArrayList<List<PathObject>>();
		if(rule.getType().equals(RuleType.REQUIRED)){
			//generate required path for element type
			tests.addAll(generateRequirementChecks(input_elem, true, submitField));
		}
		else if(rule.getType().equals(RuleType.ALPHABETIC_RESTRICTION)){
			tests.addAll(generateAlphabeticRestrictionTests(input_elem, rule, submitField));
		}
		else if(rule.getType().equals(RuleType.NUMERIC_RESTRICTION)){
			tests.addAll(generateNumericRestrictionTests(input_elem, rule, submitField));
		}
		else if(rule.getType().equals(RuleType.SPECIAL_CHARACTER_RESTRICTION)){
			tests.addAll(generateSpecialCharacterRestrictionTests(input_elem, rule, submitField));
		}
		else if(rule.getType().equals(RuleType.DISABLED)){
			//path.addAll(generateEnabledTests(input_elem, rule));
		}
		else if(rule.getType().equals(RuleType.READ_ONLY)){
			//path.addAll(generateReadOnlyTests(input_elem, rule));
		}
		else if(rule.getType().equals(RuleType.MAX_LENGTH)){
			tests.addAll(generateLengthBoundaryTests(input_elem, rule, submitField));
		}
		else if(rule.getType().equals(RuleType.MIN_LENGTH)){
			tests.addAll(generateLengthBoundaryTests(input_elem, rule, submitField));
		}
		else if(rule.getType().equals(RuleType.MAX_VALUE)){
			tests.addAll(generateLengthBoundaryTests(input_elem, rule, submitField));
		}
		else if(rule.getType().equals(RuleType.MIN_VALUE)){
			tests.addAll(generateLengthBoundaryTests(input_elem, rule, submitField));
		}
		else if(rule.getType().equals(RuleType.PATTERN)){
			tests.addAll(generatePatternTests(input_elem, rule, submitField));
		}
		else if(rule.getType().equals(RuleType.EMAIL_PATTERN)){
			tests.addAll(generateEmailTests(input_elem, rule, submitField));	
		}
		return tests;
	}
	
	/**
	 * Generates rule tests for a given {@link PageElement} and {@link Rule}
	 * 
	 * @param input_elem
	 * @param rule
	 * @return
	 */
	public static List<List<PathObject>> generateInputRuleTests(PageElement input_elem, Rule rule){
		assert rule != null;
		
		List<List<PathObject>> tests = new ArrayList<List<PathObject>>();
		if(rule.getType().equals(RuleType.ALPHABETIC_RESTRICTION)){
			tests.addAll(generateAlphabeticRestrictionTests(input_elem, rule));
		}
		else if(rule.getType().equals(RuleType.NUMERIC_RESTRICTION)){
			tests.addAll(generateNumericRestrictionTests(input_elem, rule));
		}
		else if(rule.getType().equals(RuleType.SPECIAL_CHARACTER_RESTRICTION)){
			tests.addAll(generateSpecialCharacterRestrictionTests(input_elem, rule));
		}
		else if(rule.getType().equals(RuleType.DISABLED)){
			tests.addAll(generateEnabledTests(input_elem, rule));
		}
		else if(rule.getType().equals(RuleType.READ_ONLY)){
			tests.addAll(generateReadOnlyTests(input_elem, rule));
		}
		else if(rule.getType().equals(RuleType.MAX_LENGTH)){
			tests.addAll(generateLengthBoundaryTests(input_elem, rule));
		}
		else if(rule.getType().equals(RuleType.MIN_LENGTH)){
			tests.addAll(generateLengthBoundaryTests(input_elem, rule));
		}
		else if(rule.getType().equals(RuleType.MAX_VALUE)){
			tests.addAll(generateLengthBoundaryTests(input_elem, rule));
		}
		else if(rule.getType().equals(RuleType.MIN_VALUE)){
			tests.addAll(generateLengthBoundaryTests(input_elem, rule));
		}
		else if(rule.getType().equals(RuleType.PATTERN)){
			tests.addAll(generatePatternTests(input_elem, rule));
		}
		else if(rule.getType().equals(RuleType.EMAIL_PATTERN)){
			tests.addAll(generateEmailTests(input_elem, rule));	
		}
		return tests;
	}
	
	public static List<List<PathObject>> generateBoundaryTests(PageElement input){
		
		return null;
		
	}
	
	public static List<List<PathObject>> generateLengthBoundaryTests(PageElement input, Rule rule, PageElement submit){
		List<List<PathObject>> tests = new ArrayList<List<PathObject>>();

		if(rule.getType().equals(RuleType.MAX_LENGTH)){
			//generate empty string test
			List<PathObject> path_obj_list = new ArrayList<PathObject>();
			path_obj_list.add(input);
			path_obj_list.add(new Action("click", ""));
			path_obj_list.add(input);
			
			//generate string with length equal to MAX_LENGTH
			String short_str = NumericRule.generateRandomAlphabeticString(Integer.parseInt(rule.getValue()));
			
			path_obj_list.add(new Action("sendKeys", short_str));
			path_obj_list.add(submit);
			path_obj_list.add(new Action("click", ""));
			tests.add(path_obj_list);
	
			//generate single character str test
			path_obj_list = new ArrayList<PathObject>();
			path_obj_list.add(input);
			path_obj_list.add(new Action("click", ""));
			path_obj_list.add(input);
			
			//generate string with length that is 1 character greater than MAX_LENGTH
			String large_str = NumericRule.generateRandomAlphabeticString(Integer.parseInt(rule.getValue())+1);
			path_obj_list.add(new Action("sendKeys", large_str));
			path_obj_list.add(submit);
			path_obj_list.add(new Action("click", ""));
			tests.add(path_obj_list);
		}
		else if(rule.getType().equals(RuleType.MIN_LENGTH)){
			//generate empty string test
			List<PathObject> path_obj_list = new ArrayList<PathObject>();
			path_obj_list.add(input);
			path_obj_list.add(new Action("click", ""));
			path_obj_list.add(input);
			
			//generate string with length equal to MAX_LENGTH
			String short_str = NumericRule.generateRandomAlphabeticString(Integer.parseInt(rule.getValue()));

			path_obj_list.add(new Action("sendKeys", short_str));
			path_obj_list.add(submit);
			path_obj_list.add(new Action("click", ""));
			tests.add(path_obj_list);
			
			//generate single character str test
			path_obj_list = new ArrayList<PathObject>();
			path_obj_list.add(input);
			path_obj_list.add(new Action("click", ""));
			path_obj_list.add(input);
			
			//generate string with length that is 1 character greater than MAX_LENGTH
			String large_str = NumericRule.generateRandomAlphabeticString(Integer.parseInt(rule.getValue())-1);

			path_obj_list.add(new Action("sendKeys", large_str));
			path_obj_list.add(submit);
			path_obj_list.add(new Action("click", ""));
			tests.add(path_obj_list);
		}
		else if(rule.getType().equals(RuleType.MAX_VALUE)){
			//generate empty string test
			List<PathObject> path_obj_list = new ArrayList<PathObject>();
			path_obj_list.add(input);
			path_obj_list.add(new Action("click", ""));
			path_obj_list.add(input);
			
			//generate string with length equal to MAX_LENGTH
			path_obj_list.add(new Action("sendKeys", Integer.toString(Integer.parseInt(rule.getValue()))));
			path_obj_list.add(submit);
			path_obj_list.add(new Action("click", ""));
			tests.add(path_obj_list);
			
			//generate single character str test
			path_obj_list = new ArrayList<PathObject>();
			path_obj_list.add(input);
			path_obj_list.add(new Action("click", ""));
			path_obj_list.add(input);
			
			//generate string with length that is 1 character greater than MAX_LENGTH
			path_obj_list.add(new Action("sendKeys", Integer.toString(Integer.parseInt(rule.getValue())+1)));
			path_obj_list.add(submit);
			path_obj_list.add(new Action("click", ""));
			tests.add(path_obj_list);
		}
		else if(rule.getType().equals(RuleType.MIN_VALUE)){
			//generate empty string test
			List<PathObject> path_obj_list = new ArrayList<PathObject>();
			path_obj_list.add(input);
			path_obj_list.add(new Action("click", ""));
			path_obj_list.add(input);
			
			//generate string with length equal to MAX_LENGTH
			path_obj_list.add(new Action("sendKeys", Integer.toString(Integer.parseInt(rule.getValue()))));
			path_obj_list.add(submit);
			path_obj_list.add(new Action("click", ""));
			tests.add(path_obj_list);
			
			//generate single character str test
			path_obj_list = new ArrayList<PathObject>();
			path_obj_list.add(input);
			path_obj_list.add(new Action("click", ""));
			path_obj_list.add(input);
			
			//generate string with length that is 1 character greater than MAX_LENGTH
			path_obj_list.add(new Action("sendKeys", Integer.toString(Integer.parseInt(rule.getValue())-1)));
			path_obj_list.add(submit);
			path_obj_list.add(new Action("click", ""));
			tests.add(path_obj_list);
		}
		return tests;
		
	}
	
	public static List<List<PathObject>> generateValueBoundaryTests(PageElement input){
		return null;	
	}
	
	public static List<List<PathObject>> generateRequirementChecks(PageElement input, boolean isRequired, PageElement submit){
		assert input.getName().equals("input");
		
		List<List<PathObject>> tests = new ArrayList<List<PathObject>>();
		for(Attribute attribute: input.getAttributes()){
			if("type".equals(attribute.getName())){
				String input_type = attribute.getVals().get(0);
				if("text".equals( input_type ) ||
						"textarea".equals(input_type) ||
						"email".equals(input_type)){
					//generate empty string test
					List<PathObject> path_obj_list = new ArrayList<PathObject>();
					path_obj_list.add(input);
					path_obj_list.add(new Action("click", ""));
					path_obj_list.add(input);
					path_obj_list.add(new Action("sendKeys", ""));
					path_obj_list.add(submit);
					path_obj_list.add(new Action("click", ""));
					tests.add(path_obj_list);
					
					//generate single character str test
					List<PathObject> path_obj_list_2 = new ArrayList<PathObject>();
					path_obj_list_2.add(input);
					path_obj_list_2.add(new Action("click", ""));
					path_obj_list_2.add(input);
					path_obj_list_2.add(new Action("sendKeys", "a"));
					path_obj_list_2.add(submit);
					path_obj_list_2.add(new Action("click", ""));
					tests.add(path_obj_list_2);
				}
				else if( "number".equals(input_type)){
					//generate empty string test
					List<PathObject> path_obj_list = new ArrayList<PathObject>();
					path_obj_list.add(input);
					path_obj_list.add(new Action("click", ""));
					path_obj_list.add(input);
					path_obj_list.add(new Action("sendKeys", ""));
					path_obj_list.add(submit);
					path_obj_list.add(new Action("click", ""));
					tests.add(path_obj_list);
					
					//generate single character str test
					List<PathObject> path_obj_list_2 = new ArrayList<PathObject>();
					path_obj_list_2.add(input);
					path_obj_list_2.add(new Action("click", ""));
					path_obj_list_2.add(input);
					path_obj_list_2.add(new Action("sendKeys", "0"));
					path_obj_list_2.add(submit);
					path_obj_list_2.add(new Action("click", ""));
					tests.add(path_obj_list_2);
				}
			}
		}
		
		return tests;
	}
	
	/**
	 * Generates a test with an alphabetic character to verify an alphabetic restriction
	 * @param input_elem
	 * @param rule
	 * @return
	 */
	private static List<List<PathObject>> generateAlphabeticRestrictionTests(PageElement input_elem, Rule rule, PageElement submit) {
		//generate single character str test		
		List<List<PathObject>> tests = new ArrayList<List<PathObject>>();
		List<PathObject> path_obj_list = new ArrayList<PathObject>();
		path_obj_list.add(input_elem);
		path_obj_list.add(new Action("click", ""));
		path_obj_list.add(input_elem);
		path_obj_list.add(new Action("sendKeys", "abcdefghijklmopqrstuvwxyz"));
		path_obj_list.add(submit);
		path_obj_list.add(new Action("click", ""));
		tests.add(path_obj_list);		
		return tests;
	}

	private static List<List<PathObject>> generateNumericRestrictionTests(PageElement input_elem, Rule rule, PageElement submit) {
		List<List<PathObject>> tests = new ArrayList<List<PathObject>>();

		List<PathObject> path_obj_list = new ArrayList<PathObject>();
		path_obj_list.add(input_elem);
		path_obj_list.add(new Action("click", ""));
		path_obj_list.add(input_elem);
		path_obj_list.add(new Action("sendKeys", "0"));
		path_obj_list.add(submit);
		path_obj_list.add(new Action("click", ""));
		tests.add(path_obj_list);
		
		return tests;
	}

	private static List<List<PathObject>> generateSpecialCharacterRestrictionTests(PageElement input_elem, Rule rule, PageElement submit) {
		//generate single character str test
		List<List<PathObject>> tests = new ArrayList<List<PathObject>>();
		List<PathObject> path_obj_list = new ArrayList<PathObject>();
		path_obj_list.add(input_elem);
		path_obj_list.add(new Action("click", ""));
		path_obj_list.add(input_elem);
		path_obj_list.add(new Action("sendKeys", "!"));
		path_obj_list.add(submit);
		path_obj_list.add(new Action("click", ""));
		tests.add(path_obj_list);
		
		return tests;
	}

	private static List<List<PathObject>> generateEnabledTests(PageElement input_elem, Rule rule, PageElement submit) {
		// TODO Auto-generated method stub
		return null;
	}

	private static List<List<PathObject>> generateReadOnlyTests(PageElement input_elem, Rule rule, PageElement submit) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private static List<List<PathObject>> generatePatternTests(PageElement input_elem, Rule rule, PageElement submit) {
		// TODO Auto-generated method stub
		return null;
	}

	private static List<List<PathObject>> generateEmailTests(PageElement input_elem, Rule rule, PageElement submit) {
		List<List<PathObject>> tests = new ArrayList<List<PathObject>>();
		
		List<PathObject> path_obj_list = new ArrayList<PathObject>();
		path_obj_list.add(input_elem);
		path_obj_list.add(new Action("click", ""));
		path_obj_list.add(input_elem);
		path_obj_list.add(new Action("sendKeys", "!test@test.com"));
		path_obj_list.add(submit);
		path_obj_list.add(new Action("click", ""));
		tests.add(path_obj_list);		

		//generate single character str test	
		List<PathObject> path_obj_list1 = new ArrayList<PathObject>();
		path_obj_list1.add(input_elem);
		path_obj_list1.add(new Action("click", ""));
		path_obj_list1.add(input_elem);
		path_obj_list1.add(new Action("sendKeys", "test!test.com"));
		path_obj_list1.add(submit);
		path_obj_list1.add(new Action("click", ""));
		tests.add(path_obj_list1);

		List<PathObject> path_obj_list2 = new ArrayList<PathObject>();
		path_obj_list2.add(input_elem);
		path_obj_list2.add(new Action("click", ""));
		path_obj_list2.add(input_elem);
		path_obj_list2.add(new Action("sendKeys", "test@test"));
		path_obj_list2.add(submit);
		path_obj_list2.add(new Action("click", ""));
		tests.add(path_obj_list2);
		
		return tests;
	}
}
