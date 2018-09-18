package com.minion.actors;

import static com.qanairy.config.SpringExtension.SpringExtProvider;

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
import com.qanairy.models.PageElement;
import com.qanairy.models.PathObject;
import com.qanairy.models.enums.FormType;
import com.minion.structs.Message;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
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
	
	@Autowired
	private ActorSystem actor_system;
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(Message.class, message -> {
					if(message.getData() instanceof Form){
						Form form = ((Form)message.getData());
						if(form.getType().equals(FormType.LOGIN)){
							final ActorRef loginFormTestDiscoveryActor = actor_system.actorOf(SpringExtProvider.get(actor_system)
									  .props("loginFormTestDiscoveryActor"), "login_form_test_discovery_actor"+UUID.randomUUID());
							loginFormTestDiscoveryActor.tell(message, getSelf() );
						}
						else{
							final ActorRef generalFormTestDiscoveryActor = actor_system.actorOf(SpringExtProvider.get(actor_system)
									  .props("generalFormTestDiscoveryActor"), "general_form_test_discovery_actor"+UUID.randomUUID());
							generalFormTestDiscoveryActor.tell(message, getSelf() );
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
	

	
	
	@Deprecated
	public static List<List<PathObject>> generateRequirementChecks(PageElement input, boolean isRequired){
		assert input.getName().equals("input");
		
		List<List<PathObject>> tests = new ArrayList<List<PathObject>>();
		for(Attribute attribute: input.getAttributes()){
			if(attribute.getName().equals("type")){
				String input_type = attribute.getVals().get(0);
				if(input_type.equals("text") ||
						input_type.equals("textarea") ||
						input_type.equals("email")){
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
				else if( input_type.equals("number")){

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
