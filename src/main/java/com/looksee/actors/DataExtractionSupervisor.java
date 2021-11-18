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
import com.looksee.models.message.ElementsSaved;
import com.looksee.services.ElementStateService;
import com.looksee.services.PageStateService;

import akka.actor.AbstractActor;
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
	private ElementStateService element_state_service;
	
	@Autowired
	private PageStateService page_state_service;
	
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
						log.warn("saving elements");
						saveNewElements(message.getPageStateId(),
										message.getElementStates());
						
						log.warn("saved "+message.getElementStates().size()+" elements to neo4j to page state id : "+message.getPageStateId());
						ElementsSaved elements = new ElementsSaved(message.getAccountId(),
																   message.getPageUrl(), 
																   message.getAuditRecordId(), 
																   message.getElementStates().size());
						
						getContext().getSender().tell(elements, getSelf());
					} catch(Exception e) {
						e.printStackTrace();
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
	
	private void saveNewElements(long page_state_id, List<ElementState> element_states) {
		List<Long> element_ids = new ArrayList<>();
		List<String> element_keys = new ArrayList<>();

		for(ElementState element : element_states){
			element_keys.add(element.getKey());
	   	}
		
		Set<String> existing_keys = new HashSet<>();
		existing_keys.addAll(element_state_service.getAllExistingKeys(element_keys));
		List<ElementState> existing_elements = element_state_service.getElements(existing_keys);
		log.warn(existing_elements.size()+" existing elements found");
		List<ElementState> new_element_states = element_states
												   .stream()
												   .filter(f -> !existing_keys.contains(f.getKey()))
												   .collect(Collectors.toList());
		
		log.warn(new_element_states.size()+" new elements flagged for db creation");
		List<Long> existing_element_ids = existing_elements
													   .stream()
													   .map(ElementState::getId)
													   .collect(Collectors.toList());

		log.warn("Does this add up ??  "+(0 == element_states.size()-(existing_elements.size()+new_element_states.size())));
		//Iterable<ElementState> new_elements = element_state_service.saveAll(new_element_states);
		for(ElementState element : new_element_states){
			element_ids.add(element_state_service.save(element).getId());
	   	}
		log.warn(existing_element_ids.size()+" elements created");
		element_ids.addAll(existing_element_ids);
		
		page_state_service.addAllElements(page_state_id, element_ids);
		log.warn("done adding elements to page state");
	}	
}
