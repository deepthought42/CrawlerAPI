package com.minion.actors;

import static com.qanairy.config.SpringExtension.SpringExtProvider;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.minion.structs.Message;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Test;

import akka.actor.Props;
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

/**
 * Simplifies tests by chopping up tests based on the landability of a page state as well as checking if other 
 * tests already contain the action-element pairings to either reduce a test in size or eliminate it as a duplicate.
 */
@Component
@Scope("prototype")
public class TestPathSimplifier extends AbstractActor{
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	Cluster cluster = Cluster.get(getContext().getSystem());
	
	@Autowired
	private ActorSystem actor_system;
	
	public static Props props() {
	  return Props.create(TestPathSimplifier.class);
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
					System.err.println("Test path simplfier received message of type "+message.getData().getClass().getName());
					if(message.getData() instanceof Test){
						
						Test test = (Test)message.getData();
						List<PathObject> new_path = new ArrayList<PathObject>();
						List<String> path_keys = new ArrayList<String>();
						
						List<PathObject> path_objects = new ArrayList<PathObject>();
						for(String key : test.getPathKeys()){
							for(PathObject obj : test.getPathObjects()){
								if(obj.getKey().equals(key)){
									path_objects.add(obj);
									break;
								}
							}
						}
						
						
						for(PathObject path_obj : path_objects){
							if(path_obj instanceof PageState){
								PageState page_state = (PageState)path_obj;
								//clear known path if page is landable
															
								System.err.println("is page state landable  ?? :: "+page_state.isLandable());
								//return landable;
								
								if(page_state.isLandable()){
									final ActorRef work_allocator = actor_system.actorOf(SpringExtProvider.get(actor_system)
											  .props("workAllocationActor"), "work_allocation_actor"+UUID.randomUUID());

									Message<URL> url_msg = new Message<URL>(message.getAccountKey(), new URL(page_state.getUrl()), message.getOptions());
									work_allocator.tell(url_msg, getSelf() );
									
									new_path.clear();
									path_keys.clear();
								}
								new_path.add(path_obj);
								path_keys.add(path_obj.getKey());
							}
						}
						
						//check if test has any element action pairs that have been seen in another test already
						//if there is a duplication, then check which test is longer and choose for the shortest test. 
						//	Then eliminate the element action pair along with resulting page state from all other tests 
						/*
						Set<Test> tests = domain_repo.getTests(message.getOptions().get("host").toString());
						 
						for(Test existing_test : tests){
							for(int existing_test_path_idx = 0; existing_test_path_idx<existing_test.getPathKeys().size(); existing_test_path_idx++){
								int seq_cnt = 0;
								for(int test_path_idx=0; test_path_idx<test.getPathKeys().size(); test_path_idx++){
									if(existing_test.getPathKeys().get(existing_test_path_idx).equals(test.getPathKeys().get(test_path_idx))){
										existing_test_path_idx++;
										seq_cnt++;
									}
									else {
									}
								}
							}
						}
						*/
						test.setPathKeys(path_keys);
						test.setPathObjects(path_objects);
						test.setKey(test.generateKey());
						
						//Test new_test = new Test(path_keys, new_path, test.getResult(), test.getName());
						Message<Test> test_msg = new Message<Test>(message.getAccountKey(), test, message.getOptions());
						
						System.err.println("!!!!!!!!!!!!!!!!!!     EXPLORATORY ACTOR SENDING TEST TO PATH EXPANSION");
						final ActorRef path_expansion_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
								  .props("pathExpansionActor"), "path_expansion"+UUID.randomUUID());
						path_expansion_actor.tell(test_msg, getSelf());
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
	
	public static final class TestBrowserRecord {
		Test test;
		String browser_name;
		
		public TestBrowserRecord(Test test, String browser_name){
			this.test = test;
			this.browser_name = browser_name;
		}
	}
}
