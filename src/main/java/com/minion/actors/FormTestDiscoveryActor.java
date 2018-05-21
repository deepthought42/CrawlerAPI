package com.minion.actors;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;

import com.qanairy.models.ActionPOJO;
import com.qanairy.models.TestPOJO;
import com.qanairy.models.dao.DiscoveryRecordDao;
import com.qanairy.models.dao.impl.DiscoveryRecordDaoImpl;
import com.qanairy.models.rules.NumericRule;
import com.qanairy.models.rules.RuleType;
import com.qanairy.persistence.DiscoveryRecord;
import com.qanairy.persistence.OrientConnectionFactory;
import com.qanairy.persistence.PageElement;
import com.qanairy.persistence.PageState;
import com.qanairy.persistence.PathObject;
import com.qanairy.persistence.Rule;
import com.qanairy.persistence.Test;
import com.minion.api.MessageBroadcaster;
import com.minion.browsing.Browser;
import com.minion.browsing.Crawler;
import com.minion.browsing.element.ComplexField;
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
			Test test = null;
			if(acct_msg.getData() instanceof Test){
				test = (Test)acct_msg.getData();
			}
			else if(acct_msg.getData() instanceof Test){
				test = ((Test)acct_msg.getData());
			}
			
			//get first page in path
			PageState page = test.firstPage();

			int cnt = 0;
		  	Browser browser = null;
		  	
		  	while(browser == null && cnt < 5){
		  		try{
			  		browser = new Browser(acct_msg.getOptions().get("browser").toString());
					break;
				}catch(NullPointerException e){
					log.error(e.getMessage());
				}
				cnt++;
			}	

			PageState current_page = Crawler.crawlPath(test.getPathKeys(), test.getPathObjects(), browser);

		  	List<Form> forms = Browser.extractAllForms(current_page, browser);
		  	List<Test> form_tests = new ArrayList<Test>();
		  	for(Form form : forms){
		  		form_tests.addAll(FormTestDiscoveryActor.generateAllFormTests(test, form));
		  	}
		  	
		  	OrientConnectionFactory conn = new OrientConnectionFactory();
			DiscoveryRecordDao discovery_repo = new DiscoveryRecordDaoImpl();
			DiscoveryRecord discovery_record = discovery_repo.find(acct_msg.getOptions().get("discovery_key").toString());
			discovery_record.setTestCount(discovery_record.getTestCount()+form_tests.size());
			discovery_repo.save(discovery_record);
			MessageBroadcaster.broadcastDiscoveryStatus(page.getUrl().getHost(), discovery_record);
			System.err.println("Broadcasting discovery record now that we've added "+form_tests.size()+"        tests   ");
			conn.close();
			
		  	final ActorRef work_allocator = this.getContext().actorOf(Props.create(WorkAllocationActor.class), "workAllocator"+UUID.randomUUID());
			for(Test expanded : form_tests){
				//send all tests to work allocator to be evaluated
				Message<Test> expanded_path_msg = new Message<Test>(acct_msg.getAccountKey(), expanded, acct_msg.getOptions());
				work_allocator.tell(expanded_path_msg, getSelf() );
			}
			
		  	browser.close();
		}
	}
	
	/**
	 * Generates rule tests for a given {@link PageElement} and {@link Rule}
	 * 
	 * @param input_elem
	 * @param rule
	 * @return
	 */
	public static List<Test> generateFormRuleTests(PageElement input_elem, Rule rule, PageElement submitField){
		assert rule != null;
		
		List<Test> tests = new ArrayList<Test>();
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
	public static List<Test> generateInputRuleTests(PageElement input_elem, Rule rule){
		assert rule != null;
		
		List<Test> tests = new ArrayList<Test>();
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
	
	public static List<Test> generateBoundaryTests(PageElement input){
		
		return null;
		
	}
	
	public static List<Test> generateLengthBoundaryTests(PageElement input, Rule rule, PageElement submit){
		List<Test> tests = new ArrayList<Test>();

		if(rule.getType().equals(RuleType.MAX_LENGTH)){
			//generate empty string test
			Test path = new TestPOJO();
			path.addPathObject(input);
			path.addPathObject(new ActionPOJO("click", ""));
			path.addPathObject(input);
			
			//generate string with length equal to MAX_LENGTH
			String short_str = NumericRule.generateRandomAlphabeticString(Integer.parseInt(rule.getValue()));
			
			path.addPathObject(new ActionPOJO("sendKeys", short_str));
			path.addPathObject(submit);
			path.addPathObject(new ActionPOJO("click", ""));
			tests.add(path);
	
			//generate single character str test
			path = new TestPOJO();
			path.addPathObject(input);
			path.addPathObject(new ActionPOJO("click", ""));
			path.addPathObject(input);
			
			//generate string with length that is 1 character greater than MAX_LENGTH
			String large_str = NumericRule.generateRandomAlphabeticString(Integer.parseInt(rule.getValue())+1);
			path.addPathObject(new ActionPOJO("sendKeys", large_str));
			path.addPathObject(submit);
			path.addPathObject(new ActionPOJO("click", ""));
			tests.add(path);
		}
		else if(rule.getType().equals(RuleType.MIN_LENGTH)){
			//generate empty string test
			Test path = new TestPOJO();
			path.addPathObject(input);
			path.addPathObject(new ActionPOJO("click", ""));
			path.addPathObject(input);
			
			//generate string with length equal to MAX_LENGTH
			String short_str = NumericRule.generateRandomAlphabeticString(Integer.parseInt(rule.getValue()));

			path.addPathObject(new ActionPOJO("sendKeys", short_str));
			path.addPathObject(submit);
			path.addPathObject(new ActionPOJO("click", ""));
			tests.add(path);
			
			//generate single character str test
			path = new TestPOJO();
			path.addPathObject(input);
			path.addPathObject(new ActionPOJO("click", ""));
			path.addPathObject(input);
			
			//generate string with length that is 1 character greater than MAX_LENGTH
			String large_str = NumericRule.generateRandomAlphabeticString(Integer.parseInt(rule.getValue())-1);

			path.addPathObject(new ActionPOJO("sendKeys", large_str));
			path.addPathObject(submit);
			path.addPathObject(new ActionPOJO("click", ""));
			tests.add(path);
		}
		else if(rule.getType().equals(RuleType.MAX_VALUE)){
			//generate empty string test
			Test path = new TestPOJO();
			path.addPathObject(input);
			path.addPathObject(new ActionPOJO("click", ""));
			path.addPathObject(input);
			
			//generate string with length equal to MAX_LENGTH
			path.addPathObject(new ActionPOJO("sendKeys", Integer.toString(Integer.parseInt(rule.getValue()))));
			path.addPathObject(submit);
			path.addPathObject(new ActionPOJO("click", ""));
			tests.add(path);
			
			//generate single character str test
			path = new TestPOJO();
			path.addPathObject(input);
			path.addPathObject(new ActionPOJO("click", ""));
			path.addPathObject(input);
			
			//generate string with length that is 1 character greater than MAX_LENGTH
			path.addPathObject(new ActionPOJO("sendKeys", Integer.toString(Integer.parseInt(rule.getValue())+1)));
			path.addPathObject(submit);
			path.addPathObject(new ActionPOJO("click", ""));
			tests.add(path);
		}
		else if(rule.getType().equals(RuleType.MIN_VALUE)){
			//generate empty string test
			Test path = new TestPOJO();
			path.addPathObject(input);
			path.addPathObject(new ActionPOJO("click", ""));
			path.addPathObject(input);
			
			//generate string with length equal to MAX_LENGTH
			path.addPathObject(new ActionPOJO("sendKeys", Integer.toString(Integer.parseInt(rule.getValue()))));
			path.addPathObject(submit);
			path.addPathObject(new ActionPOJO("click", ""));
			tests.add(path);
			
			//generate single character str test
			path = new TestPOJO();
			path.addPathObject(input);
			path.addPathObject(new ActionPOJO("click", ""));
			path.addPathObject(input);
			
			//generate string with length that is 1 character greater than MAX_LENGTH
			path.addPathObject(new ActionPOJO("sendKeys", Integer.toString(Integer.parseInt(rule.getValue())-1)));
			path.addPathObject(submit);
			path.addPathObject(new ActionPOJO("click", ""));
			tests.add(path);
		}
		return tests;
		
	}
	
	public static List<Test> generateValueBoundaryTests(PageElement input){
		return null;	
	}
	
	public static List<Test> generateRequirementChecks(PageElement input, boolean isRequired, PageElement submit){
		assert input.getName().equals("input");
		
		List<Test> tests = new ArrayList<Test>();
		String input_type = input.getAttribute("type").getVals().get(0);
		if(input_type.equals("text") ||
				input_type.equals("textarea") ||
				input_type.equals("email")){
			//generate empty string test
			Test path = new TestPOJO();
			path.addPathObject(input);
			path.addPathObject(new ActionPOJO("click", ""));
			path.addPathObject(input);
			path.addPathObject(new ActionPOJO("sendKeys", ""));
			path.addPathObject(submit);
			path.addPathObject(new ActionPOJO("click", ""));
			tests.add(path);
			
			//generate single character str test
			Test path_2 = new TestPOJO();
			path_2.addPathObject(input);
			path_2.addPathObject(new ActionPOJO("click", ""));
			path_2.addPathObject(input);
			path_2.addPathObject(new ActionPOJO("sendKeys", "a"));
			path_2.addPathObject(submit);
			path_2.addPathObject(new ActionPOJO("click", ""));
			tests.add(path_2);
		}
		else if( input_type.equals("number")){
			//generate empty string test
			Test path = new TestPOJO();
			path.addPathObject(input);
			path.addPathObject(new ActionPOJO("click", ""));
			path.addPathObject(input);
			path.addPathObject(new ActionPOJO("sendKeys", ""));
			path.addPathObject(submit);
			path.addPathObject(new ActionPOJO("click", ""));
			tests.add(path);
			
			//generate single character str test
			Test path_2 = new TestPOJO();
			path_2.addPathObject(input);
			path_2.addPathObject(new ActionPOJO("click", ""));
			path_2.addPathObject(input);
			path_2.addPathObject(new ActionPOJO("sendKeys", "0"));
			path_2.addPathObject(submit);
			path_2.addPathObject(new ActionPOJO("click", ""));
			tests.add(path_2);
		}
		return tests;
	}
	
	/**
	 * Generates a test with an alphabetic character to verify an alphabetic restriction
	 * @param input_elem
	 * @param rule
	 * @return
	 */
	private static List<Test> generateAlphabeticRestrictionTests(PageElement input_elem, Rule rule, PageElement submit) {
		//generate single character str test		
		List<Test> tests = new ArrayList<Test>();
		Test path = new TestPOJO();
		path.addPathObject(input_elem);
		path.addPathObject(new ActionPOJO("click", ""));
		path.addPathObject(input_elem);
		path.addPathObject(new ActionPOJO("sendKeys", "abcdefghijklmopqrstuvwxyz"));
		path.addPathObject(submit);
		path.addPathObject(new ActionPOJO("click", ""));
		tests.add(path);		
		return tests;
	}

	private static List<Test> generateNumericRestrictionTests(PageElement input_elem, Rule rule, PageElement submit) {
		List<Test> tests = new ArrayList<Test>();

		Test path = new TestPOJO();
		path.addPathObject(input_elem);
		path.addPathObject(new ActionPOJO("click", ""));
		path.addPathObject(input_elem);
		path.addPathObject(new ActionPOJO("sendKeys", "0"));
		path.addPathObject(submit);
		path.addPathObject(new ActionPOJO("click", ""));
		tests.add(path);
		
		return tests;
	}

	private static List<Test> generateSpecialCharacterRestrictionTests(PageElement input_elem, Rule rule, PageElement submit) {
		//generate single character str test
		List<Test> tests = new ArrayList<Test>();
		Test path = new TestPOJO();
		path.addPathObject(input_elem);
		path.addPathObject(new ActionPOJO("click", ""));
		path.addPathObject(input_elem);
		path.addPathObject(new ActionPOJO("sendKeys", "!"));
		path.addPathObject(submit);
		path.addPathObject(new ActionPOJO("click", ""));
		tests.add(path);
		
		return tests;
	}

	private static List<Test> generateEnabledTests(PageElement input_elem, Rule rule, PageElement submit) {
		// TODO Auto-generated method stub
		return null;
	}

	private static List<Test> generateReadOnlyTests(PageElement input_elem, Rule rule, PageElement submit) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private static List<Test> generatePatternTests(PageElement input_elem, Rule rule, PageElement submit) {
		// TODO Auto-generated method stub
		return null;
	}

	private static List<Test> generateEmailTests(PageElement input_elem, Rule rule, PageElement submit) {
		List<Test> tests = new ArrayList<Test>();
		
		Test path = new TestPOJO();
		path.addPathObject(input_elem);
		path.addPathObject(new ActionPOJO("click", ""));
		path.addPathObject(input_elem);
		path.addPathObject(new ActionPOJO("sendKeys", "!test@test.com"));
		path.addPathObject(submit);
		path.addPathObject(new ActionPOJO("click", ""));
		tests.add(path);		

		//generate single character str test	
		Test path1 = new TestPOJO();
		path1.addPathObject(input_elem);
		path1.addPathObject(new ActionPOJO("click", ""));
		path1.addPathObject(input_elem);
		path1.addPathObject(new ActionPOJO("sendKeys", "test!test.com"));
		path1.addPathObject(submit);
		path1.addPathObject(new ActionPOJO("click", ""));
		tests.add(path1);

		Test path2 = new TestPOJO();
		path2.addPathObject(input_elem);
		path2.addPathObject(new ActionPOJO("click", ""));
		path2.addPathObject(input_elem);
		path2.addPathObject(new ActionPOJO("sendKeys", "test@test"));
		path2.addPathObject(submit);
		path2.addPathObject(new ActionPOJO("click", ""));
		tests.add(path2);
		
		return tests;
	}
	/**
	 * 
	 * @param path
	 * @param form
	 * @return
	 */
	public static List<Test> generateAllFormTests(Test test, Form form){
		List<Test> form_tests = new ArrayList<Test>();
		for(ComplexField complex_field: form.getFormFields()){
			//for each field in the complex field generate a set of tests for all known rules
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
				
				List<Rule> rules = field.getInputElement().getRules();
				for(Rule rule : rules){
					List<Test> path_list = generateFormRuleTests(input_elem, rule, form.getSubmitField());
					for(Test curr_test : path_list){
						Test clone_test = TestPOJO.clone(test);
						form_tests.add(clone_test);
					}
				}
			}
		}
		return form_tests;
	}
	

	public static List<Test> generateLengthBoundaryTests(PageElement input, Rule rule){
		List<Test> tests = new ArrayList<Test>();

		if(rule.getType().equals(RuleType.MAX_LENGTH)){
			//generate empty string test
			Test path = new TestPOJO();
			path.addPathObject(input);
			path.addPathObject(new ActionPOJO("click", ""));
			path.addPathObject(input);

			//generate string with length equal to MAX_LENGTH
			String short_str = NumericRule.generateRandomAlphabeticString(Integer.parseInt(rule.getValue()));
			
			path.addPathObject(new ActionPOJO("sendKeys", short_str));
			tests.add(path);

			//generate single character str test
			path = new TestPOJO();
			path.addPathObject(input);
			path.addPathObject(new ActionPOJO("click", ""));
			path.addPathObject(input);

			//generate string with length that is 1 character greater than MAX_LENGTH
			String large_str = NumericRule.generateRandomAlphabeticString(Integer.parseInt(rule.getValue())+1);
			path.addPathObject(new ActionPOJO("sendKeys", large_str));
			tests.add(path);
		}
		else if(rule.getType().equals(RuleType.MIN_LENGTH)){
			//generate empty string test
			Test path = new TestPOJO();
			path.addPathObject(input);
			path.addPathObject(new ActionPOJO("click", ""));
			path.addPathObject(input);
			
			//generate string with length equal to MAX_LENGTH
			String short_str = NumericRule.generateRandomAlphabeticString(Integer.parseInt(rule.getValue()));

			path.addPathObject(new ActionPOJO("sendKeys", short_str));
			tests.add(path);
			
			//generate single character str test
			path = new TestPOJO();
			path.addPathObject(input);
			path.addPathObject(new ActionPOJO("click", ""));
			path.addPathObject(input);
			
			//generate string with length that is 1 character greater than MAX_LENGTH
			String large_str = NumericRule.generateRandomAlphabeticString(Integer.parseInt(rule.getValue())-1);

			path.addPathObject(new ActionPOJO("sendKeys", large_str));
			tests.add(path);
		}
		else if(rule.getType().equals(RuleType.MAX_VALUE)){
			//generate empty string test
			Test path = new TestPOJO();
			path.addPathObject(input);
			path.addPathObject(new ActionPOJO("click", ""));
			path.addPathObject(input);
			
			//generate string with length equal to MAX_LENGTH
			path.addPathObject(new ActionPOJO("sendKeys", Integer.toString(Integer.parseInt(rule.getValue()))));
			tests.add(path);
			
			//generate single character str test
			path = new TestPOJO();
			path.addPathObject(input);
			path.addPathObject(new ActionPOJO("click", ""));
			path.addPathObject(input);
			
			//generate string with length that is 1 character greater than MAX_LENGTH
			path.addPathObject(new ActionPOJO("sendKeys", Integer.toString(Integer.parseInt(rule.getValue())+1)));
			tests.add(path);
		}
		else if(rule.getType().equals(RuleType.MIN_VALUE)){
			//generate empty string test
			Test path = new TestPOJO();
			path.addPathObject(input);
			path.addPathObject(new ActionPOJO("click", ""));
			path.addPathObject(input);
			
			//generate string with length equal to MAX_LENGTH
			path.addPathObject(new ActionPOJO("sendKeys", Integer.toString(Integer.parseInt(rule.getValue()))));
			tests.add(path);
			
			//generate single character str test
			path = new TestPOJO();
			path.addPathObject(input);
			path.addPathObject(new ActionPOJO("click", ""));
			path.addPathObject(input);
			
			//generate string with length that is 1 character greater than MAX_LENGTH
			path.addPathObject(new ActionPOJO("sendKeys", Integer.toString(Integer.parseInt(rule.getValue())-1)));
			tests.add(path);
		}
		return tests;
		
	}
	
	@Deprecated
	public static List<Test> generateRequirementChecks(PageElement input, boolean isRequired){
		assert input.getName().equals("input");
		
		List<Test> tests = new ArrayList<Test>();
		String input_type = input.getAttribute("type").getVals().get(0);
		if(input_type.equals("text") ||
				input_type.equals("textarea") ||
				input_type.equals("email")){
			//generate empty string test
			Test path = new TestPOJO();
			path.addPathObject(input);
			path.addPathObject(new ActionPOJO("click", ""));
			path.addPathObject(input);
			path.addPathObject(new ActionPOJO("sendKeys", ""));
			tests.add(path);
			
			//generate single character str test
			Test path_2 = new TestPOJO();
			path_2.addPathObject(input);
			path_2.addPathObject(new ActionPOJO("click", ""));
			path_2.addPathObject(input);
			path_2.addPathObject(new ActionPOJO("sendKeys", "a"));
			tests.add(path_2);
		}
		else if( input_type.equals("number")){

			//generate empty string test
			Test path = new TestPOJO();
			path.addPathObject(input);
			path.addPathObject(new ActionPOJO("click", ""));
			path.addPathObject(input);
			path.addPathObject(new ActionPOJO("sendKeys", ""));
			tests.add(path);
			
			//generate single character str test
			Test path_2 = new TestPOJO();
			path_2.addPathObject(input);
			path_2.addPathObject(new ActionPOJO("click", ""));
			path_2.addPathObject(input);
			path_2.addPathObject(new ActionPOJO("sendKeys", "0"));
			tests.add(path_2);
		}
		return tests;
	}
	
	/**
	 * Generates a test with an alphabetic character to verify an alphabetic restriction
	 * @param input_elem
	 * @param rule
	 * @return
	 */
	private static List<Test> generateAlphabeticRestrictionTests(PageElement input_elem, Rule rule) {
		//generate single character str test		
		List<Test> tests = new ArrayList<Test>();
		Test path = new TestPOJO();
		path.addPathObject(input_elem);
		path.addPathObject(new ActionPOJO("click", ""));
		path.addPathObject(input_elem);
		path.addPathObject(new ActionPOJO("sendKeys", "a"));
		tests.add(path);		
		return tests;
	}

	private static List<Test> generateNumericRestrictionTests(PageElement input_elem, Rule rule) {
		List<Test> tests = new ArrayList<Test>();

		Test path = new TestPOJO();
		path.addPathObject(input_elem);
		path.addPathObject(new ActionPOJO("click", ""));
		path.addPathObject(input_elem);
		path.addPathObject(new ActionPOJO("sendKeys", "0"));
		tests.add(path);
		
		return tests;
	}

	private static List<Test> generateSpecialCharacterRestrictionTests(PageElement input_elem, Rule rule) {
		//generate single character str test
		List<Test> tests = new ArrayList<Test>();
		Test path = new TestPOJO();
		path.addPathObject(input_elem);
		path.addPathObject(new ActionPOJO("click", ""));
		path.addPathObject(input_elem);
		path.addPathObject(new ActionPOJO("sendKeys", "!"));
		tests.add(path);
		
		return tests;
	}

	private static List<Test> generateEnabledTests(PageElement input_elem, Rule rule) {
		// TODO Auto-generated method stub
		return null;
	}

	private static List<Test> generateReadOnlyTests(PageElement input_elem, Rule rule) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private static List<Test> generatePatternTests(PageElement input_elem, Rule rule) {
		// TODO Auto-generated method stub
		return null;
	}

	private static List<Test> generateEmailTests(PageElement input_elem, Rule rule) {
		List<Test> tests = new ArrayList<Test>();

		Test path = new TestPOJO();
		path.addPathObject(input_elem);
		path.addPathObject(new ActionPOJO("click", ""));
		path.addPathObject(input_elem);
		path.addPathObject(new ActionPOJO("sendKeys", "!test@test.com"));
		tests.add(path);		

		//generate single character str test	
		Test path1 = new TestPOJO();
		path1.addPathObject(input_elem);
		path1.addPathObject(new ActionPOJO("click", ""));
		path1.addPathObject(input_elem);
		path1.addPathObject(new ActionPOJO("sendKeys", "test!test.com"));
		tests.add(path1);

		Test path2 = new TestPOJO();
		path2.addPathObject(input_elem);
		path2.addPathObject(new ActionPOJO("click", ""));
		path2.addPathObject(input_elem);
		path2.addPathObject(new ActionPOJO("sendKeys", "test@test"));
		tests.add(path2);
		
		Test path3 = new TestPOJO();
		path3.addPathObject(input_elem);
		path3.addPathObject(new ActionPOJO("click", ""));
		path3.addPathObject(input_elem);
		path3.addPathObject(new ActionPOJO("sendKeys", "test.test@test"));
		tests.add(path3);
		
		Test path4 = new TestPOJO();
		path4.addPathObject(input_elem);
		path4.addPathObject(new ActionPOJO("click", ""));
		path4.addPathObject(input_elem);
		path4.addPathObject(new ActionPOJO("sendKeys", "test_test@test"));
		tests.add(path4);
		
		return tests;
	}
}
