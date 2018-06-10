package com.minion.actors;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.qanairy.models.ActionPOJO;
import com.qanairy.models.TestPOJO;
import com.qanairy.models.dao.DiscoveryRecordDao;
import com.qanairy.models.dao.DomainDao;
import com.qanairy.models.dao.PageStateDao;
import com.qanairy.models.dao.impl.DiscoveryRecordDaoImpl;
import com.qanairy.models.dao.impl.DomainDaoImpl;
import com.qanairy.models.dao.impl.PageStateDaoImpl;
import com.qanairy.models.rules.NumericRule;
import com.qanairy.models.rules.RuleType;
import com.qanairy.persistence.DiscoveryRecord;
import com.qanairy.persistence.Domain;
import com.qanairy.persistence.PageElement;
import com.qanairy.persistence.PageState;
import com.qanairy.persistence.PathObject;
import com.qanairy.persistence.Rule;
import com.qanairy.persistence.Test;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.minion.api.MessageBroadcaster;
import com.minion.browsing.Browser;
import com.minion.browsing.Crawler;
import com.minion.browsing.element.ComplexField;
import com.minion.browsing.form.ElementRuleExtractor;
import com.minion.browsing.form.Form;
import com.minion.browsing.form.FormField;
import com.minion.structs.Message;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

/**
 * Handles discovery and creation of various form tests
 */
public class FormTestDiscoveryActor extends UntypedActor {
	private static Logger log = LoggerFactory.getLogger(FormTestDiscoveryActor.class);

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof Message){
			Message<?> acct_msg = (Message<?>)message;
			
			if(acct_msg.getData() instanceof Test){
				Test test = ((Test)acct_msg.getData());
	
				//get first page in path
				PageState page = test.firstPage();
	
				int cnt = 0;
			  	Browser browser = null;
			  	
			  	while(browser == null && cnt < 5){
			  		try{
				  		browser = new Browser(acct_msg.getOptions().get("browser").toString());
				  		browser.getDriver().get(page.getUrl().toString());
						break;
					}catch(NullPointerException e){
						log.error(e.getMessage());
					}
					cnt++;
				}	
	
				//PageState current_page = Crawler.crawlPath(test.getPathKeys(), test.getPathObjects(), browser);
			  	
			  	List<Form> forms = Browser.extractAllForms(test.getResult(), browser);
			  	List<List<PathObject>> path_object_lists = new ArrayList<List<PathObject>>();
			  	System.err.println("FORM COUNT ::: "+forms.size());
			  	for(Form form : forms){
			  		path_object_lists.addAll(FormTestDiscoveryActor.generateAllFormTestPaths(test, form));
			  	}
			  	
			  	//Evaluate all tests now
			  	System.err.println("Constructing tests with path object lists   :::   "+path_object_lists.size());
			  	List<Test> tests = new ArrayList<Test>();
			  	for(List<PathObject> path_obj_list : path_object_lists){
			  		List<String> path_keys = new ArrayList<String>(test.getPathKeys());
			  		for(PathObject path_obj : path_obj_list){
			  			path_keys.add(path_obj.getKey());
			  		}
			  		
			  		List<PathObject> test_path_objects = new ArrayList<PathObject>(test.getPathObjects());
			  		test_path_objects.addAll(path_obj_list);
			  		
			  		List<String> test_path_keys = new ArrayList<String>(test.getPathKeys());
			  		test_path_keys.addAll(path_keys);
			  		
					final long pathCrawlStartTime = System.currentTimeMillis();
					
			  		System.err.println("Crawling potential form test path");
			  		browser = new Browser(acct_msg.getOptions().get("browser").toString());
			  		PageState result_page = Crawler.crawlPath(test_path_keys, test_path_objects, browser, acct_msg.getOptions().get("host").toString());
				  	browser.close();
					final long pathCrawlEndTime = System.currentTimeMillis();
					
					long crawl_time_in_ms = pathCrawlEndTime - pathCrawlStartTime;
					
				  	System.err.println("Looking up domain with url :: "+page.getUrl().toString());
				  	System.err.println("Looking up domain with url :: "+page.getUrl().getHost());
					
				  	PageStateDao page_state_dao = new PageStateDaoImpl();

			  		DomainDao domain_dao = new DomainDaoImpl();
			  		Domain domain = domain_dao.find(page.getUrl().getHost());
					domain.addPageState(page_state_dao.save(result_page));
					//domain_dao.save(domain);
					
					for(PageElement element : result_page.getElements()){
						try {
							MessageBroadcaster.broadcastPageElement(element, domain.getUrl() );
						} catch (JsonProcessingException e) {
						}
					}
					
			  		Test new_test = new TestPOJO(test_path_keys, test_path_objects, result_page, "Test #" + domain.getTestCount(), false, test.getSpansMultipleDomains());
			  		new_test.setRunTime(crawl_time_in_ms);
			  		new_test.setLastRunTimestamp(test.getLastRunTimestamp());
			  		
			  		tests.add(new_test);
			  	}
			  	
				DiscoveryRecordDao discovery_dao = new DiscoveryRecordDaoImpl();
				DiscoveryRecord discovery_record = discovery_dao.find(acct_msg.getOptions().get("discovery_key").toString());
				discovery_record.setTestCount(discovery_record.getTestCount()+tests.size());
				
				MessageBroadcaster.broadcastDiscoveryStatus(discovery_record);
				System.err.println("Broadcasting discovery record now that we've added "+tests.size()+"        tests   ");
				final ActorRef memory_actor = this.getContext().actorOf(Props.create(MemoryRegistryActor.class), "MemoryRegistration"+UUID.randomUUID());
								
			  	for(Test form_test : tests){
			  		//send all tests to work allocator to be evaluated
			  		Message<Test> test_msg = new Message<Test>(acct_msg.getAccountKey(), form_test, acct_msg.getOptions());

					//tell memory worker of test
					memory_actor.tell(test_msg, getSelf());
				}
			}
		}
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
			path_obj_list.add(new ActionPOJO("click", ""));
			path_obj_list.add(input);
			
			//generate string with length equal to MAX_LENGTH
			String short_str = NumericRule.generateRandomAlphabeticString(Integer.parseInt(rule.getValue()));
			
			path_obj_list.add(new ActionPOJO("sendKeys", short_str));
			path_obj_list.add(submit);
			path_obj_list.add(new ActionPOJO("click", ""));
			tests.add(path_obj_list);
	
			//generate single character str test
			path_obj_list = new ArrayList<PathObject>();
			path_obj_list.add(input);
			path_obj_list.add(new ActionPOJO("click", ""));
			path_obj_list.add(input);
			
			//generate string with length that is 1 character greater than MAX_LENGTH
			String large_str = NumericRule.generateRandomAlphabeticString(Integer.parseInt(rule.getValue())+1);
			path_obj_list.add(new ActionPOJO("sendKeys", large_str));
			path_obj_list.add(submit);
			path_obj_list.add(new ActionPOJO("click", ""));
			tests.add(path_obj_list);
		}
		else if(rule.getType().equals(RuleType.MIN_LENGTH)){
			//generate empty string test
			List<PathObject> path_obj_list = new ArrayList<PathObject>();
			path_obj_list.add(input);
			path_obj_list.add(new ActionPOJO("click", ""));
			path_obj_list.add(input);
			
			//generate string with length equal to MAX_LENGTH
			String short_str = NumericRule.generateRandomAlphabeticString(Integer.parseInt(rule.getValue()));

			path_obj_list.add(new ActionPOJO("sendKeys", short_str));
			path_obj_list.add(submit);
			path_obj_list.add(new ActionPOJO("click", ""));
			tests.add(path_obj_list);
			
			//generate single character str test
			path_obj_list = new ArrayList<PathObject>();
			path_obj_list.add(input);
			path_obj_list.add(new ActionPOJO("click", ""));
			path_obj_list.add(input);
			
			//generate string with length that is 1 character greater than MAX_LENGTH
			String large_str = NumericRule.generateRandomAlphabeticString(Integer.parseInt(rule.getValue())-1);

			path_obj_list.add(new ActionPOJO("sendKeys", large_str));
			path_obj_list.add(submit);
			path_obj_list.add(new ActionPOJO("click", ""));
			tests.add(path_obj_list);
		}
		else if(rule.getType().equals(RuleType.MAX_VALUE)){
			//generate empty string test
			List<PathObject> path_obj_list = new ArrayList<PathObject>();
			path_obj_list.add(input);
			path_obj_list.add(new ActionPOJO("click", ""));
			path_obj_list.add(input);
			
			//generate string with length equal to MAX_LENGTH
			path_obj_list.add(new ActionPOJO("sendKeys", Integer.toString(Integer.parseInt(rule.getValue()))));
			path_obj_list.add(submit);
			path_obj_list.add(new ActionPOJO("click", ""));
			tests.add(path_obj_list);
			
			//generate single character str test
			path_obj_list = new ArrayList<PathObject>();
			path_obj_list.add(input);
			path_obj_list.add(new ActionPOJO("click", ""));
			path_obj_list.add(input);
			
			//generate string with length that is 1 character greater than MAX_LENGTH
			path_obj_list.add(new ActionPOJO("sendKeys", Integer.toString(Integer.parseInt(rule.getValue())+1)));
			path_obj_list.add(submit);
			path_obj_list.add(new ActionPOJO("click", ""));
			tests.add(path_obj_list);
		}
		else if(rule.getType().equals(RuleType.MIN_VALUE)){
			//generate empty string test
			List<PathObject> path_obj_list = new ArrayList<PathObject>();
			path_obj_list.add(input);
			path_obj_list.add(new ActionPOJO("click", ""));
			path_obj_list.add(input);
			
			//generate string with length equal to MAX_LENGTH
			path_obj_list.add(new ActionPOJO("sendKeys", Integer.toString(Integer.parseInt(rule.getValue()))));
			path_obj_list.add(submit);
			path_obj_list.add(new ActionPOJO("click", ""));
			tests.add(path_obj_list);
			
			//generate single character str test
			path_obj_list = new ArrayList<PathObject>();
			path_obj_list.add(input);
			path_obj_list.add(new ActionPOJO("click", ""));
			path_obj_list.add(input);
			
			//generate string with length that is 1 character greater than MAX_LENGTH
			path_obj_list.add(new ActionPOJO("sendKeys", Integer.toString(Integer.parseInt(rule.getValue())-1)));
			path_obj_list.add(submit);
			path_obj_list.add(new ActionPOJO("click", ""));
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
		String input_type = input.getAttributes().get(input.getAttributes().indexOf("type")).getVals().get(0);
		if(input_type.equals("text") ||
				input_type.equals("textarea") ||
				input_type.equals("email")){
			//generate empty string test
			List<PathObject> path_obj_list = new ArrayList<PathObject>();
			path_obj_list.add(input);
			path_obj_list.add(new ActionPOJO("click", ""));
			path_obj_list.add(input);
			path_obj_list.add(new ActionPOJO("sendKeys", ""));
			path_obj_list.add(submit);
			path_obj_list.add(new ActionPOJO("click", ""));
			tests.add(path_obj_list);
			
			//generate single character str test
			List<PathObject> path_obj_list_2 = new ArrayList<PathObject>();
			path_obj_list_2.add(input);
			path_obj_list_2.add(new ActionPOJO("click", ""));
			path_obj_list_2.add(input);
			path_obj_list_2.add(new ActionPOJO("sendKeys", "a"));
			path_obj_list_2.add(submit);
			path_obj_list_2.add(new ActionPOJO("click", ""));
			tests.add(path_obj_list_2);
		}
		else if( input_type.equals("number")){
			//generate empty string test
			List<PathObject> path_obj_list = new ArrayList<PathObject>();
			path_obj_list.add(input);
			path_obj_list.add(new ActionPOJO("click", ""));
			path_obj_list.add(input);
			path_obj_list.add(new ActionPOJO("sendKeys", ""));
			path_obj_list.add(submit);
			path_obj_list.add(new ActionPOJO("click", ""));
			tests.add(path_obj_list);
			
			//generate single character str test
			List<PathObject> path_obj_list_2 = new ArrayList<PathObject>();
			path_obj_list_2.add(input);
			path_obj_list_2.add(new ActionPOJO("click", ""));
			path_obj_list_2.add(input);
			path_obj_list_2.add(new ActionPOJO("sendKeys", "0"));
			path_obj_list_2.add(submit);
			path_obj_list_2.add(new ActionPOJO("click", ""));
			tests.add(path_obj_list_2);
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
		path_obj_list.add(new ActionPOJO("click", ""));
		path_obj_list.add(input_elem);
		path_obj_list.add(new ActionPOJO("sendKeys", "abcdefghijklmopqrstuvwxyz"));
		path_obj_list.add(submit);
		path_obj_list.add(new ActionPOJO("click", ""));
		tests.add(path_obj_list);		
		return tests;
	}

	private static List<List<PathObject>> generateNumericRestrictionTests(PageElement input_elem, Rule rule, PageElement submit) {
		List<List<PathObject>> tests = new ArrayList<List<PathObject>>();

		List<PathObject> path_obj_list = new ArrayList<PathObject>();
		path_obj_list.add(input_elem);
		path_obj_list.add(new ActionPOJO("click", ""));
		path_obj_list.add(input_elem);
		path_obj_list.add(new ActionPOJO("sendKeys", "0"));
		path_obj_list.add(submit);
		path_obj_list.add(new ActionPOJO("click", ""));
		tests.add(path_obj_list);
		
		return tests;
	}

	private static List<List<PathObject>> generateSpecialCharacterRestrictionTests(PageElement input_elem, Rule rule, PageElement submit) {
		//generate single character str test
		List<List<PathObject>> tests = new ArrayList<List<PathObject>>();
		List<PathObject> path_obj_list = new ArrayList<PathObject>();
		path_obj_list.add(input_elem);
		path_obj_list.add(new ActionPOJO("click", ""));
		path_obj_list.add(input_elem);
		path_obj_list.add(new ActionPOJO("sendKeys", "!"));
		path_obj_list.add(submit);
		path_obj_list.add(new ActionPOJO("click", ""));
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
		path_obj_list.add(new ActionPOJO("click", ""));
		path_obj_list.add(input_elem);
		path_obj_list.add(new ActionPOJO("sendKeys", "!test@test.com"));
		path_obj_list.add(submit);
		path_obj_list.add(new ActionPOJO("click", ""));
		tests.add(path_obj_list);		

		//generate single character str test	
		List<PathObject> path_obj_list1 = new ArrayList<PathObject>();
		path_obj_list1.add(input_elem);
		path_obj_list1.add(new ActionPOJO("click", ""));
		path_obj_list1.add(input_elem);
		path_obj_list1.add(new ActionPOJO("sendKeys", "test!test.com"));
		path_obj_list1.add(submit);
		path_obj_list1.add(new ActionPOJO("click", ""));
		tests.add(path_obj_list1);

		List<PathObject> path_obj_list2 = new ArrayList<PathObject>();
		path_obj_list2.add(input_elem);
		path_obj_list2.add(new ActionPOJO("click", ""));
		path_obj_list2.add(input_elem);
		path_obj_list2.add(new ActionPOJO("sendKeys", "test@test"));
		path_obj_list2.add(submit);
		path_obj_list2.add(new ActionPOJO("click", ""));
		tests.add(path_obj_list2);
		
		return tests;
	}
	/**
	 * 
	 * @param test
	 * @param form
	 * @return
	 */
	public static List<List<PathObject>> generateAllFormTestPaths(Test test, Form form){
		List<List<PathObject>> form_tests = new ArrayList<List<PathObject>>();
		System.err.println("FORM FIELDS COUNT     :::    "+form.getFormFields());
		for(ComplexField complex_field: form.getFormFields()){
			//for each field in the complex field generate a set of tests for all known rules
			System.err.println("COMPLEX FIELD ELEMENTS   :::   "+complex_field.getElements());
			for(FormField field : complex_field.getElements()){
				PageElement input_elem = field.getInputElement();
				
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
					continue;
				}
				
				List<Rule> rules = ElementRuleExtractor.extractInputRules(input_elem);
				System.err.println("Total RULES   :::   "+rules.size());
				for(Rule rule : rules){
					List<List<PathObject>> path_list = generateFormRuleTests(input_elem, rule, form.getSubmitField());
					form_tests.addAll(path_list);
				}
				System.err.println("FORM TESTS    :::   "+form_tests.size());
			}
		}
		return form_tests;
	}
	

	public static List<List<PathObject>> generateLengthBoundaryTests(PageElement input, Rule rule){
		List<List<PathObject>> tests = new ArrayList<List<PathObject>>();

		if(rule.getType().equals(RuleType.MAX_LENGTH)){
			//generate empty string test
			List<PathObject> path_obj_list = new ArrayList<PathObject>();
			path_obj_list.add(input);
			path_obj_list.add(new ActionPOJO("click", ""));
			path_obj_list.add(input);

			//generate string with length equal to MAX_LENGTH
			String short_str = NumericRule.generateRandomAlphabeticString(Integer.parseInt(rule.getValue()));
			
			path_obj_list.add(new ActionPOJO("sendKeys", short_str));
			tests.add(path_obj_list);

			//generate single character str test
			path_obj_list = new ArrayList<PathObject>();
			path_obj_list.add(input);
			path_obj_list.add(new ActionPOJO("click", ""));
			path_obj_list.add(input);

			//generate string with length that is 1 character greater than MAX_LENGTH
			String large_str = NumericRule.generateRandomAlphabeticString(Integer.parseInt(rule.getValue())+1);
			path_obj_list.add(new ActionPOJO("sendKeys", large_str));
			tests.add(path_obj_list);
		}
		else if(rule.getType().equals(RuleType.MIN_LENGTH)){
			//generate empty string test
			List<PathObject> path_obj_list = new ArrayList<PathObject>();
			path_obj_list.add(input);
			path_obj_list.add(new ActionPOJO("click", ""));
			path_obj_list.add(input);
			
			//generate string with length equal to MAX_LENGTH
			String short_str = NumericRule.generateRandomAlphabeticString(Integer.parseInt(rule.getValue()));

			path_obj_list.add(new ActionPOJO("sendKeys", short_str));
			tests.add(path_obj_list);
			
			//generate single character str test
			path_obj_list = new ArrayList<PathObject>();
			path_obj_list.add(input);
			path_obj_list.add(new ActionPOJO("click", ""));
			path_obj_list.add(input);
			
			//generate string with length that is 1 character greater than MAX_LENGTH
			String large_str = NumericRule.generateRandomAlphabeticString(Integer.parseInt(rule.getValue())-1);

			path_obj_list.add(new ActionPOJO("sendKeys", large_str));
			tests.add(path_obj_list);
		}
		else if(rule.getType().equals(RuleType.MAX_VALUE)){
			//generate empty string test
			List<PathObject> path_obj_list = new ArrayList<PathObject>();
			path_obj_list.add(input);
			path_obj_list.add(new ActionPOJO("click", ""));
			path_obj_list.add(input);
			
			//generate string with length equal to MAX_LENGTH
			path_obj_list.add(new ActionPOJO("sendKeys", Integer.toString(Integer.parseInt(rule.getValue()))));
			tests.add(path_obj_list);
			
			//generate single character str test
			path_obj_list = new ArrayList<PathObject>();
			path_obj_list.add(input);
			path_obj_list.add(new ActionPOJO("click", ""));
			path_obj_list.add(input);
			
			//generate string with length that is 1 character greater than MAX_LENGTH
			path_obj_list.add(new ActionPOJO("sendKeys", Integer.toString(Integer.parseInt(rule.getValue())+1)));
			tests.add(path_obj_list);
		}
		else if(rule.getType().equals(RuleType.MIN_VALUE)){
			//generate empty string test
			List<PathObject> path_obj_list = new ArrayList<PathObject>();
			path_obj_list.add(input);
			path_obj_list.add(new ActionPOJO("click", ""));
			path_obj_list.add(input);
			
			//generate string with length equal to MAX_LENGTH
			path_obj_list.add(new ActionPOJO("sendKeys", Integer.toString(Integer.parseInt(rule.getValue()))));
			tests.add(path_obj_list);
			
			//generate single character str test
			path_obj_list = new ArrayList<PathObject>();
			path_obj_list.add(input);
			path_obj_list.add(new ActionPOJO("click", ""));
			path_obj_list.add(input);
			
			//generate string with length that is 1 character greater than MAX_LENGTH
			path_obj_list.add(new ActionPOJO("sendKeys", Integer.toString(Integer.parseInt(rule.getValue())-1)));
			tests.add(path_obj_list);
		}
		return tests;
		
	}
	
	@Deprecated
	public static List<List<PathObject>> generateRequirementChecks(PageElement input, boolean isRequired){
		assert input.getName().equals("input");
		
		List<List<PathObject>> tests = new ArrayList<List<PathObject>>();
		String input_type = input.getAttributes().get(input.getAttributes().indexOf("type")).getVals().get(0);
		if(input_type.equals("text") ||
				input_type.equals("textarea") ||
				input_type.equals("email")){
			//generate empty string test
			List<PathObject> path_obj_list = new ArrayList<PathObject>();
			path_obj_list.add(input);
			path_obj_list.add(new ActionPOJO("click", ""));
			path_obj_list.add(input);
			path_obj_list.add(new ActionPOJO("sendKeys", ""));
			tests.add(path_obj_list);
			
			//generate single character str test
			List<PathObject> path_obj_list_2 = new ArrayList<PathObject>();
			path_obj_list_2.add(input);
			path_obj_list_2.add(new ActionPOJO("click", ""));
			path_obj_list_2.add(input);
			path_obj_list_2.add(new ActionPOJO("sendKeys", "a"));
			tests.add(path_obj_list_2);
		}
		else if( input_type.equals("number")){

			//generate empty string test
			List<PathObject> path_obj_list = new ArrayList<PathObject>();
			path_obj_list.add(input);
			path_obj_list.add(new ActionPOJO("click", ""));
			path_obj_list.add(input);
			path_obj_list.add(new ActionPOJO("sendKeys", ""));
			tests.add(path_obj_list);
			
			//generate single character str test
			List<PathObject> path_obj_list_2 = new ArrayList<PathObject>();
			path_obj_list_2.add(input);
			path_obj_list_2.add(new ActionPOJO("click", ""));
			path_obj_list_2.add(input);
			path_obj_list_2.add(new ActionPOJO("sendKeys", "0"));
			tests.add(path_obj_list_2);
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
		path_obj_list.add(new ActionPOJO("click", ""));
		path_obj_list.add(input_elem);
		path_obj_list.add(new ActionPOJO("sendKeys", "a"));
		tests.add(path_obj_list);		
		return tests;
	}

	private static List<List<PathObject>> generateNumericRestrictionTests(PageElement input_elem, Rule rule) {
		List<List<PathObject>> tests = new ArrayList<List<PathObject>>();

		List<PathObject> path_obj_list = new ArrayList<PathObject>();
		path_obj_list.add(input_elem);
		path_obj_list.add(new ActionPOJO("click", ""));
		path_obj_list.add(input_elem);
		path_obj_list.add(new ActionPOJO("sendKeys", "0"));
		tests.add(path_obj_list);
		
		return tests;
	}

	private static List<List<PathObject>> generateSpecialCharacterRestrictionTests(PageElement input_elem, Rule rule) {
		//generate single character str test
		List<List<PathObject>> tests = new ArrayList<List<PathObject>>();
		List<PathObject> path_obj_list = new ArrayList<PathObject>();
		path_obj_list.add(input_elem);
		path_obj_list.add(new ActionPOJO("click", ""));
		path_obj_list.add(input_elem);
		path_obj_list.add(new ActionPOJO("sendKeys", "!"));
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
		path_obj_list.add(new ActionPOJO("click", ""));
		path_obj_list.add(input_elem);
		path_obj_list.add(new ActionPOJO("sendKeys", "!test@test.com"));
		tests.add(path_obj_list);		

		//generate single character str test	
		List<PathObject> path_obj_list1 = new ArrayList<PathObject>();
		path_obj_list1.add(input_elem);
		path_obj_list1.add(new ActionPOJO("click", ""));
		path_obj_list1.add(input_elem);
		path_obj_list1.add(new ActionPOJO("sendKeys", "test!test.com"));
		tests.add(path_obj_list1);

		List<PathObject> path_obj_list2 = new ArrayList<PathObject>();
		path_obj_list2.add(input_elem);
		path_obj_list2.add(new ActionPOJO("click", ""));
		path_obj_list2.add(input_elem);
		path_obj_list2.add(new ActionPOJO("sendKeys", "test@test"));
		tests.add(path_obj_list2);
		
		List<PathObject> path_obj_list3 = new ArrayList<PathObject>();
		path_obj_list3.add(input_elem);
		path_obj_list3.add(new ActionPOJO("click", ""));
		path_obj_list3.add(input_elem);
		path_obj_list3.add(new ActionPOJO("sendKeys", "test.test@test"));
		tests.add(path_obj_list3);
		
		List<PathObject> path_obj_list4 = new ArrayList<PathObject>();
		path_obj_list4.add(input_elem);
		path_obj_list4.add(new ActionPOJO("click", ""));
		path_obj_list4.add(input_elem);
		path_obj_list4.add(new ActionPOJO("sendKeys", "test_test@test"));
		tests.add(path_obj_list4);
		
		return tests;
	}
}
