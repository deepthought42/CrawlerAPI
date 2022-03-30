package com.looksee.actors;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.looksee.models.ElementState;
import com.looksee.models.message.ElementProgressMessage;
import com.looksee.models.message.ElementsSaveError;
import com.looksee.models.message.ElementsSaved;
import com.looksee.services.ElementStateService;

import akka.actor.AbstractActor;
import akka.actor.ActorSystem;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;

@Component
@Scope("prototype")
public class DataExtractionSupervisor extends AbstractActor{
	private static Logger log = LoggerFactory.getLogger(DataExtractionSupervisor.class);
	private Cluster cluster = Cluster.get(getContext().getSystem());
	
	@Autowired
	private ActorSystem actor_system;
	
	@Autowired
	private ElementStateService element_state_service;
	
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
	 *
	 * NOTE: Do not change the order of the checks for instance of below. These are in this order because ExploratoryPath
	 * 		 is also a Test and thus if the order is reversed, then the ExploratoryPath code never runs when it should
	 * @throws NullPointerException
	 * @throws IOException
	 * @throws NoSuchElementException
	 */
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(ElementProgressMessage.class, message-> { 
					try {
						
						List<Long> element_ids = saveNewElements(message.getPageStateId(),
																 message.getElementStates());
						
						ElementsSaved elements = new ElementsSaved(message.getAccountId(),
																   message.getPageUrl(), 
																   message.getAuditRecordId(), 
																   element_ids, 
																   message.getPageStateId(),
																   message.getDomainId());
						
						getContext().getSender().tell(elements, getSelf());
					} catch(Exception e) {
						e.printStackTrace();
						
						ElementsSaveError err = new ElementsSaveError(message.getAccountId(),
								   message.getPageStateId(), 
								   message.getAuditRecordId(),
								   message.getDomainId(),
								   message.getPageUrl());

						getContext().getSender().tell(err, getSelf());
					}
					
					
				})
				.match(MemberUp.class, mUp -> {
					log.info("Member is Up: {}", mUp.member());
				})
				.match(UnreachableMember.class, mUnreachable -> {
					log.warn("Member detected as unreachable: {}", mUnreachable.member());
				})
				.match(MemberRemoved.class, mRemoved -> {
					log.warn("Member is Removed: {}", mRemoved.member());
				})
				.matchAny(o -> {
					log.warn("received unknown message of type :: "+o.getClass().getName());
				})
				.build();
	}
	
	private List<Long> saveNewElements(long page_state_id, List<ElementState> element_states) {
		List<Long> element_ids = new ArrayList<>();
		
		/*
		List<String> element_keys = new ArrayList<>();

		for(ElementState element : element_states){
			element_keys.add(element.getKey());
	   	}
		*/
		Set<String> existing_keys = new HashSet<>();
		existing_keys.addAll(element_state_service.getAllExistingKeys(page_state_id));
		//List<ElementState> existing_elements = element_state_service.getElements(existing_keys);
		return element_states.parallelStream()
									   .filter(f -> !existing_keys.contains(f.getKey()))
									   .map(element -> element_state_service.save(element).getId())
									   .collect(Collectors.toList());
		/*
		List<Long> existing_element_ids = existing_elements
													   .parallelStream()
													   .map(ElementState::getId)
													   .collect(Collectors.toList());
		for(ElementState element : new_element_states){
			element_ids.add(element_state_service.save(element).getId());
	   	}
		return element_ids;
		 */
	}	
}
