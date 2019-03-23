package com.minion.actors;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.openqa.grid.common.exception.GridException;
import org.openqa.selenium.WebDriverException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.minion.api.MessageBroadcaster;
import com.minion.browsing.Browser;
import com.minion.browsing.BrowserConnectionFactory;
import com.minion.browsing.Crawler;
import com.minion.structs.Message;
import com.minion.util.Timing;
import com.qanairy.models.Action;
import com.qanairy.models.Attribute;
import com.qanairy.models.Domain;
import com.qanairy.models.ExploratoryPath;
import com.qanairy.models.Form;
import com.qanairy.models.PageElement;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Test;
import com.qanairy.models.TestUser;
import com.qanairy.models.enums.BrowserEnvironment;
import com.qanairy.models.enums.FormType;
import com.qanairy.models.repository.ActionRepository;
import com.qanairy.services.DomainService;
import com.qanairy.services.FormService;
import com.qanairy.services.TestService;

import akka.actor.AbstractActor;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;
import akka.event.Logging;
import akka.event.LoggingAdapter;

@Component
@Scope("prototype")
public class LoginFormTestDiscoveryActor extends AbstractActor {
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

	@Autowired
	private Crawler crawler;
	
	@Autowired
	private FormService form_service;
	
	@Autowired
	private DomainService domain_service;
	
	@Autowired
	private TestService test_service;
	
	@Autowired 
	private ActionRepository action_repo;
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(Message.class, message -> {
					//check that message data is of type Form and that the form type is set to login
					log.info("login form test discovery actor is up!");
					if(message.getData() instanceof Form){
						Form form = (Form)message.getData();
						Domain domain = domain_service.findByHost(message.getOptions().get("host").toString());
						//check if form type is set to login
						if(form.getType().equals(FormType.LOGIN)){
							log.info("FORM TYPE IS    LOGIN");
							//get current domain from options list within message
							//  generate path leading to current form
							
							//get users for current domain
							Set<TestUser> test_users = domain_service.getTestUsers(domain);
							log.info("generating tests for "+test_users.size()+"   users");
							for(TestUser user : test_users){
								ExploratoryPath exploratory_path = initializeFormTest(form);

								//  clone test
								//  get username element and add it to path
								List<PageElement> elements = form.getFormFields();
								//find username input element
								PageElement username_elem = findInputElementByAttribute(elements, "username");
								
								if(username_elem == null){
									username_elem = findInputElementByAttribute(elements, "email");
									if(username_elem == null){
										log.info("could not find username !!!!!!!!");
										//throw error that cannot find username field
									}
								}

								exploratory_path.addPathObject(username_elem);
								exploratory_path.addToPathKeys(username_elem.getKey());

								Action type_username = new Action("sendKeys", user.getUsername());
								Action action_record = action_repo.findByKey(type_username.getKey());
								if(action_record != null){
									type_username = action_record;
								}
								exploratory_path.addPathObject(type_username);
								exploratory_path.addToPathKeys(type_username.getKey());
								//	add typing action to path with value equal to user.username
								
								
								//  get password element and add it to the path
								PageElement password_elem = findInputElementByAttribute(elements, "password");

								if(password_elem == null){
									log.info("could not find password !!!!!!!!");
									//throw error that cannot find password field
								}
								
								//  add typing action to path with value equal to user.password	
								
								exploratory_path.addPathObject(password_elem);
								exploratory_path.addToPathKeys(password_elem.getKey());
								Action type_password = new Action("sendKeys", user.getPassword());
								action_record = action_repo.findByKey(type_password.getKey());
								if(action_record != null){
									type_password= action_record;
								}
								exploratory_path.addPathObject(type_password);
								exploratory_path.addToPathKeys(type_password.getKey());
								
								
								//find submit button
								exploratory_path.addPathObject(form.getSubmitField());
								exploratory_path.addToPathKeys(form.getSubmitField().getKey());
								
								List<Action> action_list = new ArrayList<Action>();
								Action submit_login = new Action("click", "");
								action_record = action_repo.findByKey(submit_login.getKey());
								if(action_record != null){
									submit_login= action_record;
								}
								
								action_list.add(submit_login);

								//exploratory_path.setPossibleActions(action_list);
								exploratory_path.addPathObject(submit_login);
								exploratory_path.addToPathKeys(submit_login.getKey());
								
								PageState result_page = null;
								Browser browser = null;
								int tries = 0;
								
								do{
									try{
										log.info("Crawling path for login form test discovery");
										browser = BrowserConnectionFactory.getConnection(browser.getBrowserName(), BrowserEnvironment.DISCOVERY);
										result_page = crawler.crawlPath(exploratory_path.getPathKeys(), exploratory_path.getPathObjects(), browser, message.getOptions().get("host").toString(), null);
									}catch(NullPointerException e){
										log.error("Error happened while login form test discovery actor attempted to crawl test "+e.getLocalizedMessage());
									} catch (GridException e) {
										e.printStackTrace();
									} catch (WebDriverException e) {
										e.printStackTrace();
									} catch (NoSuchAlgorithmException e) {
										e.printStackTrace();
									}
									finally{
										if(browser != null){
											browser.close();
										}
									}
							  		tries++;
								}while(result_page == null && tries < Integer.MAX_VALUE);
						
								Test test = new Test(exploratory_path.getPathKeys(), exploratory_path.getPathObjects(), result_page, user.getUsername()+" user login");
								test = test_service.save(test, domain.getUrl());
								MessageBroadcaster.broadcastDiscoveredTest(test, domain.getUrl());
							
							}
						}
					}
					postStop();

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

	private ExploratoryPath initializeFormTest(Form form) {
		
		List<String> path_keys = new ArrayList<String>();
		List<PathObject> path_objects = new ArrayList<PathObject>();
		PageState page = form_service.getPageState(form);
		path_objects.add(page);
		path_keys.add(page.getKey());
		
		return new ExploratoryPath(path_keys, path_objects, new ArrayList<Action>());
	}

	private PageElement findInputElementByAttribute(List<PageElement> elements, String search_val) {
		for(PageElement element : elements){
			//check if element is type email
			if(element.getType().contains(search_val)){
				return element;
			}
			
			//check if element has value username in any attributes
			for(Attribute attribute : element.getAttributes()){
				for(String val : attribute.getVals()){
					if(val.contains(search_val)){
						return element;
					}
				}
			}
			if(element.getName().contains(search_val)){
				return element;
			}
			else if(element.getXpath().contains(search_val)){
				return element;
			}
		}
		
		return null;
	}

}
