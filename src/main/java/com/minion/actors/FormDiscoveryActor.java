package com.minion.actors;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.minion.aws.UploadObjectSingleOperation;
import com.minion.browsing.Browser;
import com.minion.browsing.element.ComplexField;
import com.minion.browsing.form.ElementRuleExtractor;
import com.minion.browsing.form.Form;
import com.minion.browsing.form.FormField;
import com.minion.structs.Message;
import com.qanairy.models.PageElement;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Test;
import com.qanairy.models.repository.PageElementRepository;
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
	private PageElementRepository page_element_repo;
	
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
	
						PageState page_state = ((PageState)message.getData());
						
						//get first page in path
			
						int cnt = 0;
					  	Browser browser = null;
					  	
					  	while(browser == null && cnt < 5){
					  		try{
						  		browser = new Browser(message.getOptions().get("browser").toString());
						  		browser.navigateTo(page_state.getUrl());
								break;
							}catch(NullPointerException e){
								log.error(e.getMessage());
							}
							cnt++;
						}	
					  	
					  	List<Form> forms = browser_service.extractAllForms(page_state, browser);
					  	for(Form form : forms){
	
						  	for(ComplexField complex_field: form.getFormFields()){
								//for each field in the complex field generate a set of tests for all known rules
								System.err.println("COMPLEX FIELD ELEMENTS   :::   "+complex_field.getElements().size());
								for(FormField field : complex_field.getElements()){
									PageElement input_elem = field.getInputElement();
									
									List<Rule> rules = ElementRuleExtractor.extractInputRules(input_elem);
									
									log.info("Total RULES   :::   "+rules.size());
									for(Rule rule : rules){
										input_elem.addRule(rule);
									}
								}
							}
						  	
						  	try{
						  		browser.close();
						  	}
						  	catch(Exception e){}

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
					System.err.println("o class :: "+o.getClass().getName());
					log.info("received unknown message");
				})
				.build();
	}
}
