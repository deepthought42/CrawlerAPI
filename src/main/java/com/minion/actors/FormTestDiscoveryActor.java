package com.minion.actors;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.encog.util.normalize.input.InputField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minion.api.PastPathExperienceController;
import com.minion.browsing.Browser;
import com.minion.browsing.Crawler;
import com.minion.browsing.Page;
import com.minion.browsing.PageElement;
import com.minion.browsing.PathObject;
import com.minion.browsing.element.ComboElement;
import com.minion.browsing.form.Form;
import com.minion.browsing.form.FormField;
import com.minion.structs.Message;
import com.minion.structs.Path;
import com.minion.tester.Test;
import com.minion.tester.TestRecord;

import akka.actor.UntypedActor;

/**
 * Handles discovery and creation of various form tests
 */
public class FormTestDiscoveryActor extends UntypedActor {
	private static final Logger log = LoggerFactory.getLogger(FormTestDiscoveryActor.class);

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
			PathObject<?> page_obj = path.getPath().get(0);
			if(page_obj instanceof Page){
				Page page = (Page)page_obj;
			  	Browser browser = new Browser(page.getUrl().toString());
			  	
			  	//clone path
			  	//Path new_path = Path.clone(path);
			  	
			  	log.info("Crawling path for test :: "+(path!=null));
			  	Page current_page = Crawler.crawlPath(path);
			  	
			  	log.info("Getting current choices on page for form tests");
			  	List<Form> choices = Browser.extractAllForms(current_page, browser.getDriver());
			  	System.err.println("Total Choice fields : " + choices.size());
			  	
			  	
			  	for(Form form : choices){
			  		for(FormField field : form.getFormFields()){
			  			log.info("Form field combo elements : "+field.getComboElement().getElements().size());
			  			for(PageElement elem : field.getComboElement().getElements()){
			  				log.info("assessing element with xpath : " +elem.getXpath());
			  			}
			  		}
			  	}
			  	
			  	List<PageElement> input_choices = Browser.extractAllInputElements(current_page, browser.getDriver());
			  	System.err.println("Total input Choice fields : " + input_choices.size());
			}
			else{
				
			}

		  	
		  	/*Page current_page = null;
		  	if(last_page != null && last_page.getSrc().equals(Browser.cleanSrc(browser.getDriver().getPageSource())) && path.getPath().size() > 1){
		  		current_page = last_page;
		  		path.setIsUseful(false);
		  	}
		  	else{
		  		current_page = browser.getPage();
				path.setIsUseful(true);
				if(path.size() > 1){
					path.add(current_page);
				}
				Message<Path> path_msg = new Message<Path>(acct_msg.getAccountKey(), path);

				final ActorRef path_expansion_actor = this.getContext().actorOf(Props.create(PathExpansionActor.class), "PathExpansionActor"+UUID.randomUUID());
				path_expansion_actor.tell(path_msg, getSelf() );
		  	}
		  	this.browser.close();

			Test test = new Test(path, current_page, current_page.getUrl().getHost());
			PastPathExperienceController.broadcastTestExperience(test);
			
			log.info("Saving test");
		  	Message<Test> test_msg = new Message<Test>(acct_msg.getAccountKey(), test);

			final ActorRef memory_actor = this.getContext().actorOf(Props.create(MemoryRegistryActor.class), "MemoryRegistryActor"+UUID.randomUUID());
			memory_actor.tell(test_msg, getSelf() );
			
			
			//get all checkbox fields
			// check if checkbox field is required
			Browser.extractAllChoiceElements(elem, tag, driver);
			//get all radio fields
			// check if radio button is required
			
				log.info("Path passed to work allocator");
				final ActorRef browser_actor = this.getContext().actorOf(Props.create(BrowserActor.class), "browserActor"+UUID.randomUUID());
				browser_actor.tell(acct_message, getSelf() );
			}*/
		}
	}
	
	/**
	 * Runs an {@code Test} 
	 * 
	 * @param test test to be ran
	 * 
	 * @pre test != null
	 * @return
	 */
	public static TestRecord runTest(Test test){		
		assert test != null;
		
		log.info("Running test...");
		boolean passing = false;
		try {
			Page page = Crawler.crawlPath(test.getPath());
			passing = test.isTestPassing(page);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		TestRecord test_record = new TestRecord(test, new Date(), passing );
		return test_record;
	}
	
	public static Test generateBoundaryTests(InputField input){
		
		return null;
		
	}
	
	public static Test generateLengthBoundaryTests(InputField input){
		return null;
		
	}
	
	public static Test  generateValueBoundaryTests(InputField input){
		return null;
		
	}
	
	public static Test  generateRequirementChecks(InputField input){
		return null;
		
	}
	
	public static Test  generateCharacterTests(InputField input){
		return null;
		
	}
	
	public static Test generateAllFormTests(Form form){
		for(FormField field: form.getFormFields()){
			ComboElement elem = field.getComboElement();

			System.out.println("ELEMENT CLASS : "+elem.getClass());
		}
		
		return null;
	}
}
