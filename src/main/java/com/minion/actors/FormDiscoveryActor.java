package com.minion.actors;

import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.minion.api.MessageBroadcaster;
import com.minion.browsing.Browser;
import com.minion.browsing.form.ElementRuleExtractor;
import com.minion.structs.Message;
import com.qanairy.integrations.DeepthoughtApi;
import com.qanairy.models.Form;
import com.qanairy.models.PageElement;
import com.qanairy.models.PageState;
import com.qanairy.models.repository.PageStateRepository;
import com.qanairy.models.rules.Rule;
import com.qanairy.services.BrowserService;

import akka.actor.Props;
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
public class FormDiscoveryActor extends AbstractActor{
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	Cluster cluster = Cluster.get(getContext().getSystem());
	
	@Autowired
	private BrowserService browser_service;
	
	@Autowired
	ElementRuleExtractor rule_extractor;
	
	@Autowired
	PageStateRepository page_state_repo;
	
	public static Props props() {
	  return Props.create(FormDiscoveryActor.class);
	}
	
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
					if(message.getData() instanceof PageState){
					  	System.err.println("FORM DISCOVERY HAS STARTED");

						PageState page_state = ((PageState)message.getData());
						
						//get first page in path
			
						int cnt = 0;
					  	Browser browser = null;
					  	boolean forms_created = false;
					  	do{
					  		
					  		try{
					  			System.err.println("Getting browser for form extraction");
						  		browser = new Browser(message.getOptions().get("browser").toString());

					  			System.err.println("navigating to url :: "+page_state.getUrl());
						  		browser.navigateTo(page_state.getUrl());
						  		

					  			System.err.println("Looking up page state by key");
						  		page_state = page_state_repo.findByKey(page_state.getKey());
								  
						  		System.err.println("FORM DISCOVERY ACTOR IS EXTRACTING FORMS " );
							  	List<Form> forms = browser_service.extractAllForms(page_state, browser);
						  		System.err.println("FORM DISCOVERY ACTOR IS EXTRACTING FORMS :: " + forms.size());

							  	for(Form form : forms){
								  	for(PageElement field: form.getFormFields()){
										//for each field in the complex field generate a set of tests for all known rules
								  		List<Rule> rules = rule_extractor.extractInputRules(field);
										
										log.info("Total RULES   :::   "+rules.size());
										for(Rule rule : rules){
											field.addRule(rule);
										
										}
									}
								  							  	
								    DeepthoughtApi.predict(form);
							       
								    System.err.println("PREDICTION DONE !!! ");
								    System.err.println("********************************************************");
							  		browser.close();
								  	
								  	page_state.addForm(form);
								  	page_state_repo.save(page_state);
							        System.err.println("SENDING FORM FOR BROADCAST    !!!!!!!!!!!!!@@@@@@@@@!!!!!!!!!!!!!");
								  	MessageBroadcaster.broadcastDiscoveredForm(form, message.getOptions().get("host").toString());
							  	}
							  	System.err.println("FORM DISCOVERY HAS ENDED");
							  	forms_created = true;
								break;
							} catch(Exception e){
						  		log.warning(e.getMessage());
						  	}
					  		browser = new Browser(page_state.getUrl());
							cnt++;
						}	while(!forms_created && cnt < 100000);
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
}
