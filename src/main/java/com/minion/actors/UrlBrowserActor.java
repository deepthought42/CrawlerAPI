package com.minion.actors;

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
import com.minion.structs.Message;
import com.qanairy.config.SpringExtension;
import com.qanairy.models.Test;
import com.qanairy.services.TestCreatorService;
import com.qanairy.services.TestService;

/**
 * Manages a browser instance and sets a crawler upon the instance using a given path to traverse 
 *
 */
@Component
@Scope("prototype")
public class UrlBrowserActor extends UntypedActor {
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
	public void onReceive(Object message) throws Exception {
		if(message instanceof Message){
			Message<?> acct_msg = (Message<?>)message;

			System.err.println("Recieved data of type :: "+acct_msg.getData().getClass().getSimpleName());
			if(acct_msg.getData() instanceof URL){
				boolean test_generated_successfully = false;
				do{
					try{
						String browser = acct_msg.getOptions().get("browser").toString();
						String discovery_key = acct_msg.getOptions().get("discovery_key").toString();
						String host = acct_msg.getOptions().get("host").toString();
						String url = ((URL)acct_msg.getData()).toString();
						
						Test test = test_creator_service.generate_landing_page_test(browser, discovery_key, host, url);
						test_service.save(test, host);
						
						Message<Test> test_msg = new Message<Test>(acct_msg.getAccountKey(), test, acct_msg.getOptions());

						/*
						final ActorRef path_expansion_actor = actor_system.actorOf(SpringExtension.SPRING_EXTENSION_PROVIDER.get(actor_system)
								  .props("pathExpansionActor"), "path_expansion"+UUID.randomUUID());
						path_expansion_actor.tell(test_msg, getSelf() );
						*/
						
						final ActorRef form_test_discoverer = actor_system.actorOf(SpringExtension.SPRING_EXTENSION_PROVIDER.get(actor_system)
								  .props("formTestDiscoveryActor"), "form_test_discovery"+UUID.randomUUID());
						form_test_discoverer.tell(test_msg, getSelf() );
												
						
						break;
					}
					catch(Exception e){
						e.printStackTrace();
						log.error(e.getMessage());
					}
				}while(!test_generated_successfully);
		   }
			//log.warn("Total Test execution time (browser open, crawl, build test, save data) : " + browserActorRunTime);

		}else unhandled(message);
	}
}