package com.minion.actors;

import static com.qanairy.config.SpringExtension.SpringExtProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.minion.api.MessageBroadcaster;
import com.minion.browsing.Crawler;
import com.minion.structs.Message;
import com.qanairy.models.Action;
import com.qanairy.models.Attribute;
import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.Domain;
import com.qanairy.models.ExploratoryPath;
import com.qanairy.models.Form;
import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Test;
import com.qanairy.models.TestUser;
import com.qanairy.models.enums.FormType;
import com.qanairy.models.repository.ActionRepository;
import com.qanairy.services.AccountService;
import com.qanairy.services.DiscoveryRecordService;
import com.qanairy.services.DomainService;
import com.qanairy.services.FormService;
import com.qanairy.services.TestService;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
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
	private AccountService account_service;
	
	@Autowired 
	private ActionRepository action_repo;
	
	@Autowired
	private ActorSystem actor_system;
	
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
								List<ElementState> elements = form.getFormFields();
								//find username input element
								ElementState username_elem = findInputElementByAttribute(elements, "username");
								
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
								ElementState password_elem = findInputElementByAttribute(elements, "password");

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
								log.warning("performing path exploratory crawl");
								PageState result_page = crawler.performPathExploratoryCrawl(domain.getDiscoveryBrowserName(), exploratory_path, message.getOptions().get("host").toString());
								
								Test test = new Test(exploratory_path.getPathKeys(), exploratory_path.getPathObjects(), result_page, user.getUsername()+" user login");
								test = test_service.save(test, domain.getUrl());
								MessageBroadcaster.broadcastDiscoveredTest(test, domain.getUrl());
							
								DiscoveryRecord discovery = domain_service.getMostRecentDiscoveryRecord(domain.getUrl(), null);
								//send test for exploration
								message.getOptions().put("discovery_key", discovery.getKey());
								message.getOptions().put("browser", domain.getDiscoveryBrowserName());
								
								Message<Test> test_msg = new Message<Test>(message.getAccountKey(), test, message.getOptions());
								log.warning("sending path expansion actor");
								final ActorRef path_expansion_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
										  .props("pathExpansionActor"), "path_expansion"+UUID.randomUUID());
								path_expansion_actor.tell(test_msg, getSelf() );
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
		
		//get page state		
		PageState page = form_service.getPageState(form);
		
		//get all tests that contain page state as path object
		List<Test> tests = test_service.findTestsWithPageState(page.getKey());
		//get test with smallest path
		int shortest_length = Integer.MAX_VALUE;
		Test shortest_test = null;
		for(Test test : tests){
			if(test.getPathKeys().size() < shortest_length){
				shortest_length = test.getPathKeys().size();
				shortest_test = test;
			}
		}
	
		log.warning("shortest test :: " + shortest_test.getPathKeys().size());
		//add test path to path objects and keys
		List<PathObject> test_path_objects = test_service.getPathObjects(shortest_test.getKey());
		log.warning("path objects size ::   "+test_path_objects);
		for(String key : shortest_test.getPathKeys()){
			for(PathObject obj : test_path_objects){
				if(key.equals(obj.getKey())){
					path_objects.add(obj);
					path_keys.add(key);
				}
			}
		}
		log.warning("path keys size   :     "+path_keys.size());
		
		return new ExploratoryPath(path_keys, path_objects, new ArrayList<Action>());
	}

	private ElementState findInputElementByAttribute(List<ElementState> elements, String search_val) {
		for(ElementState element : elements){
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
