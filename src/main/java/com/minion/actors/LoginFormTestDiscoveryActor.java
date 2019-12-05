package com.minion.actors;

import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.minion.api.MessageBroadcaster;
import com.minion.browsing.Crawler;
import com.qanairy.models.Action;
import com.qanairy.models.Attribute;
import com.qanairy.models.Domain;
import com.qanairy.models.ExploratoryPath;
import com.qanairy.models.Form;
import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Test;
import com.qanairy.models.TestRecord;
import com.qanairy.models.TestUser;
import com.qanairy.models.enums.BrowserType;
import com.qanairy.models.enums.BugType;
import com.qanairy.models.enums.FormType;
import com.qanairy.models.enums.TestStatus;
import com.qanairy.models.message.BugMessage;
import com.qanairy.models.message.FormDiscoveryMessage;
import com.qanairy.models.message.TestMessage;
import com.qanairy.models.repository.ActionRepository;
import com.qanairy.services.DomainService;
import com.qanairy.services.FormService;
import com.qanairy.services.TestService;
import com.qanairy.utils.PathUtils;

import akka.actor.AbstractActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;
import akka.event.Logging;
import akka.event.LoggingAdapter;

@Component
@Scope("prototype")
public class LoginFormTestDiscoveryActor extends AbstractActor {
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	private Cluster cluster = Cluster.get(getContext().getSystem());

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
	
	private final String MISSING_USERNAME_ERROR = "We were unable to find a username field";
	private final String MISSING_PASSWORD_ERROR = "We were unable to find a password field";
	
	//subscribe to cluster changes
	@Override
	public void preStart() {
	  cluster.subscribe(getSelf(), ClusterEvent.initialStateAsEvents(), 
	      MemberEvent.class, UnreachableMember.class);
	}

	//re-subscribe when restart
	@Override
    public void postStop() {
	  cluster.unsubscribe(getSelf());
    }

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(FormDiscoveryMessage.class, message -> {
					Form form = form_service.clearBugMessages(message.getForm().getId());
					//check that message data is of type Form and that the form type is set to login
					log.info("login form test discovery actor is up!");

					Domain domain = message.getDomain();
					//check if form type is set to login
					if(form.getType().equals(FormType.LOGIN)){
						log.info("FORM TYPE IS    LOGIN");
						//get current domain from options list within message
						//  generate path leading to current form
						
						//get users for current domain
						Set<TestUser> test_users = domain_service.getTestUsers(domain);
						log.info("generating tests for "+test_users.size()+"   users");
						for(TestUser user : test_users){
							ExploratoryPath exploratory_path = initializeFormTest(form, domain.getUrl(), message.getAccountId());

							//  clone test
							//  get username element and add it to path
							List<ElementState> elements = form.getFormFields();
							//find username input element
							ElementState username_elem = findInputElementByAttribute(elements, "username");
							
							if(username_elem == null){
								username_elem = findInputElementByAttribute(elements, "email");
								if(username_elem == null){
									log.info("could not find username !!!!!!!!");
									BugMessage bug_message = new BugMessage(MISSING_USERNAME_ERROR, BugType.MISSING_FIELD, new Date());
									form_service.addBugMessage(form.getId(), bug_message);
									//throw error that cannot find username field
								}
							}

							if(username_elem != null) {
								//	add typing action to path with value equal to user.username
								exploratory_path.addPathObject(username_elem);
								exploratory_path.addToPathKeys(username_elem.getKey());
								
								Action type_username = new Action("sendKeys", user.getUsername());
								Action action_record = action_repo.findByKey(type_username.getKey());
								if(action_record != null){
									type_username = action_record;
								}
								exploratory_path.addPathObject(type_username);
								exploratory_path.addToPathKeys(type_username.getKey());
							}
							
							//  get password element and add it to the path
							ElementState password_elem = findInputElementByAttribute(elements, "password");

							if(password_elem == null){
								log.info("could not find password !!!!!!!!");
								//throw error that cannot find password field
								BugMessage bug_message = new BugMessage(MISSING_PASSWORD_ERROR, BugType.MISSING_FIELD, new Date());
								form_service.addBugMessage(form.getId(), bug_message);
							}
							
							if(password_elem != null) {
								//  add typing action to path with value equal to user.password	
								
								exploratory_path.addPathObject(password_elem);
								exploratory_path.addToPathKeys(password_elem.getKey());
								Action type_password = new Action("sendKeys", user.getPassword());
								Action action_record = action_repo.findByKey(type_password.getKey());
								if(action_record != null){
									type_password= action_record;
								}
								exploratory_path.addPathObject(type_password);
								exploratory_path.addToPathKeys(type_password.getKey());
							}
							
							
							//find submit button
							exploratory_path.addPathObject(form.getSubmitField());
							exploratory_path.addToPathKeys(form.getSubmitField().getKey());
							
							List<Action> action_list = new ArrayList<Action>();
							Action submit_login = new Action("click", "");
							Action action_record = action_repo.findByKey(submit_login.getKey());
							if(action_record != null){
								submit_login= action_record;
							}
							
							action_list.add(submit_login);

							exploratory_path.addPathObject(submit_login);
							exploratory_path.addToPathKeys(submit_login.getKey());
							log.warning("performing path exploratory crawl");
							PageState result_page = crawler.performPathExploratoryCrawl(domain.getDiscoveryBrowserName(), exploratory_path, domain.getUrl());
							result_page.setLoginRequired(true);
							log.warning("exploratory path keys being saved for test   ::   " + exploratory_path.getPathKeys());

							boolean leaves_domain = !(message.getDomain().getUrl().trim().equals(new URL(result_page.getUrl()).getHost()) || result_page.getUrl().contains(new URL(PathUtils.getLastPageState(exploratory_path.getPathObjects()).getUrl()).getHost()));
							
							Test test = new Test(exploratory_path.getPathKeys(), exploratory_path.getPathObjects(), result_page, user.getUsername()+" user login", false, leaves_domain);
							
							test.addRecord(new TestRecord(new Date(), TestStatus.UNVERIFIED, domain.getDiscoveryBrowserName(), result_page, 0L, test.getPathKeys()));
							//test_service.save(test);
							MessageBroadcaster.broadcastDiscoveredTest(test, domain.getUrl(), message.getAccountId());

							TestMessage test_message = new TestMessage(test, message.getDiscoveryActor(), BrowserType.create(message.getDomain().getDiscoveryBrowserName()), message.getDomainActor(), message.getDomain(), message.getAccountId());
							message.getDiscoveryActor().tell(test_message, getSelf());
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

	private ExploratoryPath initializeFormTest(Form form, String url, String user_id) {
		
		List<String> path_keys = new ArrayList<String>();
		List<PathObject> path_objects = new ArrayList<PathObject>();
		
		//get page state		
		PageState page = form_service.getPageState(form);
		
		//get all tests that contain page state as path object
		List<Test> tests = test_service.findTestsWithPageState(page.getKey(), url, user_id);
		//get test with smallest path
		int shortest_length = 1000;
		Test shortest_test = null;
		for(Test test : tests){
			if(test.getPathKeys().size() < shortest_length){
				shortest_length = test.getPathKeys().size();
				shortest_test = test;
			}
		}
	
		log.warning("shortest test :: " + shortest_test.getPathKeys().size());
		//add test path to path objects and keys
		List<PathObject> test_path_objects = test_service.getPathObjects(shortest_test.getKey(), url, user_id);
		log.warning("path objects size ::   "+test_path_objects.size());
		for(String key : shortest_test.getPathKeys()){
			for(PathObject obj : test_path_objects){
				if(key.equals(obj.getKey())){
					path_objects.add(obj);
					path_keys.add(key);
				}
			}
		}
		log.warning("path keys size   :     "+path_keys.size());
		
		return new ExploratoryPath(path_keys, path_objects);
	}

	private ElementState findInputElementByAttribute(List<ElementState> elements, String search_val) {
		for(ElementState element : elements){
			//check if element is type email
			log.warning("element get type :: "+element.getType().contains(search_val));
			if(element.getType().contains(search_val)){
				return element;
			}
			
			//check if element has value username in any attributes
			for(Attribute attribute : element.getAttributes()){
				for(String val : attribute.getVals()){
					log.warning("element attribute value  :: "+val.contains(search_val));

					if(val.contains(search_val)){
						return element;
					}
				}
			}
			log.warning("element attribute value  :: "+element.getName().contains(search_val));
			log.warning("element xpath value  :: "+element.getXpath().contains(search_val));

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
