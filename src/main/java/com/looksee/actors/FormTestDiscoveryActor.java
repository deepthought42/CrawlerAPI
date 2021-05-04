package com.looksee.actors;

import static com.looksee.config.SpringExtension.SpringExtProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.looksee.models.Action;
import com.looksee.models.Element;
import com.looksee.models.Form;
import com.looksee.models.LookseeObject;
import com.looksee.models.enums.FormType;
import com.looksee.models.message.FormDiscoveryMessage;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;

/**
 * Handles discovery and creation of various form tests
 */
@Component
@Scope("prototype")
public class FormTestDiscoveryActor extends AbstractActor {
	private static Logger log = LoggerFactory.getLogger(FormTestDiscoveryActor.class);
	private Cluster cluster = Cluster.get(getContext().getSystem());

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
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(FormDiscoveryMessage.class, message -> {
					Form form = message.getForm();
					
					/*
					Timeout timeout = Timeout.create(Duration.ofSeconds(120));
					Future<Object> future = Patterns.ask(message.getDomainActor(), new DiscoveryActionRequest(message.getDomain(), message.getAccountId()), timeout);
					DiscoveryAction discovery_action = (DiscoveryAction) Await.result(future, timeout.duration());
					if(discovery_action == DiscoveryAction.STOP) {
						return;
					}
					*/
					
					if(form.getType().equals(FormType.LOGIN)){
						log.info("LOGIN type recieved");
						final ActorRef loginFormTestDiscoveryActor = actor_system.actorOf(SpringExtProvider.get(actor_system)
								  .props("loginFormTestDiscoveryActor"), "login_form_test_discovery_actor"+UUID.randomUUID());
						loginFormTestDiscoveryActor.tell(message, getSelf() );
					}
					else{
						log.info("Another different type recieved");
						final ActorRef generalFormTestDiscoveryActor = actor_system.actorOf(SpringExtProvider.get(actor_system)
								  .props("generalFormTestDiscoveryActor"), "general_form_test_discovery_actor"+UUID.randomUUID());
						generalFormTestDiscoveryActor.tell(message, getSelf() );
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
					log.info("o class :: "+o.getClass().getName());
					log.info("received unknown message");
				})
				.build();
	}
	

	
	
	@Deprecated
	public static List<List<LookseeObject>> generateRequirementChecks(Element input, boolean isRequired){
		assert input.getName().equals("input");
		
		List<List<LookseeObject>> tests = new ArrayList<List<LookseeObject>>();
		for(String attribute: input.getAttributes().keySet()){
			if("type".equals(attribute)){
				String input_type = input.getAttributes().get(attribute);
				if("text".equals( input_type ) ||
						"textarea".equals(input_type) ||
						"email".equals(input_type)){
					//generate empty string test
					List<LookseeObject> path_obj_list = new ArrayList<LookseeObject>();
					path_obj_list.add(input);
					path_obj_list.add(new Action("click", ""));
					path_obj_list.add(input);
					path_obj_list.add(new Action("sendKeys", ""));
					tests.add(path_obj_list);
					
					//generate single character str test
					List<LookseeObject> path_obj_list_2 = new ArrayList<LookseeObject>();
					path_obj_list_2.add(input);
					path_obj_list_2.add(new Action("click", ""));
					path_obj_list_2.add(input);
					path_obj_list_2.add(new Action("sendKeys", "a"));
					tests.add(path_obj_list_2);
				}
				else if( "number".equals(input_type)){

					//generate empty string test
					List<LookseeObject> path_obj_list = new ArrayList<LookseeObject>();
					path_obj_list.add(input);
					path_obj_list.add(new Action("click", ""));
					path_obj_list.add(input);
					path_obj_list.add(new Action("sendKeys", ""));
					tests.add(path_obj_list);
					
					//generate single character str test
					List<LookseeObject> path_obj_list_2 = new ArrayList<LookseeObject>();
					path_obj_list_2.add(input);
					path_obj_list_2.add(new Action("click", ""));
					path_obj_list_2.add(input);
					path_obj_list_2.add(new Action("sendKeys", "0"));
					tests.add(path_obj_list_2);
				}
			}
		}
		return tests;
	}
	
}
