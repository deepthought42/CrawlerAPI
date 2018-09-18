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
import com.minion.browsing.Crawler;
import com.minion.structs.Message;
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
import com.qanairy.models.enums.FormType;
import com.qanairy.models.repository.ActionRepository;
import com.qanairy.models.repository.TestRepository;
import com.qanairy.services.DomainService;
import com.qanairy.services.FormService;

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
	private TestRepository test_repo;
	
	@Autowired 
	private ActionRepository action_repo;
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(Message.class, message -> {
					//check that message data is of type Form and that the form type is set to login
					
					if(message.getData() instanceof Form){
						Form form = (Form)message.getData();
						Domain domain = domain_service.findByHost(message.getOptions().get("host").toString());
						//check if form type is set to login
						if(form.getType().equals(FormType.LOGIN)){
							//get current domain from options list within message
							//  generate path leading to current form
							
							//get users for current domain
							Set<TestUser> test_users = domain_service.getTestUsers(domain);
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
										System.err.println("could not find username !!!!!!!!");
										//throw error that cannot find username field
									}
								}
								System.err.println("USERNAME ELEMENT :::   "+username_elem);

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
								System.err.println("PASSWORD ELEMENT :::   "+password_elem);
								//  add typing action to path with value equal to user.password	
								if(password_elem == null){
									System.err.println("could not find password !!!!!!!!");
									//throw error that cannot find password field
								}
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
								Action submit_login = new Action("click");
								action_record = action_repo.findByKey(submit_login.getKey());
								if(action_record != null){
									submit_login= action_record;
								}
								
								exploratory_path.setPossibleActions(action_list);
								
								PageState result_page = null;
								Browser browser = new Browser((String)message.getOptions().get("browser"));
								int tries = 0;
								
								do{
									try{
										System.err.println("Crawling path");
										result_page = crawler.crawlPath(exploratory_path.getPathKeys(), exploratory_path.getPathObjects(), browser, message.getOptions().get("host").toString());
										break;
									}catch(NullPointerException e){
										browser = new Browser(browser.getBrowserName());
										log.error("Error happened while exploratory actor attempted to crawl test "+e.getLocalizedMessage());
										e.printStackTrace();
									} catch (GridException e) {
										browser = new Browser(browser.getBrowserName());
										// TODO Auto-generated catch block
										e.printStackTrace();
									} catch (WebDriverException e) {
										browser = new Browser(browser.getBrowserName());
										// TODO Auto-generated catch block
										e.printStackTrace();
									} catch (NoSuchAlgorithmException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
									tries++;
								}while(result_page == null && tries < 5);
							
								Test test = new Test(exploratory_path.getPathKeys(), exploratory_path.getPathObjects(), result_page, "successful login test");
								Test test_record = test_repo.findByKey(test.getKey());
								if(test_record == null){
									test = test_repo.save(test);
									MessageBroadcaster.broadcastDiscoveredTest(test, domain.getUrl());
								}
								else{
									test = test_record;
								}
								domain.addTest(test);
								domain_service.save(domain);
							}
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

	private ExploratoryPath initializeFormTest(Form form) {
		
		List<String> path_keys = new ArrayList<String>();
		List<PathObject> path_objects = new ArrayList<PathObject>();
		PageState page = form_service.getPageState(form);
		path_objects.add(page);
		path_keys.add(page.getKey());
		
		return new ExploratoryPath(path_keys, path_objects, null);
	}

	private PageElement findInputElementByAttribute(List<PageElement> elements, String search_val) {
		for(PageElement element : elements){
			boolean isUsername = false;
			//check if element is type email
			if(element.getType().contains(search_val))
			
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

			//if any of the above apply, then set isUsername flag to true
			isUsername = true;
			
			if(isUsername){
				return element;
			}
		}
		
		return null;
	}

}
