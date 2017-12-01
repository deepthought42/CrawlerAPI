package com.minion.actors;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


import org.slf4j.Logger;import org.slf4j.LoggerFactory;

import com.qanairy.models.Test;
import com.minion.browsing.Browser;
import com.minion.browsing.Crawler;
import com.minion.browsing.element.ComplexField;
import com.minion.browsing.form.Form;
import com.minion.browsing.form.FormField;
import com.minion.structs.Message;
import com.qanairy.models.Action;
import com.qanairy.models.Page;
import com.qanairy.models.PageElement;
import com.qanairy.models.Path;
import com.qanairy.models.PathObject;
import com.qanairy.rules.Rule;
import com.qanairy.rules.NumericRule;
import com.qanairy.rules.RuleType;

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
			Message<?> acct_message = (Message<?>)message;
			log.info("Starting form tester");
			Path path = null;
			if(acct_message.getData() instanceof Path){
				path = (Path)acct_message.getData();
			}
			else if(acct_message.getData() instanceof Test){
				path = ((Test)acct_message.getData()).getPath();
			}
			
			//get first page in path
			Page page = (Page)path.getPath().get(0);
			//if(path_obj instanceof Page){
				//Page page = (Page)path_obj;
				int cnt = 0;
			  	Browser browser = null;
			  	
			  	while(browser == null && cnt < 5){
			  		try{
				  		browser = new Browser(page.getUrl().toString(), "phantomjs");
						break;
					}catch(NullPointerException e){
						log.error(e.getMessage());
					}
					cnt++;
				}
			  	//clone path
			  	//Path new_path = Path.clone(path);		

				Page current_page = Crawler.crawlPath(path, browser);

			  	List<Form> forms = Browser.extractAllForms(current_page, browser);
			  	List<Path> form_paths = new ArrayList<Path>();
			  	log.info("Total Choice fields in form : " + forms.get(0).getFormFields().size());
			  	log.info("Generating tests for " + forms.size() + " forms");
			  	for(Form form : forms){
			  		form_paths.addAll(FormTestDiscoveryActor.generateAllFormPaths(path, form));
			  	}
			  	
			  	final ActorRef work_allocator = this.getContext().actorOf(Props.create(WorkAllocationActor.class), "workAllocator"+UUID.randomUUID());
				for(Path expanded : form_paths){
					//send all paths to work allocator to be evaluated
					Message<Path> expanded_path_msg = new Message<Path>(acct_message.getAccountKey(), expanded);
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
	public static List<Path> generateRuleTests(PageElement input_elem, Rule rule, PageElement submitField){
		assert rule != null;
		
		List<Path> paths = new ArrayList<Path>();
		if(rule.getType().equals(RuleType.REQUIRED)){
			//generate required path for element type
			log.info("SHOULD BE GENERATING REQUIRED TESTS");
			paths.addAll(generateRequirementChecks(input_elem, true, submitField));
		}
		else if(rule.getType().equals(RuleType.ALPHABETIC_RESTRICTION)){
			log.info("SHOULD BE GENERATED ALPHABETIC ONLY RESTRICTION");
			paths.addAll(generateAlphabeticRestrictionTests(input_elem, rule, submitField));
		}
		else if(rule.getType().equals(RuleType.NUMERIC_RESTRICTION)){
			log.info("SHOULD BE GENERATING NUMBER RESTRICTION TESTS ");
			paths.addAll(generateNumericRestrictionTests(input_elem, rule, submitField));
		}
		else if(rule.getType().equals(RuleType.SPECIAL_CHARACTER_RESTRICTION)){
			log.info("SHOULD BE GENERATING SPECIAL CHARACTER RESTRICTION path");		
			paths.addAll(generateSpecialCharacterRestrictionTests(input_elem, rule, submitField));
		}
		else if(rule.getType().equals(RuleType.DISABLED)){
			log.info("SHOULD BE GENERATING DISABLED FIELD TESTS ");
			//path.addAll(generateEnabledTests(input_elem, rule));
		}
		else if(rule.getType().equals(RuleType.READ_ONLY)){
			log.info("SHOULD BE GENERATING READ-ONLY FIELD TESTS ");
			//path.addAll(generateReadOnlyTests(input_elem, rule));
		}
		else if(rule.getType().equals(RuleType.MAX_LENGTH)){
			log.info("SHOULD BE GENERATING MAX LENGTH TESTS ");
			paths.addAll(generateLengthBoundaryTests(input_elem, rule, submitField));
		}
		else if(rule.getType().equals(RuleType.MIN_LENGTH)){
			log.info("SHOULD BE GENERATING MIN LENGTH TESTS ");
			paths.addAll(generateLengthBoundaryTests(input_elem, rule, submitField));
		}
		else if(rule.getType().equals(RuleType.MAX_VALUE)){
			log.info("SHOULD BE GENERATING MAX VALUE TESTS ");
			paths.addAll(generateLengthBoundaryTests(input_elem, rule, submitField));
		}
		else if(rule.getType().equals(RuleType.MIN_VALUE)){
			log.info("SHOULD BE GENERATING MIN LENGTH TESTS ");
			paths.addAll(generateLengthBoundaryTests(input_elem, rule, submitField));
		}
		else if(rule.getType().equals(RuleType.PATTERN)){
			log.info("SHOULD BE GENERATING PATTERN TESTS ");
			paths.addAll(generatePatternTests(input_elem, rule, submitField));
		}
		else if(rule.getType().equals(RuleType.EMAIL_PATTERN)){
			log.info("SHOULD BE GENERATING EMAIL PATTERN TESTS ");
			paths.addAll(generateEmailTests(input_elem, rule, submitField));	
		}
		return paths;
	}
	
	public static List<Path> generateBoundaryTests(PageElement input){
		
		return null;
		
	}
	
	public static List<Path> generateLengthBoundaryTests(PageElement input, Rule rule, PageElement submit){
		log.info("generating length boundary test paths");

		List<Path> paths = new ArrayList<Path>();

		if(rule.getType().equals(RuleType.MAX_LENGTH)){
			//generate empty string test
			Path path = new Path();
			path.add(input);
			path.add(new Action("click", ""));
			path.add(input);
			
			//generate string with length equal to MAX_LENGTH
			String short_str = NumericRule.generateRandomAlphabeticString(Integer.parseInt(rule.getValue()));
			log.info("Generated string of length : " + short_str.length());
			
			path.add(new Action("sendKeys", short_str));
			path.add(submit);
			path.add(new Action("click", ""));
			paths.add(path);
	
			//generate single character str test
			path = new Path();
			path.add(input);
			path.add(new Action("click", ""));
			path.add(input);
			
			//generate string with length that is 1 character greater than MAX_LENGTH
			String large_str = NumericRule.generateRandomAlphabeticString(Integer.parseInt(rule.getValue())+1);
			log.info("Generated string of length : " + large_str.length());
			path.add(new Action("sendKeys", large_str));
			path.add(submit);
			path.add(new Action("click", ""));
			paths.add(path);
		}
		else if(rule.getType().equals(RuleType.MIN_LENGTH)){
			//generate empty string test
			Path path = new Path();
			path.add(input);
			path.add(new Action("click", ""));
			path.add(input);
			
			//generate string with length equal to MAX_LENGTH
			String short_str = NumericRule.generateRandomAlphabeticString(Integer.parseInt(rule.getValue()));
			log.info("Generated string of length : " + short_str.length());

			path.add(new Action("sendKeys", short_str));
			path.add(submit);
			path.add(new Action("click", ""));
			paths.add(path);
			
			log.info("adding single character text string sendKeys action For MIN LENGTH" );

			//generate single character str test
			path = new Path();
			path.add(input);
			path.add(new Action("click", ""));
			path.add(input);
			
			//generate string with length that is 1 character greater than MAX_LENGTH
			String large_str = NumericRule.generateRandomAlphabeticString(Integer.parseInt(rule.getValue())-1);
			log.info("Generated string of length : " + large_str.length());

			path.add(new Action("sendKeys", large_str));
			path.add(submit);
			path.add(new Action("click", ""));
			paths.add(path);
		}
		else if(rule.getType().equals(RuleType.MAX_VALUE)){
			//generate empty string test
			Path path = new Path();
			path.add(input);
			path.add(new Action("click", ""));
			path.add(input);
			
			//generate string with length equal to MAX_LENGTH
			path.add(new Action("sendKeys", Integer.toString(Integer.parseInt(rule.getValue()))));
			path.add(submit);
			path.add(new Action("click", ""));
			paths.add(path);
			
			log.info("adding single character text string sendKeys action" );

			//generate single character str test
			path = new Path();
			path.add(input);
			path.add(new Action("click", ""));
			path.add(input);
			
			//generate string with length that is 1 character greater than MAX_LENGTH
			path.add(new Action("sendKeys", Integer.toString(Integer.parseInt(rule.getValue())+1)));
			path.add(submit);
			path.add(new Action("click", ""));
			paths.add(path);
		}
		else if(rule.getType().equals(RuleType.MIN_VALUE)){
			//generate empty string test
			Path path = new Path();
			path.add(input);
			path.add(new Action("click", ""));
			path.add(input);
			
			//generate string with length equal to MAX_LENGTH
			path.add(new Action("sendKeys", Integer.toString(Integer.parseInt(rule.getValue()))));
			path.add(submit);
			path.add(new Action("click", ""));
			paths.add(path);
			
			log.info("adding single character text string sendKeys action" );

			//generate single character str test
			path = new Path();
			path.add(input);
			path.add(new Action("click", ""));
			path.add(input);
			
			//generate string with length that is 1 character greater than MAX_LENGTH
			path.add(new Action("sendKeys", Integer.toString(Integer.parseInt(rule.getValue())-1)));
			path.add(submit);
			path.add(new Action("click", ""));
			paths.add(path);
		}
		return paths;
		
	}
	
	public static List<Path> generateValueBoundaryTests(PageElement input){
		return null;	
	}
	
	public static List<Path> generateRequirementChecks(PageElement input, boolean isRequired, PageElement submit){
		assert input.getName().equals("input");
		log.info("generating requirements checks");
		
		List<Path> paths = new ArrayList<Path>();
		String input_type = input.getAttribute("type").getVals().get(0);
		if(input_type.equals("text") ||
				input_type.equals("textarea") ||
				input_type.equals("email")){
			log.info("adding empty text string sendKeys action" );
			//generate empty string test
			Path path = new Path();
			path.add(input);
			path.add(new Action("click", ""));
			path.add(input);
			path.add(new Action("sendKeys", ""));
			path.add(submit);
			path.add(new Action("click", ""));
			paths.add(path);
			
			log.info("adding single character text string sendKeys action" );

			//generate single character str test
			Path path_2 = new Path();
			path.add(input);
			path.add(new Action("click", ""));
			path_2.add(input);
			path_2.add(new Action("click", ""));
			path_2.add(input);
			path_2.add(new Action("sendKeys", "a"));
			path.add(submit);
			path.add(new Action("click", ""));
			paths.add(path_2);
		}
		else if( input_type.equals("number")){
			log.info("adding empty text string sendKeys action" );

			//generate empty string test
			Path path = new Path();
			path.add(input);
			path.add(new Action("click", ""));
			path.add(input);
			path.add(new Action("sendKeys", ""));
			path.add(submit);
			path.add(new Action("click", ""));
			paths.add(path);
			
			log.info("adding single digit text string sendKeys action" );

			//generate single character str test
			Path path_2 = new Path();
			path_2.add(input);
			path_2.add(new Action("click", ""));
			path_2.add(input);
			path_2.add(new Action("sendKeys", "0"));
			path.add(submit);
			path.add(new Action("click", ""));
			paths.add(path_2);
		}
		return paths;
	}
	
	/**
	 * Generates a test with an alphabetic character to verify an alphabetic restriction
	 * @param input_elem
	 * @param rule
	 * @return
	 */
	private static List<Path> generateAlphabeticRestrictionTests(PageElement input_elem, Rule rule, PageElement submit) {
		//generate single character str test		
		List<Path> paths = new ArrayList<Path>();
		Path path = new Path();
		path.add(input_elem);
		path.add(new Action("click", ""));
		path.add(input_elem);
		path.add(new Action("sendKeys", "a"));
		path.add(submit);
		path.add(new Action("click", ""));
		paths.add(path);		
		return paths;
	}

	private static List<Path> generateNumericRestrictionTests(PageElement input_elem, Rule rule, PageElement submit) {
		List<Path> paths = new ArrayList<Path>();

		Path path = new Path();
		path.add(input_elem);
		path.add(new Action("click", ""));
		path.add(input_elem);
		path.add(new Action("sendKeys", "0"));
		path.add(submit);
		path.add(new Action("click", ""));
		paths.add(path);
		
		log.info("adding single digit text string sendKeys action" );
		return paths;
	}

	private static List<Path> generateSpecialCharacterRestrictionTests(PageElement input_elem, Rule rule, PageElement submit) {
		//generate single character str test
		List<Path> paths = new ArrayList<Path>();
		Path path = new Path();
		path.add(input_elem);
		path.add(new Action("click", ""));
		path.add(input_elem);
		path.add(new Action("sendKeys", "!"));
		path.add(submit);
		path.add(new Action("click", ""));
		paths.add(path);
		
		return paths;
	}

	private static List<Path> generateEnabledTests(PageElement input_elem, Rule rule, PageElement submit) {
		// TODO Auto-generated method stub
		return null;
	}

	private static List<Path> generateReadOnlyTests(PageElement input_elem, Rule rule, PageElement submit) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private static List<Path> generatePatternTests(PageElement input_elem, Rule rule, PageElement submit) {
		// TODO Auto-generated method stub
		return null;
	}

	private static List<Path> generateEmailTests(PageElement input_elem, Rule rule, PageElement submit) {
		List<Path> paths = new ArrayList<Path>();
		
		Path path = new Path();
		path.add(input_elem);
		path.add(new Action("click", ""));
		path.add(input_elem);
		path.add(new Action("sendKeys", "!test@test.com"));
		path.add(submit);
		path.add(new Action("click", ""));
		paths.add(path);		

		//generate single character str test	
		Path path1 = new Path();
		path1.add(input_elem);
		path1.add(new Action("click", ""));
		path1.add(input_elem);
		path1.add(new Action("sendKeys", "test!test.com"));
		path.add(submit);
		path.add(new Action("click", ""));
		paths.add(path1);

		Path path2 = new Path();
		path2.add(input_elem);
		path2.add(new Action("click", ""));
		path2.add(input_elem);
		path2.add(new Action("sendKeys", "test@test"));
		path.add(submit);
		path.add(new Action("click", ""));
		paths.add(path2);
		
		Path path3 = new Path();
		path3.add(input_elem);
		path3.add(new Action("click", ""));
		path3.add(input_elem);
		path3.add(new Action("sendKeys", "test.test@test"));
		path.add(submit);
		path.add(new Action("click", ""));
		paths.add(path3);
		
		Path path4 = new Path();
		path4.add(input_elem);
		path4.add(new Action("click", ""));
		path4.add(input_elem);
		path4.add(new Action("sendKeys", "test_test@test"));
		path.add(submit);
		path.add(new Action("click", ""));
		paths.add(path4);
		
		return paths;
	}
	/**
	 * 
	 * @param path
	 * @param form
	 * @return
	 */
	public static List<Path> generateAllFormPaths(Path path, Form form){
		List<Path> form_paths = new ArrayList<Path>();
		log.info("Form complex field size : " + form.getFormFields().size());
		for(ComplexField complex_field: form.getFormFields()){
			log.info("complex field elements size : " + complex_field.getElements().size());
			//for each field in the complex field generate a set of tests for all known rules
			for(FormField field : complex_field.getElements()){
				PageElement input_elem = field.getInputElement();
				
				boolean field_exists = false;
				
				//CHECK IF FORM FIELD ALREADY EXISTS IN PATH
				for(PathObject path_obj : path.getPath()){
					if(path_obj instanceof PageElement){
						PageElement page_elem = (PageElement)path_obj;
						if(page_elem.equals(input_elem)){
							field_exists = true;
						}
					}
				}
				
				if(field_exists){
					continue;
				}
				
				List<Rule> rules = field.getInputElement().getRules();
				for(Rule rule : rules){
					List<Path> path_list = generateRuleTests(input_elem, rule, form.getSubmitField());
					log.info("# rule tests created : " + path_list.size());
					for(Path curr_path : path_list){
						Path clone_path = Path.clone(path);
						clone_path.getPath().addAll(curr_path.getPath());
						form_paths.add(clone_path);
					}
				}
			}
		}
		log.info("Tests created for form : " +form_paths.size());
		return form_paths;
	}
}
