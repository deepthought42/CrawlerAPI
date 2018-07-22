package com.minion.actors;

import static com.qanairy.config.SpringExtension.SpringExtProvider;

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
import com.qanairy.models.repository.PageStateRepository;
import com.qanairy.services.BrowserService;
import com.qanairy.services.TestService;

import akka.actor.Props;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.UnreachableMember;
import akka.event.Logging;
import akka.event.LoggingAdapter;

@Component
@Scope("prototype")
public class TestPathSimplifier extends AbstractActor{
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	Cluster cluster = Cluster.get(getContext().getSystem());

	@Autowired
	private BrowserService browser_service;
	
	@Autowired
	private TestService test_service;
	
	@Autowired
	private PageStateRepository page_state_repo;
	
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
						
						for(PathObject path_obj : test.getPathObjects()){
							if(path_obj instanceof PageState){
								PageState page_state = (PageState)path_obj;
								//clear known path if page is landable
															
								System.err.println("is page state landable  ?? :: "+page_state.isLandable());
								//return landable;
								
								if(page_state.isLandable()){
									Test new_test = new Test(path_keys, new_path, page_state, test.getName());
									new_test = test_service.save(new_test, message.getOptions().get("host").toString());
									
									Message<Test> test_msg = new Message<Test>(message.getAccountKey(), new_test, message.getOptions());

									System.err.println("!!!!!!!!!!!!!!!!!!     EXPLORATORY ACTOR SENDING TEST TO PATH EXPANSION");
									final ActorRef path_expansion_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
											  .props("pathExpansionActor"), "path_expansion"+UUID.randomUUID());
									path_expansion_actor.tell(test_msg, getSelf());
									new_path.clear();
									path_keys.clear();
								}
								new_path.add(path_obj);
								path_keys.add(path_obj.getKey());
							}
						}
						
						System.err.println("!!!!!!!!!!!!!!!!!!     EXPLORATORY ACTOR SENDING TEST TO PATH EXPANSION");
						final ActorRef path_expansion_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
								  .props("pathExpansionActor"), "path_expansion"+UUID.randomUUID());
						path_expansion_actor.tell(message, getSelf());
					}
				})
				.matchAny(o -> log.info("received unknown message"))
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
