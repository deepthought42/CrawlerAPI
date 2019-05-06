package browser;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.minion.browsing.Browser;
import com.minion.browsing.BrowserConnectionFactory;
import com.qanairy.models.Form;
import com.qanairy.models.PageState;
import com.qanairy.models.enums.BrowserEnvironment;
import com.qanairy.models.rules.AlphabeticRestrictionRule;
import com.qanairy.models.rules.NumericRestrictionRule;
import com.qanairy.models.rules.NumericRule;
import com.qanairy.models.rules.ReadOnlyRule;
import com.qanairy.models.rules.RequirementRule;
import com.qanairy.models.rules.Rule;
import com.qanairy.models.rules.SpecialCharacterRestriction;
import com.qanairy.services.BrowserService;


/**
 * A group of JUnit tests designed to verify the extraction of tests involving forms and rules on fields
 */
public class FormTestExtractionTests {
	private static Logger log = LoggerFactory.getLogger(FormTestExtractionTests.class);

	/**
	 * Tests if {@link RequirementRule} can be extracted on a required field
	 * @throws Exception 
	 */
	//@Test
	public void testRequirementRuleExtractions() throws Exception{
		//String url = "file:///C:/Users/brand/workspace/WebTestVisualizer/src/test/resources/form_tests/requiredFieldForm.html";
		Browser browser = null;
		try {
			BrowserService browser_service = new BrowserService();
			browser = BrowserConnectionFactory.getConnection("chrome", BrowserEnvironment.DISCOVERY);
			PageState page = browser_service.buildPage(browser);
			log.info("Extracting forms");
			List<Form> form = browser_service.extractAllForms(page, browser);
			
			//log.info("Extracting rules");
			//List<Rule<?>> form_rules = ElementRuleExtractor.extractRules(form.get(0).getFormTag());
			
			log.info(form.get(0).getFormFields().get(0).getRules().size() + " Rules extracted :: ");
			boolean rule_is_required = false;
			for(Rule rule : form.get(0).getFormFields().get(0).getRules()){
				log.info("rule class :: " + rule.getClass());
				if(rule.getClass().equals(RequirementRule.class)){
					rule_is_required = true;
				}
			}
			assert rule_is_required;
		} 
		catch (MalformedURLException e) {
			e.printStackTrace();
		}
		catch (IOException e){
			e.printStackTrace();
		}
		finally{
			browser.close();
		}
	}
	
	/**
	 * Tests if a {@link NumericRestrionRule} is can be extracted from a number type field
	 * @throws Exception 
	 */
	//@Test
	public void testNumericRestrictionRuleExtractions() throws Exception{
		//String url = "file:///C:/Users/brand/workspace/WebTestVisualizer/src/test/resources/form_tests/numericRestrictionForm.html";
		Browser browser = null;
		try {
			BrowserService browser_service = new BrowserService();

			browser = BrowserConnectionFactory.getConnection("chrome", BrowserEnvironment.DISCOVERY);
			PageState page = browser_service.buildPage(browser);
			log.info("Extracting forms");
			List<Form> form = browser_service.extractAllForms(page, browser);
			
			//log.info("Extracting rules");
			//List<Rule<?>> form_rules = ElementRuleExtractor.extractRules(form.get(0).getFormTag());
			
			log.info(form.get(0).getFormFields().get(0).getRules().size() + " Rules extracted :: ");
			boolean alphabetic_restrict_rule = false;
			boolean special_char_restrict_rule = false;

			
			for(Rule rule : form.get(0).getFormFields().get(0).getRules()){
				log.info("rule class :: " + rule.getClass());
				if(rule.getClass().equals(AlphabeticRestrictionRule.class)){
					alphabetic_restrict_rule = true;
				}
				if(rule.getClass().equals(SpecialCharacterRestriction.class)){
					special_char_restrict_rule = true;
				}
			}
			assert alphabetic_restrict_rule && special_char_restrict_rule;
		} 
		catch (MalformedURLException e) {
			e.printStackTrace();
		}
		catch (IOException e){
			e.printStackTrace();
		}
		finally{
			browser.close();
		}
	}
	
	/**
	 * Tests if a {@link NumericRestrionRule} is can be extracted from a number type field
	 * @throws Exception 
	 */
	//@Test
	public void testAlphabeticRestrictionRuleExtractions() throws Exception{
		//String url = "file:///C:/Users/brand/workspace/WebTestVisualizer/src/test/resources/form_tests/alphabeticRestrictionForm.html";
		Browser browser = null;
		try {
			BrowserService browser_service = new BrowserService();
			browser = BrowserConnectionFactory.getConnection("chrome", BrowserEnvironment.DISCOVERY);
			PageState page = browser_service.buildPage(browser);
			log.info("Extracting forms");
			List<Form> form = browser_service.extractAllForms(page, browser);
			
			//log.info("Extracting rules");
			//List<Rule<?>> form_rules = ElementRuleExtractor.extractRules(form.get(0).getFormTag());
			
			log.info(form.get(0).getFormFields().get(0).getRules().size() + " Rules extracted :: ");
			boolean numeric_restrict_rule = false;
			boolean special_char_restrict_rule = false;

			for(Rule rule : form.get(0).getFormFields().get(0).getRules()){
				log.info("rule class :: " + rule.getClass());
				if(rule.getClass().equals(NumericRestrictionRule.class)){
					numeric_restrict_rule = true;
				}
				if(rule.getClass().equals(SpecialCharacterRestriction.class)){
					special_char_restrict_rule = true;
				}
			}
			assert numeric_restrict_rule && special_char_restrict_rule;
		} 
		catch (MalformedURLException e) {
			browser.close();
			e.printStackTrace();
		}
		catch (IOException e){
			browser.close();
			e.printStackTrace();
		}
		finally{
			browser.close();
		}
	}
	
	/**
	 * Tests if a {@link ReadonlyRule} is can be extracted from a number type field
	 * @throws Exception 
	 */
	//@Test
	public void testReadonlyRestrictionRuleExtractions() throws Exception{
		//String url = "file:///C:/Users/brand/workspace/WebTestVisualizer/src/test/resources/form_tests/readonlyFieldForm.html";
		Browser browser = null;
		try {
			BrowserService browser_service = new BrowserService();
			browser = BrowserConnectionFactory.getConnection("chrome", BrowserEnvironment.DISCOVERY);
			PageState page = browser_service.buildPage(browser);
			List<Form> form = browser_service.extractAllForms(page, browser);

			boolean readonly_restrict_rule = false;

			for(Rule rule : form.get(0).getFormFields().get(0).getRules()){
				log.info("rule class :: " + rule.getClass());
				if(rule.getClass().equals(ReadOnlyRule.class)){
					readonly_restrict_rule = true;
				}
			}
			assert readonly_restrict_rule;
		} 
		catch (MalformedURLException e) {
			e.printStackTrace();
		}
		catch (IOException e){
			e.printStackTrace();
		}
		finally{
			browser.close();
		}
	}
	
	/**
	 * Tests if a {@link ReadonlyRule} is can be extracted from a number type field
	 * @throws Exception 
	 */
	//@Test
	public void testMinValueRuleExtractions() throws Exception{
		//String url = "file:///C:/Users/brand/workspace/WebTestVisualizer/src/test/resources/form_tests/minValueFieldForm.html";
		Browser browser = null;
		try {
			BrowserService browser_service = new BrowserService();
			browser = BrowserConnectionFactory.getConnection("chrome", BrowserEnvironment.DISCOVERY);
			PageState page = browser_service.buildPage(browser);
			List<Form> form = browser_service.extractAllForms(page, browser);

			log.info(form.get(0).getFormFields().get(0).getRules().size() + " Rules extracted :: ");
			boolean min_value_rule = false;

			for(Rule rule : form.get(0).getFormFields().get(0).getRules()){
				log.info("rule class :: " + rule.getClass());
				if(rule.getClass().equals(NumericRule.class)){
					min_value_rule = true;
				}
			}
			browser.close();
			assert min_value_rule;
		} 
		catch (MalformedURLException e) {
			e.printStackTrace();
		}
		catch (IOException e){
			e.printStackTrace();
		}
		finally{
			browser.close();
		}
	}
	
	/**
	 * Tests if a {@link ReadonlyRule} is can be extracted from a number type field
	 * @throws Exception 
	 */
	//@Test
	public void testMaxValueRuleExtractions() throws Exception{
		//String url = "file:///C:/Users/brand/workspace/WebTestVisualizer/src/test/resources/form_tests/maxValueFieldForm.html";
		Browser browser = null;
		try {
			BrowserService browser_service = new BrowserService();
			browser = BrowserConnectionFactory.getConnection("chrome", BrowserEnvironment.DISCOVERY);
			PageState page = browser_service.buildPage(browser);
			List<Form> form = browser_service.extractAllForms(page, browser);

			boolean max_value_rule = false;

			for(Rule rule : form.get(0).getFormFields().get(0).getRules()){
				if(rule.getClass().equals(NumericRule.class)){
					max_value_rule = true;
				}
			}
			assert max_value_rule;
		} 
		catch (MalformedURLException e) {
			browser.close();
			e.printStackTrace();
		}
		catch (IOException e){
			browser.close();
			e.printStackTrace();
		}
		finally{
			browser.close();
		}
	}
}
