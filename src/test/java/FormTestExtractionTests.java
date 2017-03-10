import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

import org.testng.annotations.Test;

import com.minion.browsing.Browser;
import com.minion.browsing.form.Form;
import com.qanairy.models.Page;
import com.qanairy.rules.FormRule;
import com.qanairy.rules.formRules.AlphabeticRestrictionRule;
import com.qanairy.rules.formRules.MaximumValueRule;
import com.qanairy.rules.formRules.MinimumValueRule;
import com.qanairy.rules.formRules.NumericRestrictionRule;
import com.qanairy.rules.formRules.ReadOnlyRule;
import com.qanairy.rules.formRules.RequirementRule;
import com.qanairy.rules.formRules.SpecialCharacterRestriction;

/**
 * A group of TestNG tests designed to verify the extraction of tests involving forms and rules on fields
 */
public class FormTestExtractionTests {
	
	/**
	 * Tests if {@link RequirementRule} can be extracted on a required field
	 */
	@Test
	public void testRequirementRuleExtractions(){
		String url = "C:/Users/brand/workspace/WebTestVisualizer/src/test/resources/form_tests/requiredFieldForm.html";
		Browser browser;
		try {
			browser = new Browser(url,"chrome");
			Page page = browser.getPage();
			System.err.println("Extracting forms");
			List<Form> form = Browser.extractAllForms(page, browser);
			
			//System.err.println("Extracting rules");
			//List<FormRule<?>> form_rules = ElementRuleExtractor.extractRules(form.get(0).getFormTag());
			
			System.err.println(form.get(0).getFormFields().get(0).getElements().get(0).getRules().size() + " Rules extracted :: ");
			boolean rule_is_required = false;
			for(FormRule rule : form.get(0).getFormFields().get(0).getElements().get(0).getRules()){
				System.err.println("rule class :: " + rule.getClass());
				if(rule.getClass().equals(RequirementRule.class)){
					rule_is_required = true;
				}
			}
			browser.close();
			assert rule_is_required;
		} 
		catch (MalformedURLException e) {
			e.printStackTrace();
		}
		catch (IOException e){
			e.printStackTrace();
		}
	}
	
	/**
	 * Tests if a {@link NumericRestrionRule} is can be extracted from a number type field
	 */
	@Test
	public void testNumericRestrictionRuleExtractions(){
		String url = "C:/Users/brand/workspace/WebTestVisualizer/src/test/resources/form_tests/numericRestrictionForm.html";
		Browser browser;
		try {
			browser = new Browser(url,"chrome");
			Page page = browser.getPage();
			System.err.println("Extracting forms");
			List<Form> form = Browser.extractAllForms(page, browser);
			
			//System.err.println("Extracting rules");
			//List<FormRule<?>> form_rules = ElementRuleExtractor.extractRules(form.get(0).getFormTag());
			
			System.err.println(form.get(0).getFormFields().get(0).getElements().get(0).getRules().size() + " Rules extracted :: ");
			boolean alphabetic_restrict_rule = false;
			boolean special_char_restrict_rule = false;

			
			for(FormRule rule : form.get(0).getFormFields().get(0).getElements().get(0).getRules()){
				System.err.println("rule class :: " + rule.getClass());
				if(rule.getClass().equals(AlphabeticRestrictionRule.class)){
					alphabetic_restrict_rule = true;
				}
				if(rule.getClass().equals(SpecialCharacterRestriction.class)){
					special_char_restrict_rule = true;
				}
			}
			browser.close();
			assert alphabetic_restrict_rule && special_char_restrict_rule;
		} 
		catch (MalformedURLException e) {
			e.printStackTrace();
		}
		catch (IOException e){
			e.printStackTrace();
		}
	}
	
	/**
	 * Tests if a {@link NumericRestrionRule} is can be extracted from a number type field
	 */
	@Test
	public void testAlphabeticRestrictionRuleExtractions(){
		String url = "C:/Users/brand/workspace/WebTestVisualizer/src/test/resources/form_tests/alphabeticRestrictionForm.html";
		Browser browser;
		try {
			browser = new Browser(url,"chrome");
			Page page = browser.getPage();
			System.err.println("Extracting forms");
			List<Form> form = Browser.extractAllForms(page, browser);
			
			//System.err.println("Extracting rules");
			//List<FormRule<?>> form_rules = ElementRuleExtractor.extractRules(form.get(0).getFormTag());
			
			System.err.println(form.get(0).getFormFields().get(0).getElements().get(0).getRules().size() + " Rules extracted :: ");
			boolean numeric_restrict_rule = false;
			boolean special_char_restrict_rule = false;

			for(FormRule rule : form.get(0).getFormFields().get(0).getElements().get(0).getRules()){
				System.err.println("rule class :: " + rule.getClass());
				if(rule.getClass().equals(NumericRestrictionRule.class)){
					numeric_restrict_rule = true;
				}
				if(rule.getClass().equals(SpecialCharacterRestriction.class)){
					special_char_restrict_rule = true;
				}
			}
			browser.close();
			assert numeric_restrict_rule && special_char_restrict_rule;
		} 
		catch (MalformedURLException e) {
			e.printStackTrace();
		}
		catch (IOException e){
			e.printStackTrace();
		}
	}
	
	/**
	 * Tests if a {@link ReadonlyRule} is can be extracted from a number type field
	 */
	@Test
	public void testReadonlyRestrictionRuleExtractions(){
		String url = "C:/Users/brand/workspace/WebTestVisualizer/src/test/resources/form_tests/readonlyFieldForm.html";
		Browser browser;
		try {
			browser = new Browser(url,"chrome");
			Page page = browser.getPage();
			List<Form> form = Browser.extractAllForms(page, browser);

			boolean readonly_restrict_rule = false;

			for(FormRule rule : form.get(0).getFormFields().get(0).getElements().get(0).getRules()){
				System.err.println("rule class :: " + rule.getClass());
				if(rule.getClass().equals(ReadOnlyRule.class)){
					readonly_restrict_rule = true;
				}
			}
			browser.close();
			assert readonly_restrict_rule;
		} 
		catch (MalformedURLException e) {
			e.printStackTrace();
		}
		catch (IOException e){
			e.printStackTrace();
		}
	}
	
	/**
	 * Tests if a {@link ReadonlyRule} is can be extracted from a number type field
	 */
	@Test
	public void testMinValueRuleExtractions(){
		String url = "C:/Users/brand/workspace/WebTestVisualizer/src/test/resources/form_tests/minValueFieldForm.html";
		Browser browser;
		try {
			browser = new Browser(url,"chrome");
			Page page = browser.getPage();
			List<Form> form = Browser.extractAllForms(page, browser);

			System.err.println(form.get(0).getFormFields().get(0).getElements().get(0).getRules().size() + " Rules extracted :: ");
			boolean min_value_rule = false;

			for(FormRule rule : form.get(0).getFormFields().get(0).getElements().get(0).getRules()){
				System.err.println("rule class :: " + rule.getClass());
				if(rule.getClass().equals(MinimumValueRule.class)){
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
	}
	
	/**
	 * Tests if a {@link ReadonlyRule} is can be extracted from a number type field
	 */
	@Test
	public void testMaxValueRuleExtractions(){
		String url = "C:/Users/brand/workspace/WebTestVisualizer/src/test/resources/form_tests/maxValueFieldForm.html";
		Browser browser;
		try {
			browser = new Browser(url,"chrome");
			Page page = browser.getPage();
			List<Form> form = Browser.extractAllForms(page, browser);

			boolean max_value_rule = false;

			for(FormRule rule : form.get(0).getFormFields().get(0).getElements().get(0).getRules()){
				if(rule.getClass().equals(MaximumValueRule.class)){
					max_value_rule = true;
				}
			}
			browser.close();
			assert max_value_rule;
		} 
		catch (MalformedURLException e) {
			e.printStackTrace();
		}
		catch (IOException e){
			e.printStackTrace();
		}
	}
}
