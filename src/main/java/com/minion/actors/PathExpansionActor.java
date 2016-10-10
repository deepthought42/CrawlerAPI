package com.minion.actors;

import java.util.ArrayList;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;

import akka.actor.Props;
import akka.actor.UntypedActor;

import com.minion.api.PastPathExperienceController;
import com.minion.browsing.Page;
import com.minion.browsing.PathObject;
import com.minion.structs.Message;
import com.minion.structs.Path;
import com.minion.structs.SessionTestTracker;
import com.minion.structs.TestMapper;
import com.minion.tester.Test;

/**
 * Actor that handles {@link Path}s and {@link Test}s to expand said paths.
 * 
 * @author Brandon Kindred
 *
 */
public class PathExpansionActor extends UntypedActor {
    private static final Logger log = LoggerFactory.getLogger(PathExpansionActor.class);

    /**
     * {@inheritDoc}
     */
	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof Message){
			Message<?> acct_msg = (Message<?>)message;
			if(acct_msg.getData() instanceof Test){
				
				Test test = (Test)acct_msg.getData();
				Path path = test.getPath();
				Message<Test> test_msg = new Message<Test>(acct_msg.getAccountKey(), test);
				
				log.info("EXPANDING TEST PATH WITH LENGTH : "+path.getPath().size());
				ArrayList<Path> pathExpansions = new ArrayList<Path>();

				final ActorRef memory_registry = this.getContext().actorOf(Props.create(MemoryRegistryActor.class), "memoryRegistry"+UUID.randomUUID());
				memory_registry.tell(test_msg, getSelf());
				
				//IF RESULT IS DIFFERENT THAN LAST PAGE IN PATH AND TEST DOESN'T CROSS INTO ANOTHER DOMAIN IN RESULT
				//   THEN 
				if(path != null && path.getIsUseful() && !path.getSpansMultipleDomains()){
					if(!test.getPath().getLastPage().getUrl().equals(test.getResult().getUrl()) && test.getResult().isLandable()){
						log.info("Last page is landable...truncating path to start with last_page");
						path = new Path();
						path.add(new PathObject<Page>(test.getResult()));
					}
					
					//EXPAND PATH IN TEST
					pathExpansions = Path.expandPath(path);
					log.info("Path expansions found : " +pathExpansions.size());
					
					final ActorRef work_allocator = this.getContext().actorOf(Props.create(WorkAllocationActor.class), "workAllocator"+UUID.randomUUID());
					for(Path expanded : pathExpansions){
						Test new_test = new Test(expanded, null,  path.getLastPage().getUrl());
						// CHECK THAT TEST HAS NOT YET BEEN EXPERIENCED RECENTLY
						SessionTestTracker seqTracker = SessionTestTracker.getInstance();
						TestMapper testMap = seqTracker.getSequencesForSession("SESSION_KEY_HERE");
						if(!testMap.containsTest(new_test)){
							Message<Test> expanded_test_msg = new Message<Test>(acct_msg.getAccountKey(), new_test);

							work_allocator.tell(expanded_test_msg, getSelf() );
							testMap.addTest(new_test);
						}
						else{
							log.info("TEST WITH KEY : "+new_test.hashCode()+" : HAS ALREADY BEEN EXAMINED!!!! No future examination will happen during this sessions");
							PastPathExperienceController.broadcastTestExperience(testMap.getTestHash().get(test.hashCode()));
						}
					}
				}
			}
			else if(acct_msg.getData() instanceof Path){
				Path path = (Path)acct_msg.getData();
				
				log.info("EXPANDING PATH WITH LENGTH : "+path.getPath().size());
				ArrayList<Path> pathExpansions = new ArrayList<Path>();

				Message<Path> path_msg = new Message<Path>(acct_msg.getAccountKey(), path);

				final ActorRef memory_registry = this.getContext().actorOf(Props.create(MemoryRegistryActor.class), "memoryRegistry"+UUID.randomUUID());
				memory_registry.tell(path_msg, getSelf());
				
				log.info("PATH SPANS MULTIPLE DOMAINS? :: " +path.getSpansMultipleDomains());
				if(path.getIsUseful() && !path.getSpansMultipleDomains()){
					Page last_page = path.getLastPage();
					Page first_page = (Page)path.getPath().get(0).getData();
					
					if(!first_page.getUrl().equals(last_page.getUrl()) && last_page.isLandable()){
						log.info("Last page is landable...truncating path to start with last_page");
						path = new Path();
						path.add(new PathObject<Page>(last_page));
					}
					// CHECK THAT PAGE ELEMENT ACTION SEQUENCE HAS NOT YET BEEN EXPERIENCED
					Test test = new Test(path, last_page, last_page.getUrl());
					SessionTestTracker seqTracker = SessionTestTracker.getInstance();
					TestMapper testMap = seqTracker.getSequencesForSession("SESSION_KEY_HERE");
					if(!testMap.containsTest(test)){
						testMap.addTest(test);
					}
					else{
						log.info("TEST WITH KEY : "+test.hashCode()+" : HAS ALREADY BEEN EXAMINED!!!! No future examination will happen during this sessions");
					}

					pathExpansions = Path.expandPath(path);
					log.info("Path expansions found : " +pathExpansions.size());
					
					final ActorRef work_allocator = this.getContext().actorOf(Props.create(WorkAllocationActor.class), "workAllocator"+UUID.randomUUID());
					for(Path expanded : pathExpansions){
						Message<Path> expanded_path_msg = new Message<Path>(acct_msg.getAccountKey(), expanded);
						
						work_allocator.tell(expanded_path_msg, getSelf() );
					}
				}	
			}
		}
	}
}
