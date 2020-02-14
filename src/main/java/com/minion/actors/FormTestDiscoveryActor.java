package com.minion.actors;

import static com.qanairy.config.SpringExtension.SpringExtProvider;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.qanairy.models.Action;
import com.qanairy.models.Attribute;
import com.qanairy.models.Form;
import com.qanairy.models.ElementState;
import com.qanairy.models.PathObject;
import com.qanairy.models.enums.DiscoveryAction;
import com.qanairy.models.enums.FormType;
import com.qanairy.models.message.DiscoveryActionRequest;
import com.qanairy.models.message.FormDiscoveryMessage;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;
import akka.pattern.Patterns;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;

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
	public static List<List<PathObject>> generateRequirementChecks(ElementState input, boolean isRequired){
		assert input.getName().equals("input");
		
		List<List<PathObject>> tests = new ArrayList<List<PathObject>>();
		for(Attribute attribute: input.getAttributes()){
			if("type".equals(attribute.getName())){
				String input_type = attribute.getVals().get(0);
				if("text".equals( input_type ) ||
						"textarea".equals(input_type) ||
						"email".equals(input_type)){
					//generate empty string test
					List<PathObject> path_obj_list = new ArrayList<PathObject>();
					path_obj_list.add(input);
					path_obj_list.add(new Action("click", ""));
					path_obj_list.add(input);
					path_obj_list.add(new Action("sendKeys", ""));
					tests.add(path_obj_list);
					
					//generate single character str test
					List<PathObject> path_obj_list_2 = new ArrayList<PathObject>();
					path_obj_list_2.add(input);
					path_obj_list_2.add(new Action("click", ""));
					path_obj_list_2.add(input);
					path_obj_list_2.add(new Action("sendKeys", "a"));
					tests.add(path_obj_list_2);
				}
				else if( "number".equals(input_type)){

					//generate empty string test
					List<PathObject> path_obj_list = new ArrayList<PathObject>();
					path_obj_list.add(input);
					path_obj_list.add(new Action("click", ""));
					path_obj_list.add(input);
					path_obj_list.add(new Action("sendKeys", ""));
					tests.add(path_obj_list);
					
					//generate single character str test
					List<PathObject> path_obj_list_2 = new ArrayList<PathObject>();
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
