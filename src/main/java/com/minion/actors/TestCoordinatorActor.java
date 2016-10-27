package com.minion.actors;

import java.net.URL;
import java.util.Iterator;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

import com.minion.api.PastPathExperienceController;
import com.minion.browsing.Browser;
import com.minion.browsing.Page;
import com.minion.browsing.PageElement;
import com.minion.browsing.actions.Action;
import com.minion.memory.OrientDbPersistor;
import com.minion.structs.Message;
import com.minion.structs.Path;
import com.minion.tester.Test;

/**
 * Handles retrieving tests
 * 
 * @author brandon kindred
 *
 */
public class TestCoordinatorActor extends UntypedActor {
    private static final Logger log = LoggerFactory.getLogger(TestCoordinatorActor.class);

    /**
     * Inputs
     * 
     * URL url: 	Get all tests for this url
     * Test:		Execute test and determine if still correct or not
     * List<Test>   Execute list of tests and get the outcomes for all of them
     */
	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof Message){
			Message<?> acct_msg = (Message<?>)message;
			if(acct_msg.getData() instanceof URL){
				URL url = (URL)acct_msg.getData();
				//Retrieve from memory
				Browser browser = new Browser(url.toString());
				Page page = browser.getPage();
				
				OrientDbPersistor persistor = new OrientDbPersistor();
				Iterator<Vertex> page_iter = persistor.findVertices(page).iterator();
				
				Path path = new Path();
				//load all edges that leading to pageElement
				while(page_iter.hasNext()){
					Vertex page_vert = page_iter.next();
					path.add((Page)page_vert);
					Iterator<Vertex> page_element_iter = page_vert.getVertices(Direction.OUT, "Page").iterator();
					while(page_element_iter.hasNext()){
						Vertex page_element_vertex = page_element_iter.next();
						path.add((PageElement)page_element_vertex);
						
						Iterator<Vertex> result_vertices = page_element_vertex.getVertices(Direction.OUT, "PageElement").iterator();
						
						while(result_vertices.hasNext()){
							path.add((Action)result_vertices.next());
						}
					}
				}
				
				//load all edges from pageElements
			}
			else if(acct_msg.getData() instanceof Test){
				Test test = (Test)acct_msg.getData();
				Path path = test.getPath();
				Message<Path> path_msg = new Message<Path>(acct_msg.getAccountKey(), path);
				
				if(path.getPath() != null){
					Crawler.crawlPath(path);
				}
				
				//get current page of browser
				Page expected_page = test.getResult();
				Page last_page = path.findLastPage();
				
				last_page.setLandable(last_page.checkIfLandable());
				
				if(!last_page.equals(expected_page)){
					log.info("Saving test, for it has changed");
					
					Test test_new = new Test(path, expected_page, expected_page.getUrl());
					Message<Test> test_msg = new Message<Test>(acct_msg.getAccountKey(), test_new);
					
					final ActorRef path_expansion_actor = this.getContext().actorOf(Props.create(PathExpansionActor.class), "PathExpansionActor");
					path_expansion_actor.tell(path_msg, getSelf() );
				
					//tell memory worker of path
					final ActorRef memory_actor = this.getContext().actorOf(Props.create(MemoryRegistryActor.class), "MemoryRegistration"+UUID.randomUUID());
					memory_actor.tell(test_msg, getSelf() );

				}
				else{
					log.info("Saving unchanged test");
					
					Message<Test> test_msg = new Message<Test>(acct_msg.getAccountKey(), test);
					
					final ActorRef path_expansion_actor = this.getContext().actorOf(Props.create(PathExpansionActor.class), "PathExpansionActor");
					path_expansion_actor.tell(path_msg, getSelf() );
				
					//tell memory worker of path
					final ActorRef memory_actor = this.getContext().actorOf(Props.create(MemoryRegistryActor.class), "MemoryRegistration"+UUID.randomUUID());
					memory_actor.tell(test_msg, getSelf() );

					//tell memory worker of path
				}
				//memory_actor.tell(path_msg, getSelf() );

				//broadcast path
				PastPathExperienceController.broadcastTestExperience(test);
			}
			else{
				log.info("ERROR : Message contains unknown format");
			}
		}
		else{
			log.info("ERROR : Did not receive a Message object");
		}
	}

}
