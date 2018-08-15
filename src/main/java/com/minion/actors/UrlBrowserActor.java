package com.minion.actors;

import static com.qanairy.config.SpringExtension.SpringExtProvider;

import java.net.URL;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.UntypedActor;
import akka.actor.AbstractActor;
import akka.actor.AbstractActor.Receive;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;

import com.minion.structs.Message;
import com.qanairy.models.Test;
import com.qanairy.models.PageState;
import com.qanairy.services.TestCreatorService;
import com.qanairy.services.TestService;

/**
 * Manages a browser instance and sets a crawler upon the instance using a given path to traverse 
 *
 */
@Component
@Scope("prototype")
public class UrlBrowserActor extends AbstractActor {
	private static Logger log = LoggerFactory.getLogger(UrlBrowserActor.class.getName());
	
	@Autowired
	private ActorSystem actor_system;
	
	@Autowired
	private TestCreatorService test_creator_service;
	
	@Autowired
	private TestService test_service;

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(Message.class, message -> {
					if(message.getData() instanceof URL){
						boolean test_generated_successfully = false;
						do{
							try{
								String browser = message.getOptions().get("browser").toString();
								String discovery_key = message.getOptions().get("discovery_key").toString();
								String host = message.getOptions().get("host").toString();
								String url = ((URL)message.getData()).toString();
								
								Test test = test_creator_service.generate_landing_page_test(browser, discovery_key, host, url);
								test_service.save(test, host);
								
								Message<Test> test_msg = new Message<Test>(message.getAccountKey(), test, message.getOptions());
		
								/**  path expansion temorarily disabled
								 */
								/*
								final ActorRef path_expansion_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
										  .props("pathExpansionActor"), "path_expansion"+UUID.randomUUID());
								path_expansion_actor.tell(test_msg, getSelf() );
								*/
								
								
								
								
								/*final ActorRef form_test_discoverer = actor_system.actorOf(SpringExtProvider.get(actor_system)
										  .props("formTestDiscoveryActor"), "form_test_discovery"+UUID.randomUUID());
								form_test_discoverer.tell(test_msg, getSelf() );
								*/
								System.err.println("test result :: "+test.getResult());
								System.err.println("message account key :: "+message.getAccountKey());
								System.err.println("message options "+ message.getOptions());
	
								Message<PageState> page_state_msg = new Message<PageState>(message.getAccountKey(), test.getResult(), message.getOptions());

								final ActorRef form_discoverer = actor_system.actorOf(SpringExtProvider.get(actor_system)
										  .props("formDiscoveryActor"), "form_discovery"+UUID.randomUUID());
								form_discoverer.tell(page_state_msg, getSelf() );
		
								
								break;
							}
							catch(Exception e){
								e.printStackTrace();
								log.error(e.getMessage());
							}
						}while(!test_generated_successfully);
				   }
					//log.warn("Total Test execution time (browser open, crawl, build test, save data) : " + browserActorRunTime);
		
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